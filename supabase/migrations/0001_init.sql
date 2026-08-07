-- =============================================================================
-- DebtTracker — Фаза 0: початкова схема БД
-- DebtTracker — Phase 0: initial database schema
--
-- Виконати один раз на щойно створеному Supabase-проєкті:
--   supabase db push
-- або вручну в Supabase Dashboard → SQL Editor.
--
-- Run once on a freshly created Supabase project:
--   supabase db push
-- or manually in Supabase Dashboard → SQL Editor.
--
-- Відповідає доменній моделі з debt-tracker-kmp-spec.md, розділ 4/4.1/9.
-- Matches the domain model in debt-tracker-kmp-spec.md, section 4/4.1/9.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Розширення / Extensions
-- gen_random_uuid() — на випадок ручних INSERT без клієнтського id
-- gen_random_uuid() — fallback for manual INSERTs without a client-supplied id
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
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

comment on table public.profiles is 'Профіль/налаштування користувача (1:1 з auth.users). User profile/settings (1:1 with auth.users).';

-- Автоматичне створення profiles-рядка після реєстрації в auth.users.
-- Auto-create a profiles row right after a new auth.users signup.
create function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id) values (new.id);
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- -----------------------------------------------------------------------------
-- 2. debtors — люди, які винні мені (я дав у борг)
-- 2. debtors — people who owe me money (I lent to them)
-- -----------------------------------------------------------------------------
create table public.debtors (
    id            uuid primary key default gen_random_uuid(),  -- генерується клієнтом (offline-first) / client-generated (offline-first)
    user_id       uuid not null references auth.users (id) on delete cascade,
    full_name     text not null,
    phone         text,
    avatar_url    text,
    comment       text,
    status        text not null default 'ACTIVE' check (status in ('ACTIVE', 'CLOSED')),
    is_deleted    boolean not null default false,               -- soft delete для синхронізації / soft delete for sync
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
    card_last_digits   text,                                    -- nullable, тільки якщо method = 'CARD' / nullable, only if method = 'CARD'
    transaction_date   timestamptz not null,
    comment            text,
    is_deleted         boolean not null default false,
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
    avatar_url    text,
    comment       text,
    status        text not null default 'ACTIVE' check (status in ('ACTIVE', 'CLOSED')),
    is_deleted    boolean not null default false,
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
    card_last_digits   text,
    transaction_date   timestamptz not null,
    comment            text,
    is_deleted         boolean not null default false,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now()
);

comment on table public.creditor_transactions is 'Транзакції кредиторів: BORROW (я взяв, amount<0) / RETURN (я повернув, amount>0). Creditor transactions: BORROW (I borrowed, amount<0) / RETURN (I paid back, amount>0).';

-- -----------------------------------------------------------------------------
-- 6. updated_at auto-touch — універсальний тригер для всіх таблиць
-- 6. updated_at auto-touch — generic trigger for all tables
-- -----------------------------------------------------------------------------
create function public.touch_updated_at()
returns trigger
language plpgsql
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
-- тригером — їх оновлює виключно recalc-тригер нижче (п.7), щоб уникнути
-- подвійного запису при кожному ручному редагуванні картки боржника.
-- Note: debtors.updated_at / creditors.updated_at are intentionally NOT
-- touched by this generic trigger — only the recalc trigger below (section 7)
-- updates them, to avoid a double write on every manual debtor/creditor edit.
create trigger trg_touch_updated_at_debtors
    before update on public.debtors
    for each row execute function public.touch_updated_at();

create trigger trg_touch_updated_at_creditors
    before update on public.creditors
    for each row execute function public.touch_updated_at();

-- -----------------------------------------------------------------------------
-- 7. Перерахунок status/updated_at при зміні транзакцій
-- 7. Recalculate status/updated_at whenever transactions change
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

-- -----------------------------------------------------------------------------
-- 8. Row Level Security — жорстка ізоляція між користувачами
-- 8. Row Level Security — strict per-user isolation
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
-- 9. Індекси / Indexes
-- -----------------------------------------------------------------------------
create index idx_debtors_user_deleted_status
    on public.debtors (user_id, is_deleted, status);

create index idx_debt_transactions_debtor_deleted_date
    on public.debt_transactions (debtor_id, is_deleted, transaction_date desc);

create index idx_creditors_user_deleted_status
    on public.creditors (user_id, is_deleted, status);

create index idx_creditor_transactions_creditor_deleted_date
    on public.creditor_transactions (creditor_id, is_deleted, transaction_date desc);

-- -----------------------------------------------------------------------------
-- 10. Realtime — підписка з клієнта (supabase-kt realtime-kt)
-- 10. Realtime — client-side subscription (supabase-kt realtime-kt)
-- -----------------------------------------------------------------------------
alter publication supabase_realtime add table
    public.debtors,
    public.debt_transactions,
    public.creditors,
    public.creditor_transactions;

-- -----------------------------------------------------------------------------
-- 11. delete_all_user_data — повне видалення даних користувача
-- 11. delete_all_user_data — hard-delete all of a user's data
-- Викликається з SettingsScreen "Видалити всі дані" (подвійне підтвердження в UI).
-- Called from SettingsScreen "Delete all data" (double-confirmation in the UI).
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

revoke all on function public.delete_all_user_data(uuid) from public;
grant execute on function public.delete_all_user_data(uuid) to authenticated;

-- =============================================================================
-- Кінець міграції 0001. / End of migration 0001.
-- =============================================================================
