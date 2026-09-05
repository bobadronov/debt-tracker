-- =============================================================================
-- DebtTracker — усунення B3 (security review): телефон як ключ автозв'язування
-- в дзеркалюванні боргів був нічим не верифікований (немає OTP ніде в
-- кодовій базі), тож будь-хто міг вписати чужий номер собі в профіль і стати
-- ціллю автоматичного дзеркалювання без жодної згоди.
--
-- Фікс: email-матч лишається миттєвим (Supabase Auth сам верифікує email при
-- реєстрації) — поведінка 0007 не змінюється. Телефон-матч більше не лінкує
-- одразу: створюється рядок у новій pending_link_requests, ціль отримує
-- сповіщення 'LINK_REQUEST' і сама вирішує approve_link_request/
-- reject_link_request — саме дзеркалювання (perform_debtor_link/
-- perform_creditor_link) виконується лише після явної згоди.
--
-- DebtTracker — closes B3 (security review): phone as an auto-link key in
-- debt mirroring was completely unverified (no OTP anywhere in the
-- codebase), so anyone could put a stranger's phone number in their own
-- profile and become the mirroring target with zero consent.
--
-- Fix: email match stays instant (Supabase Auth already verifies email at
-- signup) — 0007's behavior is unchanged there. A phone match no longer
-- links immediately: it creates a row in the new pending_link_requests
-- table, the target gets a 'LINK_REQUEST' notification, and only their own
-- approve_link_request/reject_link_request decides it — the actual
-- mirroring (perform_debtor_link/perform_creditor_link) only runs after
-- explicit consent.
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 1. pending_link_requests — запит на згоду цілі перед дзеркалюванням за
--    телефон-матчем. Інсертити/оновлювати можуть лише SECURITY DEFINER-функції
--    нижче — авторизованим користувачам дозволено лише select власних рядків
--    (як вимагач, так і ціль), як і в notifications.
-- 1. pending_link_requests — a consent request from the target before
--    mirroring starts on a phone match. Only the SECURITY DEFINER functions
--    below may insert/update — an authenticated user may only select their
--    own rows (as either requester or target), same as notifications.
-- -----------------------------------------------------------------------------
create table public.pending_link_requests (
    id                 uuid primary key default gen_random_uuid(),
    kind               text not null check (kind in ('debtor', 'creditor')),
    source_id          uuid not null,         -- id рядка в debtors/creditors вимагача / id of the requester's debtors/creditors row
    requester_user_id  uuid not null references auth.users (id) on delete cascade,
    target_user_id     uuid not null references auth.users (id) on delete cascade,
    status             text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
    created_at         timestamptz not null default now(),
    resolved_at        timestamptz
);

comment on table public.pending_link_requests is 'Запит на згоду перед дзеркалюванням боргу за телефон-матчем (B3-фікс, 0013) — approve_link_request/reject_link_request. Consent request before mirroring a phone-matched debt (B3 fix, 0013) — approve_link_request/reject_link_request.';

-- Один активний pending-запит на джерело: повторний виклик link_*_to_registered_user
-- для того самого боржника/кредитора не плодить дублікати сповіщень.
-- One active pending request per source row: calling link_*_to_registered_user
-- again for the same debtor/creditor doesn't spawn duplicate notifications.
create unique index idx_pending_link_requests_unique_pending
    on public.pending_link_requests (kind, source_id)
    where status = 'pending';

create index idx_pending_link_requests_target on public.pending_link_requests (target_user_id, status);

alter table public.pending_link_requests enable row level security;

create policy "pending_link_requests_requester_select" on public.pending_link_requests
    for select
    using (requester_user_id = auth.uid());

create policy "pending_link_requests_target_select" on public.pending_link_requests
    for select
    using (target_user_id = auth.uid());

-- -----------------------------------------------------------------------------
-- 2. notifications — нове поле для зв'язку зі своїм pending_link_requests, і
--    два нових типи в check-constraint.
-- 2. notifications — a new column pointing back to its pending_link_requests
--    row, and two new types in the check constraint.
-- -----------------------------------------------------------------------------
alter table public.notifications
    add column related_link_request_id uuid references public.pending_link_requests (id) on delete set null;

alter table public.notifications drop constraint notifications_type_check;

alter table public.notifications
    add constraint notifications_type_check check (
        type in (
            'DEBTOR_LINKED', 'CREDITOR_LINKED', 'DEBT_TRANSACTION_ADDED', 'CREDIT_TRANSACTION_ADDED',
            'LINK_REQUEST', 'LINK_REQUEST_APPROVED'
        )
    );

-- -----------------------------------------------------------------------------
-- 3. perform_debtor_link / perform_creditor_link — фактичне дзеркалювання,
--    винесене з link_debtor_to_registered_user/link_creditor_to_registered_user
--    (0007) у приватні функції: тепер їх викликають ДВА шляхи — миттєвий
--    email-матч і approve_link_request після згоди. p_requester_user_id
--    передається явно (а не auth.uid()), бо approve_link_request виконується
--    від імені ЦІЛІ, а не вимагача. Ніколи не виставлені через PostgREST —
--    той самий підхід, що й у propagate_debt_transaction (0007).
-- 3. perform_debtor_link / perform_creditor_link — the actual mirroring,
--    factored out of link_debtor_to_registered_user/link_creditor_to_registered_user
--    (0007) into private functions: now called from TWO paths — the instant
--    email match, and approve_link_request after consent. p_requester_user_id
--    is passed explicitly (not auth.uid()), since approve_link_request runs
--    as the TARGET, not the requester. Never exposed via PostgREST — same
--    approach as propagate_debt_transaction (0007).
-- -----------------------------------------------------------------------------
create function public.perform_debtor_link(p_debtor_id uuid, p_requester_user_id uuid, p_target_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_debtor     public.debtors%rowtype;
    v_caller     public.profiles%rowtype;
    v_mirror_id  uuid;
    v_tx         public.debt_transactions%rowtype;
    v_balance    numeric;
begin
    select * into v_debtor from public.debtors where id = p_debtor_id and user_id = p_requester_user_id;
    if not found then
        raise exception 'Debtor not found or not owned by requester';
    end if;

    select * into v_caller from public.profiles where id = p_requester_user_id;

    insert into public.creditors (
        id, user_id, full_name, phone, email, avatar_url, comment, currency,
        linked_user_id, mirror_debtor_id, created_at, updated_at
    ) values (
        gen_random_uuid(), p_target_user_id,
        coalesce(nullif(v_caller.display_name, ''), v_caller.email, v_debtor.full_name),
        v_caller.phone, v_caller.email, v_caller.avatar_url,
        null, v_debtor.currency,
        p_requester_user_id, p_debtor_id, now(), now()
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
            gen_random_uuid(), v_mirror_id, p_target_user_id,
            v_tx.amount, v_tx.method,
            v_tx.transaction_date, v_tx.comment, v_tx.id, now(), now()
        );
    end loop;

    update public.debtors
       set linked_user_id = p_target_user_id,
           mirror_creditor_id = v_mirror_id
     where id = p_debtor_id;

    select -coalesce(sum(amount), 0) into v_balance
      from public.creditor_transactions
     where creditor_id = v_mirror_id and is_deleted = false;

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_creditor_id, amount, currency, created_at
    ) values (
        gen_random_uuid(), p_target_user_id, 'DEBTOR_LINKED', p_requester_user_id,
        coalesce(nullif(v_caller.display_name, ''), v_caller.email),
        v_mirror_id, v_balance, v_debtor.currency, now()
    );

    return v_mirror_id;
end;
$$;

revoke all on function public.perform_debtor_link(uuid, uuid, uuid) from public, anon, authenticated;

create function public.perform_creditor_link(p_creditor_id uuid, p_requester_user_id uuid, p_target_user_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_creditor   public.creditors%rowtype;
    v_caller     public.profiles%rowtype;
    v_mirror_id  uuid;
    v_tx         public.creditor_transactions%rowtype;
    v_balance    numeric;
begin
    select * into v_creditor from public.creditors where id = p_creditor_id and user_id = p_requester_user_id;
    if not found then
        raise exception 'Creditor not found or not owned by requester';
    end if;

    select * into v_caller from public.profiles where id = p_requester_user_id;

    insert into public.debtors (
        id, user_id, full_name, phone, email, avatar_url, comment, currency,
        linked_user_id, mirror_creditor_id, created_at, updated_at
    ) values (
        gen_random_uuid(), p_target_user_id,
        coalesce(nullif(v_caller.display_name, ''), v_caller.email, v_creditor.full_name),
        v_caller.phone, v_caller.email, v_caller.avatar_url,
        null, v_creditor.currency,
        p_requester_user_id, p_creditor_id, now(), now()
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
            gen_random_uuid(), v_mirror_id, p_target_user_id,
            v_tx.amount, v_tx.method,
            v_tx.transaction_date, v_tx.comment, v_tx.id, now(), now()
        );
    end loop;

    update public.creditors
       set linked_user_id = p_target_user_id,
           mirror_debtor_id = v_mirror_id
     where id = p_creditor_id;

    select -coalesce(sum(amount), 0) into v_balance
      from public.debt_transactions
     where debtor_id = v_mirror_id and is_deleted = false;

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_debtor_id, amount, currency, created_at
    ) values (
        gen_random_uuid(), p_target_user_id, 'CREDITOR_LINKED', p_requester_user_id,
        coalesce(nullif(v_caller.display_name, ''), v_caller.email),
        v_mirror_id, v_balance, v_creditor.currency, now()
    );

    return v_mirror_id;
end;
$$;

revoke all on function public.perform_creditor_link(uuid, uuid, uuid) from public, anon, authenticated;

-- -----------------------------------------------------------------------------
-- 4. link_debtor_to_registered_user / link_creditor_to_registered_user —
--    email-матч дзеркалить одразу (як 0007), phone-матч тепер лише створює
--    pending_link_requests + сповіщення 'LINK_REQUEST' і повертає null.
--    Grants з 0007 (execute → authenticated) лишаються чинними: CREATE OR
--    REPLACE не змінює привілеї на функцію з тим самим підписом.
-- 4. link_debtor_to_registered_user / link_creditor_to_registered_user —
--    an email match mirrors instantly (as in 0007), a phone match now only
--    creates a pending_link_requests row + a 'LINK_REQUEST' notification and
--    returns null. Grants from 0007 (execute → authenticated) still apply:
--    CREATE OR REPLACE doesn't change privileges on a same-signature function.
-- -----------------------------------------------------------------------------
create or replace function public.link_debtor_to_registered_user(p_debtor_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_debtor             public.debtors%rowtype;
    v_target_user_email  uuid;
    v_target_user_phone  uuid;
    v_caller_name        text;
    v_request_id         uuid;
begin
    select * into v_debtor from public.debtors where id = p_debtor_id and user_id = auth.uid();
    if not found then
        raise exception 'Debtor not found or not owned by caller';
    end if;

    if v_debtor.linked_user_id is not null then
        return v_debtor.mirror_creditor_id;
    end if;

    select p.id into v_target_user_email
      from public.profiles p
     where p.id <> auth.uid()
       and v_debtor.email is not null and p.email is not null
       and lower(p.email) = lower(v_debtor.email)
     limit 1;

    if v_target_user_email is not null then
        return public.perform_debtor_link(p_debtor_id, auth.uid(), v_target_user_email);
    end if;

    select p.id into v_target_user_phone
      from public.profiles p
     where p.id <> auth.uid()
       and v_debtor.phone is not null and p.phone is not null
       and p.phone = v_debtor.phone
     limit 1;

    if v_target_user_phone is null then
        return null;
    end if;

    select coalesce(nullif(display_name, ''), email) into v_caller_name
      from public.profiles where id = auth.uid();

    insert into public.pending_link_requests (kind, source_id, requester_user_id, target_user_id)
    values ('debtor', p_debtor_id, auth.uid(), v_target_user_phone)
    on conflict (kind, source_id) where status = 'pending' do nothing
    returning id into v_request_id;

    if v_request_id is not null then
        insert into public.notifications (
            id, user_id, type, actor_user_id, actor_display_name,
            related_link_request_id, created_at
        ) values (
            gen_random_uuid(), v_target_user_phone, 'LINK_REQUEST', auth.uid(),
            v_caller_name, v_request_id, now()
        );
    end if;

    return null;
end;
$$;

create or replace function public.link_creditor_to_registered_user(p_creditor_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_creditor           public.creditors%rowtype;
    v_target_user_email  uuid;
    v_target_user_phone  uuid;
    v_caller_name        text;
    v_request_id         uuid;
begin
    select * into v_creditor from public.creditors where id = p_creditor_id and user_id = auth.uid();
    if not found then
        raise exception 'Creditor not found or not owned by caller';
    end if;

    if v_creditor.linked_user_id is not null then
        return v_creditor.mirror_debtor_id;
    end if;

    select p.id into v_target_user_email
      from public.profiles p
     where p.id <> auth.uid()
       and v_creditor.email is not null and p.email is not null
       and lower(p.email) = lower(v_creditor.email)
     limit 1;

    if v_target_user_email is not null then
        return public.perform_creditor_link(p_creditor_id, auth.uid(), v_target_user_email);
    end if;

    select p.id into v_target_user_phone
      from public.profiles p
     where p.id <> auth.uid()
       and v_creditor.phone is not null and p.phone is not null
       and p.phone = v_creditor.phone
     limit 1;

    if v_target_user_phone is null then
        return null;
    end if;

    select coalesce(nullif(display_name, ''), email) into v_caller_name
      from public.profiles where id = auth.uid();

    insert into public.pending_link_requests (kind, source_id, requester_user_id, target_user_id)
    values ('creditor', p_creditor_id, auth.uid(), v_target_user_phone)
    on conflict (kind, source_id) where status = 'pending' do nothing
    returning id into v_request_id;

    if v_request_id is not null then
        insert into public.notifications (
            id, user_id, type, actor_user_id, actor_display_name,
            related_link_request_id, created_at
        ) values (
            gen_random_uuid(), v_target_user_phone, 'LINK_REQUEST', auth.uid(),
            v_caller_name, v_request_id, now()
        );
    end if;

    return null;
end;
$$;

-- -----------------------------------------------------------------------------
-- 5. approve_link_request / reject_link_request — рішення ЦІЛІ по
--    pending-запиту. approve виконує фактичне дзеркалювання (п.3) і сповіщає
--    вимагача; reject лише позначає рядок — вимагач мовчки не отримує лінк
--    (без сповіщення про відмову, щоб не підказувати зловмиснику, що номер
--    належить конкретному акаунту).
-- 5. approve_link_request / reject_link_request — the TARGET's decision on a
--    pending request. approve runs the actual mirroring (section 3) and
--    notifies the requester; reject just flips the row — the requester
--    silently never gets linked (no rejection notification, so as not to
--    confirm to an attacker that the phone number belongs to a real account).
-- -----------------------------------------------------------------------------
create function public.approve_link_request(p_request_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_request         public.pending_link_requests%rowtype;
    v_mirror_id       uuid;
    v_requester_name  text;
begin
    select * into v_request from public.pending_link_requests
     where id = p_request_id and target_user_id = auth.uid() and status = 'pending';
    if not found then
        raise exception 'Link request not found, not yours, or already resolved';
    end if;

    if v_request.kind = 'debtor' then
        v_mirror_id := public.perform_debtor_link(v_request.source_id, v_request.requester_user_id, auth.uid());
    else
        v_mirror_id := public.perform_creditor_link(v_request.source_id, v_request.requester_user_id, auth.uid());
    end if;

    update public.pending_link_requests
       set status = 'approved', resolved_at = now()
     where id = p_request_id;

    select coalesce(nullif(display_name, ''), email) into v_requester_name
      from public.profiles where id = auth.uid();

    insert into public.notifications (
        id, user_id, type, actor_user_id, actor_display_name,
        related_debtor_id, related_creditor_id, related_link_request_id, created_at
    ) values (
        gen_random_uuid(), v_request.requester_user_id, 'LINK_REQUEST_APPROVED', auth.uid(),
        v_requester_name,
        case when v_request.kind = 'debtor' then v_request.source_id end,
        case when v_request.kind = 'creditor' then v_request.source_id end,
        p_request_id, now()
    );

    return v_mirror_id;
end;
$$;

revoke all on function public.approve_link_request(uuid) from public, anon;
grant execute on function public.approve_link_request(uuid) to authenticated;

create function public.reject_link_request(p_request_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.pending_link_requests
       set status = 'rejected', resolved_at = now()
     where id = p_request_id and target_user_id = auth.uid() and status = 'pending';

    if not found then
        raise exception 'Link request not found, not yours, or already resolved';
    end if;
end;
$$;

revoke all on function public.reject_link_request(uuid) from public, anon;
grant execute on function public.reject_link_request(uuid) to authenticated;

commit;

-- =============================================================================
-- Кінець міграції 0013. / End of migration 0013.
--
-- Ручна перевірка завислих pending-запитів (запустити ОКРЕМО, вручну — не
-- частина цієї міграції):
-- Manual check for stale pending requests (run SEPARATELY, by hand — not
-- part of this migration):
--
--   -- Pending довше 30 днів — ціль, ймовірно, ніколи не побачить сповіщення.
--   -- Pending for more than 30 days — the target likely never saw the notification.
--   select * from public.pending_link_requests
--    where status = 'pending' and created_at < now() - interval '30 days';
--
--   -- source_id, що більше не існує (боржника/кредитора видалили до рішення цілі).
--   -- source_id that no longer exists (debtor/creditor deleted before the target decided).
--   select r.* from public.pending_link_requests r
--    where r.status = 'pending'
--      and r.kind = 'debtor'
--      and not exists (select 1 from public.debtors d where d.id = r.source_id);
--
--   select r.* from public.pending_link_requests r
--    where r.status = 'pending'
--      and r.kind = 'creditor'
--      and not exists (select 1 from public.creditors c where c.id = r.source_id);
-- =============================================================================
