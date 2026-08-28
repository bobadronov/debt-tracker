-- =============================================================================
-- DebtTracker — повний бекап схеми БД / full schema backup
--
-- Це НЕ ще одна міграція — не кладіть цей файл у supabase/migrations/ і не
-- викликайте його через `supabase db push`. Це знімок кінцевого стану схеми
-- (еквівалент послідовного застосування 0001_init.sql → 0009_notifications_
-- cleanup.sql на порожньому проєкті + ручного `profiles.avatar_url` і
-- бакета Storage `avatars`, які на проді додано поза версійованими
-- міграціями), призначений для АВАРІЙНОГО відновлення: якщо Supabase-проєкт
-- втрачено/видалено, створіть новий проєкт і виконайте увесь цей файл один
-- раз (SQL Editor або `psql -f`) — отримаєте ідентичну робочу схему.
--
-- This is NOT another migration — do not put this file in supabase/migrations/
-- or run it via `supabase db push`. It is a point-in-time snapshot of the
-- final schema (equivalent to applying 0001_init.sql → 0009_notifications_
-- cleanup.sql in order on an empty project, plus the manual `profiles.
-- avatar_url` column and the `avatars` Storage bucket, both added on
-- production outside versioned migrations). It exists for DISASTER
-- RECOVERY: if the Supabase project is ever lost/deleted, create a fresh
-- project and run this whole file once (SQL Editor or `psql -f`) to get back
-- an identical, working schema.
--
-- Verified against the live `debt-tracker` project (ref nywvasgnbgnixfjzadbu)
-- via the Supabase MCP `list_tables`/`list_migrations`/`execute_sql` tools on
-- 2026-08-08, folded forward through migration 0009 by hand on 2026-08-28 —
-- table/column/function/policy/storage definitions below should match
-- production; re-verify with `list_tables`/`list_migrations` after applying
-- 0009 live.
--
-- What this file does NOT contain: user data (auth.users, profiles/debtors/
-- creditors rows, transactions). This app is offline-first — every device
-- keeps a full local Room copy of its own account's data, so the realistic
-- restore path is: recreate the schema from this file, point the app at the
-- new SUPABASE_URL/SUPABASE_ANON_KEY, sign back in, and let the existing
-- local databases re-push everything (syncStatus = PENDING) to the empty
-- backend. A real `pg_dump --data-only` is only needed if every device's
-- local Room DB is *also* gone; take one from the Supabase Dashboard →
-- Database → Backups if that ever applies.
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 0. Розширення / Extensions
-- -----------------------------------------------------------------------------
create extension if not exists pgcrypto;

-- -----------------------------------------------------------------------------
-- 1. profiles — 1:1 з auth.users, налаштування користувача
-- 1. profiles — 1:1 with auth.users, per-user app settings
-- -----------------------------------------------------------------------------
create table public.profiles (
    id                  uuid primary key references auth.users (id) on delete cascade,
    display_name        text,
    protection_enabled  boolean not null default false,
    biometric_enabled   boolean not null default false,
    sound_enabled       boolean not null default true,
    haptic_enabled      boolean not null default true,
    theme               text not null default 'system' check (theme in ('light', 'dark', 'system')),
    locale              text not null default 'uk' check (locale in ('uk', 'en')),
    avatar_url          text,
    email               text,
    phone               text,          -- телефон акаунта (екран "Мій акаунт"), окремо від phone контактів у debtors/creditors / account phone ("My account" screen), distinct from debtor/creditor contact phones
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

comment on table public.profiles is 'Профіль/налаштування користувача (1:1 з auth.users). User profile/settings (1:1 with auth.users).';

create unique index idx_profiles_email_lower
    on public.profiles (lower(email))
    where email is not null;

create index idx_profiles_phone
    on public.profiles (phone)
    where phone is not null;

-- -----------------------------------------------------------------------------
-- 2. debtors — люди, які винні мені (я дав у борг)
-- 2. debtors — people who owe me money (I lent to them)
-- -----------------------------------------------------------------------------
create table public.debtors (
    id            uuid primary key default gen_random_uuid(),  -- генерується клієнтом (offline-first) / client-generated (offline-first)
    user_id       uuid not null references auth.users (id) on delete cascade,
    full_name     text not null,
    phone         text,
    email         text,
    avatar_url    text,
    comment       text,
    status        text not null default 'ACTIVE' check (status in ('ACTIVE', 'CLOSED')),
    currency      text not null default 'UAH' check (currency in ('UAH', 'USD', 'PLN', 'EUR')),
    is_deleted    boolean not null default false,               -- soft delete для синхронізації / soft delete for sync
    linked_user_id      uuid references auth.users (id) on delete set null,  -- auth.uid() зареєстрованого користувача, знайденого за phone/email / auth.uid() of the registered user resolved from phone/email
    mirror_creditor_id  uuid,                                   -- id дзеркального рядка в creditors (акаунт linked_user_id) / id of the mirroring row in creditors (linked_user_id's account)
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()            -- джерело для Last-Write-Wins / source for Last-Write-Wins
);

comment on table public.debtors is 'Боржники: люди, яким я дав у борг. Debtors: people I lent money to.';
comment on column public.debtors.status is 'ACTIVE/CLOSED, денормалізовано, перераховується тригером із debt_transactions. Denormalized, recalculated by trigger from debt_transactions.';

-- -----------------------------------------------------------------------------
-- 3. debt_transactions — транзакції за боржниками (LEND/REPAY)
-- 3. debt_transactions — transactions against debtors (LEND/REPAY)
-- -----------------------------------------------------------------------------
create table public.debt_transactions (
    id                 uuid primary key default gen_random_uuid(),
    debtor_id          uuid not null references public.debtors (id) on delete cascade,
    user_id            uuid not null references auth.users (id) on delete cascade,  -- дублюється для простих RLS без join / duplicated for simple join-free RLS
    amount             numeric(14, 2) not null,                 -- зі знаком: + REPAY (зелений), - LEND (червоний) / signed: + REPAY (green), - LEND (red)
    method             text not null check (method in ('CASH', 'CARD')),
    transaction_date   timestamptz not null,
    comment            text,
    is_deleted         boolean not null default false,
    mirror_transaction_id  uuid,                                -- заповнено ЛИШЕ на авто-дзеркалі — id джерела в creditor_transactions / populated ONLY on an auto-mirror row — id of its source in creditor_transactions
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now()
);

comment on table public.debt_transactions is 'Транзакції боржників: LEND (я дав, amount<0) / REPAY (мені повернули, amount>0). Debtor transactions: LEND (I lent, amount<0) / REPAY (repaid to me, amount>0).';

-- -----------------------------------------------------------------------------
-- 4. creditors — люди, яким я винен (дзеркало debtors, п.4.1)
-- 4. creditors — people I owe money to (mirror of debtors, spec section 4.1)
-- -----------------------------------------------------------------------------
create table public.creditors (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid not null references auth.users (id) on delete cascade,
    full_name     text not null,
    phone         text,
    email         text,
    avatar_url    text,
    comment       text,
    status        text not null default 'ACTIVE' check (status in ('ACTIVE', 'CLOSED')),
    currency      text not null default 'UAH' check (currency in ('UAH', 'USD', 'PLN', 'EUR')),
    is_deleted    boolean not null default false,
    linked_user_id     uuid references auth.users (id) on delete set null,   -- дзеркало debtors.linked_user_id / mirror of debtors.linked_user_id
    mirror_debtor_id   uuid,                                    -- id дзеркального рядка в debtors (акаунт linked_user_id) / id of the mirroring row in debtors (linked_user_id's account)
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

comment on table public.creditors is 'Кредитори: люди, яким я винен. Creditors: people I owe money to.';
comment on column public.creditors.status is 'ACTIVE/CLOSED, денормалізовано, перераховується тригером із creditor_transactions. Denormalized, recalculated by trigger from creditor_transactions.';

-- -----------------------------------------------------------------------------
-- 5. creditor_transactions — транзакції за кредиторами (BORROW/RETURN)
-- 5. creditor_transactions — transactions against creditors (BORROW/RETURN)
-- -----------------------------------------------------------------------------
create table public.creditor_transactions (
    id                 uuid primary key default gen_random_uuid(),
    creditor_id        uuid not null references public.creditors (id) on delete cascade,
    user_id            uuid not null references auth.users (id) on delete cascade,
    amount             numeric(14, 2) not null,                 -- зі знаком: + RETURN (зелений), - BORROW (червоний) / signed: + RETURN (green), - BORROW (red)
    method             text not null check (method in ('CASH', 'CARD')),
    transaction_date   timestamptz not null,
    comment            text,
    is_deleted         boolean not null default false,
    mirror_transaction_id  uuid,                                -- заповнено ЛИШЕ на авто-дзеркалі — id джерела в debt_transactions / populated ONLY on an auto-mirror row — id of its source in debt_transactions
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now()
);

comment on table public.creditor_transactions is 'Транзакції кредиторів: BORROW (я взяв, amount<0) / RETURN (я повернув, amount>0). Creditor transactions: BORROW (I borrowed, amount<0) / RETURN (I paid back, amount>0).';

-- -----------------------------------------------------------------------------
-- 5a. user_sessions — активні пристрої акаунта + віддалений вихід (0006).
-- 5a. user_sessions — account's active devices + remote logout (0006).
-- Кожен пристрій реєструє/оновлює свій рядок при вході; "віддалений вихід" —
-- soft-revoke (revoked_at), клієнт-жертва бачить це через Realtime і сам себе
-- розлогінює. Each device registers/updates its own row at sign-in; "remote
-- logout" is a soft-revoke (revoked_at), the victim client sees it via
-- Realtime and signs itself out.
-- -----------------------------------------------------------------------------
create table public.user_sessions (
    id            uuid primary key,                        -- клієнтський device id / client device id
    user_id       uuid not null references auth.users (id) on delete cascade,
    device_name   text not null,
    platform      text not null check (platform in ('ANDROID', 'IOS', 'DESKTOP', 'WEB')),
    app_version   text,
    created_at    timestamptz not null default now(),
    last_seen_at  timestamptz not null default now(),
    revoked_at    timestamptz
);

comment on table public.user_sessions is 'Активні пристрої акаунта для екрана "Session management". Account''s active devices for the "Session management" screen.';

create index user_sessions_user_id_idx on public.user_sessions (user_id);

alter table public.user_sessions enable row level security;

create policy "user_sessions_owner_all" on public.user_sessions
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- -----------------------------------------------------------------------------
-- 5b. notifications — сповіщення про прив'язку/нові транзакції в дзеркальному
--     борзі (0007), опитується клієнтом кожні 15с (не Realtime).
-- 5b. notifications — notifications about linking/new transactions on a
--     mirrored debt (0007), polled by the client every 15s (not Realtime).
-- -----------------------------------------------------------------------------
create table public.notifications (
    id                   uuid primary key default gen_random_uuid(),
    user_id              uuid not null references auth.users (id) on delete cascade,  -- отримувач / recipient
    type                 text not null check (type in ('DEBTOR_LINKED', 'CREDITOR_LINKED', 'DEBT_TRANSACTION_ADDED', 'CREDIT_TRANSACTION_ADDED')),
    actor_user_id        uuid references auth.users (id) on delete set null,
    actor_display_name   text,          -- знімок імені актора на момент створення / snapshot of the actor's name at creation time
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

-- -----------------------------------------------------------------------------
-- 6. handle_new_user — авто-створення profiles-рядка після реєстрації
-- 6. handle_new_user — auto-create a profiles row right after signup
-- (кінцева форма з 0004: одразу копіює email; search_path зафіксовано, як у 0002)
-- (final form from 0004: copies email immediately; search_path pinned, per 0002)
-- -----------------------------------------------------------------------------
create function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, email) values (new.id, new.email);
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

revoke all on function public.handle_new_user() from public, anon, authenticated;

-- -----------------------------------------------------------------------------
-- 7. updated_at auto-touch — універсальний тригер для всіх таблиць
-- 7. updated_at auto-touch — generic trigger for all tables
-- -----------------------------------------------------------------------------
create function public.touch_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger trg_touch_updated_at_profiles
    before update on public.profiles
    for each row execute function public.touch_updated_at();

create trigger trg_touch_updated_at_debt_transactions
    before update on public.debt_transactions
    for each row execute function public.touch_updated_at();

create trigger trg_touch_updated_at_creditor_transactions
    before update on public.creditor_transactions
    for each row execute function public.touch_updated_at();

-- Примітка: debtors.updated_at і creditors.updated_at НЕ чіпаються цим
-- тригером — їх оновлює виключно recalc-тригер нижче (п.8), щоб уникнути
-- подвійного запису при кожному ручному редагуванні картки боржника.
-- Note: debtors.updated_at / creditors.updated_at are intentionally NOT
-- touched by this generic trigger — only the recalc trigger below (section 8)
-- updates them, to avoid a double write on every manual debtor/creditor edit.
create trigger trg_touch_updated_at_debtors
    before update on public.debtors
    for each row execute function public.touch_updated_at();

create trigger trg_touch_updated_at_creditors
    before update on public.creditors
    for each row execute function public.touch_updated_at();

revoke all on function public.touch_updated_at() from public, anon, authenticated;

-- -----------------------------------------------------------------------------
-- 8. Перерахунок status/updated_at при зміні транзакцій
-- 8. Recalculate status/updated_at whenever transactions change
-- balance = -SUM(amount); status = CLOSED якщо balance <= 0
-- balance = -SUM(amount); status = CLOSED if balance <= 0
-- -----------------------------------------------------------------------------
create function public.recalc_debtor_status()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_debtor_id uuid := coalesce(new.debtor_id, old.debtor_id);
    v_balance   numeric;
begin
    select -coalesce(sum(amount), 0)
      into v_balance
      from public.debt_transactions
     where debtor_id = v_debtor_id
       and is_deleted = false;

    update public.debtors
       set status     = case when v_balance <= 0 then 'CLOSED' else 'ACTIVE' end,
           updated_at = now()
     where id = v_debtor_id;

    return null; -- AFTER-тригер, результат ігнорується / AFTER trigger, return value ignored
end;
$$;

create trigger trg_recalc_debtor_status
    after insert or update or delete on public.debt_transactions
    for each row execute function public.recalc_debtor_status();

create function public.recalc_creditor_status()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_creditor_id uuid := coalesce(new.creditor_id, old.creditor_id);
    v_balance     numeric;
begin
    select -coalesce(sum(amount), 0)
      into v_balance
      from public.creditor_transactions
     where creditor_id = v_creditor_id
       and is_deleted = false;

    update public.creditors
       set status     = case when v_balance <= 0 then 'CLOSED' else 'ACTIVE' end,
           updated_at = now()
     where id = v_creditor_id;

    return null;
end;
$$;

create trigger trg_recalc_creditor_status
    after insert or update or delete on public.creditor_transactions
    for each row execute function public.recalc_creditor_status();

revoke all on function public.recalc_debtor_status() from public, anon, authenticated;
revoke all on function public.recalc_creditor_status() from public, anon, authenticated;

-- -----------------------------------------------------------------------------
-- 9. find_profile_by_email — пошук за email для автозаповнення в AddEdit-формі
-- 9. find_profile_by_email — email lookup for AddEdit-form autofill
-- RLS на profiles суворо власницький (id = auth.uid()), тож пряме читання
-- чужого рядка неможливе — ця SECURITY DEFINER-функція навмисно обходить це,
-- повертаючи лише безпечні публічні поля (без email/id) для ОДНОГО збігу.
-- profiles RLS is strictly owner-only (id = auth.uid()), so a direct read of
-- another user's row is impossible — this SECURITY DEFINER function
-- deliberately bypasses that, returning only safe public fields (no email/id)
-- for a SINGLE email match.
-- -----------------------------------------------------------------------------
create function public.find_profile_by_email(p_email text)
returns table (
    display_name text,
    avatar_url text
)
language sql
stable
security definer
set search_path = public
as $$
    select p.display_name, p.avatar_url
      from public.profiles p
     where p.email is not null
       and lower(p.email) = lower(p_email)
     limit 1;
$$;

revoke all on function public.find_profile_by_email(text) from public, anon;
grant execute on function public.find_profile_by_email(text) to authenticated;

-- -----------------------------------------------------------------------------
-- 10. delete_all_user_data — повне видалення даних користувача
-- 10. delete_all_user_data — hard-delete all of a user's data
-- Викликається з Settings "Видалити всі дані" (подвійне підтвердження в UI).
-- Called from Settings "Delete all data" (double-confirmation in the UI).
-- Реальний DELETE (не soft), profiles-рядок (налаштування) не чіпається.
-- Real hard DELETE (not soft), the profiles row (settings) is left untouched.
-- -----------------------------------------------------------------------------
create function public.delete_all_user_data(uid uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if auth.uid() is distinct from uid then
        raise exception 'Заборонено: можна видаляти лише власні дані. Forbidden: can only delete your own data.';
    end if;

    delete from public.debt_transactions      where user_id = uid;
    delete from public.debtors                where user_id = uid;
    delete from public.creditor_transactions  where user_id = uid;
    delete from public.creditors              where user_id = uid;
end;
$$;

revoke all on function public.delete_all_user_data(uuid) from public, anon, authenticated;
grant execute on function public.delete_all_user_data(uuid) to authenticated;

-- -----------------------------------------------------------------------------
-- 10a. link_debtor_to_registered_user / link_creditor_to_registered_user (0007)
--      Прив'язує боржника/кредитора до зареєстрованого користувача за
--      phone/email, дзеркалить усі наявні транзакції, сповіщає. Ідемпотентні.
-- 10a. link_debtor_to_registered_user / link_creditor_to_registered_user
--      (0007). Links a debtor/creditor to a registered user by phone/email,
--      mirrors all existing transactions, notifies. Idempotent.
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
-- 10b. propagate_debt_transaction / propagate_creditor_transaction (0007)
--      Тригери, що дзеркалять кожну нову/змінену транзакцію в пов'язаний
--      акаунт. Захист від рекурсії: рядок із заповненим mirror_transaction_id
--      — сам дзеркало, і ніколи не породжує нову пропагацію.
-- 10b. propagate_debt_transaction / propagate_creditor_transaction (0007)
--      Triggers that mirror every new/changed transaction into the linked
--      account. Recursion guard: a row with mirror_transaction_id already set
--      is itself a mirror, and never triggers another propagation.
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

    if TG_OP = 'UPDATE'
       and NEW.amount is not distinct from OLD.amount
       and NEW.method is not distinct from OLD.method
       and NEW.transaction_date is not distinct from OLD.transaction_date
       and NEW.comment is not distinct from OLD.comment
       and NEW.is_deleted is not distinct from OLD.is_deleted then
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

    if TG_OP = 'UPDATE'
       and NEW.amount is not distinct from OLD.amount
       and NEW.method is not distinct from OLD.method
       and NEW.transaction_date is not distinct from OLD.transaction_date
       and NEW.comment is not distinct from OLD.comment
       and NEW.is_deleted is not distinct from OLD.is_deleted then
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

-- -----------------------------------------------------------------------------
-- 11. Storage — бакет "avatars" для фото акаунта (SettingsScreen.AccountAvatar)
-- 11. Storage — "avatars" bucket for the account photo (SettingsScreen.AccountAvatar)
-- Клієнт (SupabaseAuthRepository.updateAvatar) вантажить на шлях
-- "avatars/{auth.uid()}/avatar.{ext}" і зберігає публічний URL у
-- profiles.avatar_url. Публічний бакет (аватарки не є секретом), але
-- запис/зміна/видалення дозволені лише у власну "теку" (perша частина шляху
-- == auth.uid()).
-- The client (SupabaseAuthRepository.updateAvatar) uploads to
-- "avatars/{auth.uid()}/avatar.{ext}" and stores the public URL in
-- profiles.avatar_url. Public bucket (avatars aren't secret), but writes/
-- updates/deletes are only allowed inside the caller's own "folder" (first
-- path segment == auth.uid()).
-- -----------------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('avatars', 'avatars', true)
on conflict (id) do nothing;

create policy "Avatar images are publicly accessible" on storage.objects
    for select
    using (bucket_id = 'avatars');

create policy "Users can upload their own avatar" on storage.objects
    for insert
    with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "Users can update their own avatar" on storage.objects
    for update
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "Users can delete their own avatar" on storage.objects
    for delete
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

-- -----------------------------------------------------------------------------
-- 12. Row Level Security — жорстка ізоляція між користувачами
-- 12. Row Level Security — strict per-user isolation
-- -----------------------------------------------------------------------------
alter table public.profiles              enable row level security;
alter table public.debtors                enable row level security;
alter table public.debt_transactions      enable row level security;
alter table public.creditors              enable row level security;
alter table public.creditor_transactions  enable row level security;

create policy "profiles_owner_all" on public.profiles
    for all
    using (id = auth.uid())
    with check (id = auth.uid());

create policy "debtors_owner_all" on public.debtors
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "debt_transactions_owner_all" on public.debt_transactions
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "creditors_owner_all" on public.creditors
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "creditor_transactions_owner_all" on public.creditor_transactions
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- -----------------------------------------------------------------------------
-- 13. Індекси / Indexes
-- -----------------------------------------------------------------------------
create index idx_debtors_user_deleted_status
    on public.debtors (user_id, is_deleted, status);

create index idx_debt_transactions_debtor_deleted_date
    on public.debt_transactions (debtor_id, is_deleted, transaction_date desc);

create index idx_creditors_user_deleted_status
    on public.creditors (user_id, is_deleted, status);

create index idx_creditor_transactions_creditor_deleted_date
    on public.creditor_transactions (creditor_id, is_deleted, transaction_date desc);

create index idx_debtors_linked_user
    on public.debtors (linked_user_id) where linked_user_id is not null;

create index idx_creditors_linked_user
    on public.creditors (linked_user_id) where linked_user_id is not null;

create index idx_debt_transactions_mirror
    on public.debt_transactions (mirror_transaction_id) where mirror_transaction_id is not null;

create index idx_creditor_transactions_mirror
    on public.creditor_transactions (mirror_transaction_id) where mirror_transaction_id is not null;

-- -----------------------------------------------------------------------------
-- 14. Realtime — підписка з клієнта (supabase-kt realtime-kt)
-- 14. Realtime — client-side subscription (supabase-kt realtime-kt)
-- -----------------------------------------------------------------------------
alter publication supabase_realtime add table
    public.debtors,
    public.debt_transactions,
    public.creditors,
    public.creditor_transactions,
    public.user_sessions,
    public.notifications;

commit;

-- =============================================================================
-- Після відновлення / After restoring:
--   1. Auth → Providers: увімкнути Email (та інші провайдери, якщо вони
--      використовувались). Auth → Providers: enable Email (and any other
--      providers that were in use).
--   2. Скопіювати новий Project URL / anon key у secrets.properties
--      (див. secrets.properties.example) і в GitHub Actions secrets
--      SUPABASE_URL / SUPABASE_ANON_KEY (Settings → Secrets → Actions).
--      Copy the new Project URL / anon key into secrets.properties (see
--      secrets.properties.example) and into the GitHub Actions secrets
--      SUPABASE_URL / SUPABASE_ANON_KEY (Settings → Secrets → Actions).
--   3. (Опційно) Auth → Policies → увімкнути Leaked Password Protection —
--      єдине залишкове попередження advisor'а типу security, яке не
--      стосується схеми. (Optional) Auth → Policies → enable Leaked Password
--      Protection — the one remaining security-advisor warning that isn't
--      schema-related.
-- =============================================================================
