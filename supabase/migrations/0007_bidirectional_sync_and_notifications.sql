-- =============================================================================
-- DebtTracker — прив'язка боржника/кредитора до зареєстрованого користувача за
-- телефоном/email, двостороннє дзеркалювання транзакцій між акаунтами обох
-- сторін боргу, і таблиця notifications для сповіщень (14а: клієнт опитує її
-- кожні 15с — це навмисно НЕ Realtime, а polling; див. NotificationsPoller).
-- DebtTracker — linking a debtor/creditor to a registered user by phone/email,
-- bidirectional mirroring of transactions between both sides of a debt, and a
-- notifications table (polled by the client every 15s — deliberately not
-- Realtime; see NotificationsPoller).
--
-- Дизайн дзеркалювання транзакцій:
-- - debt_transactions.amount і creditor_transactions.amount використовують
--   ОДНАКОВИЙ знак для того самого грошового руху: LEND (я дав, від'ємне) і
--   BORROW (я взяв, від'ємне) — це одна й та сама подія, побачена власником
--   боргу і боржником; так само REPAY/RETURN (додатні). Дзеркальний рядок
--   копіює amount без інверсії знаку.
-- - mirror_transaction_id заповнюється ЛИШЕ на авто-створеному дзеркальному
--   рядку (вказує назад на рядок-джерело в іншій таблиці). Оригінальний рядок,
--   уведений користувачем вручну, це поле не заповнює ніколи — це і є захист
--   від нескінченної рекурсії тригерів (див. propagate_* нижче).
-- Mirroring design:
-- - debt_transactions.amount and creditor_transactions.amount use the SAME
--   sign for the same money movement: LEND (I gave, negative) and BORROW
--   (I took, negative) are the same event seen by the debt owner and the
--   debtor; likewise REPAY/RETURN (positive). The mirror row copies amount
--   with no sign inversion.
-- - mirror_transaction_id is populated ONLY on the auto-created mirror row
--   (pointing back to its source row in the other table). The row a user
--   entered by hand never gets this populated — that absence is exactly what
--   stops the propagation triggers from recursing forever (see propagate_*
--   below).
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 1. Нові колонки: хто саме зареєстрований користувач і який рядок у нього —
--    дзеркало цього.
-- 1. New columns: which registered user this resolves to, and which row in
--    their account mirrors this one.
-- -----------------------------------------------------------------------------
alter table public.debtors
    add column linked_user_id uuid references auth.users (id) on delete set null,
    add column mirror_creditor_id uuid;

alter table public.creditors
    add column linked_user_id uuid references auth.users (id) on delete set null,
    add column mirror_debtor_id uuid;

alter table public.debt_transactions
    add column mirror_transaction_id uuid;

alter table public.creditor_transactions
    add column mirror_transaction_id uuid;

comment on column public.debtors.linked_user_id is 'auth.uid() зареєстрованого користувача, знайденого за phone/email цього боржника (див. link_debtor_to_registered_user). auth.uid() of the registered user resolved from this debtor''s phone/email.';
comment on column public.debtors.mirror_creditor_id is 'id рядка в creditors (у акаунті linked_user_id), що дзеркалить цей запис. id of the row in creditors (in linked_user_id''s account) mirroring this record.';
comment on column public.creditors.linked_user_id is 'Дзеркало debtors.linked_user_id для кредиторів. Mirror of debtors.linked_user_id for creditors.';
comment on column public.creditors.mirror_debtor_id is 'id рядка в debtors (у акаунті linked_user_id), що дзеркалить цей запис. id of the row in debtors (in linked_user_id''s account) mirroring this record.';
comment on column public.debt_transactions.mirror_transaction_id is 'Заповнено ЛИШЕ на авто-створеному дзеркальному рядку — id джерела в creditor_transactions. Populated ONLY on an auto-created mirror row — id of its source in creditor_transactions.';
comment on column public.creditor_transactions.mirror_transaction_id is 'Заповнено ЛИШЕ на авто-створеному дзеркальному рядку — id джерела в debt_transactions. Populated ONLY on an auto-created mirror row — id of its source in debt_transactions.';

create index idx_debtors_linked_user on public.debtors (linked_user_id) where linked_user_id is not null;
create index idx_creditors_linked_user on public.creditors (linked_user_id) where linked_user_id is not null;
create index idx_debt_transactions_mirror on public.debt_transactions (mirror_transaction_id) where mirror_transaction_id is not null;
create index idx_creditor_transactions_mirror on public.creditor_transactions (mirror_transaction_id) where mirror_transaction_id is not null;

-- -----------------------------------------------------------------------------
-- 2. notifications — сповіщення для отримувача (власник linked-акаунта).
--    Інсертити можуть лише SECURITY DEFINER-функції нижче — авторизованим
--    користувачам дозволено лише select/update(is_read)/delete власних рядків.
-- 2. notifications — recipient-facing notifications (owner of the linked
--    account). Only the SECURITY DEFINER functions below may insert — an
--    authenticated user may only select/update(is_read)/delete their own rows.
-- -----------------------------------------------------------------------------
create table public.notifications (
    id                   uuid primary key default gen_random_uuid(),
    user_id              uuid not null references auth.users (id) on delete cascade,  -- отримувач / recipient
    type                 text not null check (type in ('DEBTOR_LINKED', 'CREDITOR_LINKED', 'DEBT_TRANSACTION_ADDED', 'CREDIT_TRANSACTION_ADDED')),
    actor_user_id        uuid references auth.users (id) on delete set null,
    actor_display_name   text,          -- знімок імені актора на момент створення (отримувач не може читати чужий profiles через RLS) / snapshot of the actor's name at creation time (recipient can't read another profiles row via RLS)
    related_debtor_id    uuid,
    related_creditor_id  uuid,
    amount               numeric(14, 2),
    currency             text,
    is_read              boolean not null default false,
    created_at           timestamptz not null default now()
);

comment on table public.notifications is 'Сповіщення про прив''язку/нові транзакції в дзеркальному борзі (опитується клієнтом кожні 15с). Notifications about linking/new transactions on a mirrored debt (polled by the client every 15s).';

create index idx_notifications_user_created on public.notifications (user_id, created_at desc);

alter table public.notifications enable row level security;

create policy "notifications_owner_select" on public.notifications
    for select
    using (user_id = auth.uid());

create policy "notifications_owner_update" on public.notifications
    for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "notifications_owner_delete" on public.notifications
    for delete
    using (user_id = auth.uid());

alter publication supabase_realtime add table public.notifications;

-- -----------------------------------------------------------------------------
-- 3. link_debtor_to_registered_user — прив'язує боржника до зареєстрованого
--    користувача за phone/email, дзеркалить усі наявні транзакції, сповіщає.
--    Ідемпотентна: другий виклик для вже прив'язаного боржника — no-op.
-- 3. link_debtor_to_registered_user — links a debtor to a registered user by
--    phone/email, mirrors all existing transactions, notifies. Idempotent:
--    calling it again for an already-linked debtor is a no-op.
-- -----------------------------------------------------------------------------
create function public.link_debtor_to_registered_user(p_debtor_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_debtor       public.debtors%rowtype;
    v_caller       public.profiles%rowtype;
    v_target_user  uuid;
    v_mirror_id    uuid;
    v_tx           public.debt_transactions%rowtype;
    v_balance      numeric;
begin
    select * into v_debtor from public.debtors where id = p_debtor_id and user_id = auth.uid();
    if not found then
        raise exception 'Debtor not found or not owned by caller';
    end if;

    if v_debtor.linked_user_id is not null then
        return v_debtor.mirror_creditor_id;
    end if;

    select p.id into v_target_user
      from public.profiles p
     where p.id <> auth.uid()
       and (
           (v_debtor.email is not null and p.email is not null and lower(p.email) = lower(v_debtor.email))
           or (v_debtor.phone is not null and p.phone is not null and p.phone = v_debtor.phone)
       )
     limit 1;

    if v_target_user is null then
        return null;
    end if;

    select * into v_caller from public.profiles where id = auth.uid();

    insert into public.creditors (
        id, user_id, full_name, phone, email, avatar_url, comment, currency,
        linked_user_id, mirror_debtor_id, created_at, updated_at
    ) values (
        gen_random_uuid(), v_target_user,
        coalesce(nullif(v_caller.display_name, ''), v_caller.email, v_debtor.full_name),
        v_caller.phone, v_caller.email, v_caller.avatar_url,
        null, v_debtor.currency,
        auth.uid(), p_debtor_id, now(), now()
    ) returning id into v_mirror_id;

    for v_tx in
        select * from public.debt_transactions
         where debtor_id = p_debtor_id and is_deleted = false
         order by transaction_date
    loop
        insert into public.creditor_transactions (
            id, creditor_id, user_id, amount, method,
            transaction_date, comment, mirror_transaction_id, created_at, updated_at
        ) values (
            gen_random_uuid(), v_mirror_id, v_target_user,
            v_tx.amount, v_tx.method,
            v_tx.transaction_date, v_tx.comment, v_tx.id, now(), now()
        );
    end loop;

    update public.debtors
       set linked_user_id = v_target_user,
           mirror_creditor_id = v_mirror_id
     where id = p_debtor_id;

    select -coalesce(sum(amount), 0) into v_balance
      from public.creditor_transactions
     where creditor_id = v_mirror_id and is_deleted = false;

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_creditor_id, amount, currency, created_at
    ) values (
        gen_random_uuid(), v_target_user, 'DEBTOR_LINKED', auth.uid(),
        coalesce(nullif(v_caller.display_name, ''), v_caller.email),
        v_mirror_id, v_balance, v_debtor.currency, now()
    );

    return v_mirror_id;
end;
$$;

revoke all on function public.link_debtor_to_registered_user(uuid) from public, anon;
grant execute on function public.link_debtor_to_registered_user(uuid) to authenticated;

-- -----------------------------------------------------------------------------
-- 4. link_creditor_to_registered_user — дзеркало п.3 для кредиторів.
-- 4. link_creditor_to_registered_user — mirror of section 3 for creditors.
-- -----------------------------------------------------------------------------
create function public.link_creditor_to_registered_user(p_creditor_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_creditor     public.creditors%rowtype;
    v_caller       public.profiles%rowtype;
    v_target_user  uuid;
    v_mirror_id    uuid;
    v_tx           public.creditor_transactions%rowtype;
    v_balance      numeric;
begin
    select * into v_creditor from public.creditors where id = p_creditor_id and user_id = auth.uid();
    if not found then
        raise exception 'Creditor not found or not owned by caller';
    end if;

    if v_creditor.linked_user_id is not null then
        return v_creditor.mirror_debtor_id;
    end if;

    select p.id into v_target_user
      from public.profiles p
     where p.id <> auth.uid()
       and (
           (v_creditor.email is not null and p.email is not null and lower(p.email) = lower(v_creditor.email))
           or (v_creditor.phone is not null and p.phone is not null and p.phone = v_creditor.phone)
       )
     limit 1;

    if v_target_user is null then
        return null;
    end if;

    select * into v_caller from public.profiles where id = auth.uid();

    insert into public.debtors (
        id, user_id, full_name, phone, email, avatar_url, comment, currency,
        linked_user_id, mirror_creditor_id, created_at, updated_at
    ) values (
        gen_random_uuid(), v_target_user,
        coalesce(nullif(v_caller.display_name, ''), v_caller.email, v_creditor.full_name),
        v_caller.phone, v_caller.email, v_caller.avatar_url,
        null, v_creditor.currency,
        auth.uid(), p_creditor_id, now(), now()
    ) returning id into v_mirror_id;

    for v_tx in
        select * from public.creditor_transactions
         where creditor_id = p_creditor_id and is_deleted = false
         order by transaction_date
    loop
        insert into public.debt_transactions (
            id, debtor_id, user_id, amount, method,
            transaction_date, comment, mirror_transaction_id, created_at, updated_at
        ) values (
            gen_random_uuid(), v_mirror_id, v_target_user,
            v_tx.amount, v_tx.method,
            v_tx.transaction_date, v_tx.comment, v_tx.id, now(), now()
        );
    end loop;

    update public.creditors
       set linked_user_id = v_target_user,
           mirror_debtor_id = v_mirror_id
     where id = p_creditor_id;

    select -coalesce(sum(amount), 0) into v_balance
      from public.debt_transactions
     where debtor_id = v_mirror_id and is_deleted = false;

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_debtor_id, amount, currency, created_at
    ) values (
        gen_random_uuid(), v_target_user, 'CREDITOR_LINKED', auth.uid(),
        coalesce(nullif(v_caller.display_name, ''), v_caller.email),
        v_mirror_id, v_balance, v_creditor.currency, now()
    );

    return v_mirror_id;
end;
$$;

revoke all on function public.link_creditor_to_registered_user(uuid) from public, anon;
grant execute on function public.link_creditor_to_registered_user(uuid) to authenticated;

-- -----------------------------------------------------------------------------
-- 5. propagate_debt_transaction / propagate_creditor_transaction — тригери, що
--    дзеркалять кожну НОВУ/змінену транзакцію в пов'язаний акаунт, якщо
--    боржник/кредитор вже прив'язаний (linked_user_id заповнено).
--    Захист від рекурсії: рядок із заповненим mirror_transaction_id — сам
--    дзеркало, і ніколи не породжує нову пропагацію (return NEW одразу).
-- 5. propagate_debt_transaction / propagate_creditor_transaction — triggers
--    that mirror every NEW/changed transaction into the linked account, once
--    the debtor/creditor is linked (linked_user_id is set).
--    Recursion guard: a row with mirror_transaction_id already set is itself
--    a mirror, and never triggers another propagation (returns NEW immediately).
-- -----------------------------------------------------------------------------
create function public.propagate_debt_transaction()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_debtor  public.debtors%rowtype;
begin
    if NEW.mirror_transaction_id is not null then
        return NEW;
    end if;

    select * into v_debtor from public.debtors where id = NEW.debtor_id;
    if v_debtor.linked_user_id is null or v_debtor.mirror_creditor_id is null then
        return NEW;
    end if;

    if TG_OP = 'INSERT' then
        insert into public.creditor_transactions (
            id, creditor_id, user_id, amount, method,
            transaction_date, comment, mirror_transaction_id, created_at, updated_at
        ) values (
            gen_random_uuid(), v_debtor.mirror_creditor_id, v_debtor.linked_user_id,
            NEW.amount, NEW.method, NEW.transaction_date, NEW.comment,
            NEW.id, now(), now()
        );

        insert into public.notifications (
            id, user_id, type, actor_user_id, actor_display_name,
            related_creditor_id, amount, currency, created_at
        ) values (
            gen_random_uuid(), v_debtor.linked_user_id, 'DEBT_TRANSACTION_ADDED', v_debtor.user_id,
            (select coalesce(nullif(display_name, ''), email) from public.profiles where id = v_debtor.user_id),
            v_debtor.mirror_creditor_id, NEW.amount, v_debtor.currency, now()
        );
    elsif TG_OP = 'UPDATE' then
        update public.creditor_transactions
           set amount = NEW.amount,
               method = NEW.method,
               transaction_date = NEW.transaction_date,
               comment = NEW.comment,
               is_deleted = NEW.is_deleted
         where mirror_transaction_id = NEW.id;
    end if;

    return NEW;
end;
$$;

create trigger trg_propagate_debt_transaction
    after insert or update on public.debt_transactions
    for each row execute function public.propagate_debt_transaction();

create function public.propagate_creditor_transaction()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_creditor  public.creditors%rowtype;
begin
    if NEW.mirror_transaction_id is not null then
        return NEW;
    end if;

    select * into v_creditor from public.creditors where id = NEW.creditor_id;
    if v_creditor.linked_user_id is null or v_creditor.mirror_debtor_id is null then
        return NEW;
    end if;

    if TG_OP = 'INSERT' then
        insert into public.debt_transactions (
            id, debtor_id, user_id, amount, method,
            transaction_date, comment, mirror_transaction_id, created_at, updated_at
        ) values (
            gen_random_uuid(), v_creditor.mirror_debtor_id, v_creditor.linked_user_id,
            NEW.amount, NEW.method, NEW.transaction_date, NEW.comment,
            NEW.id, now(), now()
        );

        insert into public.notifications (
            id, user_id, type, actor_user_id, actor_display_name,
            related_debtor_id, amount, currency, created_at
        ) values (
            gen_random_uuid(), v_creditor.linked_user_id, 'CREDIT_TRANSACTION_ADDED', v_creditor.user_id,
            (select coalesce(nullif(display_name, ''), email) from public.profiles where id = v_creditor.user_id),
            v_creditor.mirror_debtor_id, NEW.amount, v_creditor.currency, now()
        );
    elsif TG_OP = 'UPDATE' then
        update public.debt_transactions
           set amount = NEW.amount,
               method = NEW.method,
               transaction_date = NEW.transaction_date,
               comment = NEW.comment,
               is_deleted = NEW.is_deleted
         where mirror_transaction_id = NEW.id;
    end if;

    return NEW;
end;
$$;

create trigger trg_propagate_creditor_transaction
    after insert or update on public.creditor_transactions
    for each row execute function public.propagate_creditor_transaction();

revoke all on function public.propagate_debt_transaction() from public, anon, authenticated;
revoke all on function public.propagate_creditor_transaction() from public, anon, authenticated;

commit;

-- =============================================================================
-- Кінець міграції 0007. / End of migration 0007.
-- =============================================================================
