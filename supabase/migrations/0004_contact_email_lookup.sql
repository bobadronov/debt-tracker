-- =============================================================================
-- DebtTracker — пошук зареєстрованого користувача за email при додаванні
-- боржника/кредитора, для автозаповнення (ім'я + фото) в AddEdit-формі.
-- DebtTracker — look up a registered user by email when adding a debtor/
-- creditor, to offer autofill (name + photo) in the AddEdit form.
--
-- Примітка: `profiles.avatar_url` уже існує на живій БД (додано вручну поза
-- версійованими міграціями) — тут узгоджуємо файли міграцій із фактичною
-- схемою через `add column if not exists`, ідемпотентно для обох станів.
-- Note: `profiles.avatar_url` already exists on the live DB (added by hand
-- outside versioned migrations) — reconciled here via `add column if not
-- exists`, safe to run whether or not the column is already there.
-- =============================================================================

alter table public.profiles
    add column if not exists avatar_url text;

alter table public.profiles
    add column if not exists email text;

alter table public.debtors
    add column email text;

alter table public.creditors
    add column email text;

-- -----------------------------------------------------------------------------
-- profiles.email заповнюється з auth.users.email при реєстрації, плюс
-- одноразовий backfill для вже наявних акаунтів.
-- profiles.email is populated from auth.users.email at signup, plus a
-- one-time backfill for accounts that already existed.
-- -----------------------------------------------------------------------------
create or replace function public.handle_new_user()
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

update public.profiles p
   set email = u.email
  from auth.users u
 where p.id = u.id
   and p.email is distinct from u.email;

create unique index if not exists idx_profiles_email_lower
    on public.profiles (lower(email))
    where email is not null;

-- -----------------------------------------------------------------------------
-- find_profile_by_email — єдиний дозволений спосіб для клієнта дізнатись про
-- чужий профіль: RLS на profiles суворо власницький (id = auth.uid()), тож
-- пряме читання чужого рядка неможливе. SECURITY DEFINER-функція обходить це
-- навмисно, повертаючи лише безпечні публічні поля (без email/id) для
-- ОДНОГО збігу за email.
-- find_profile_by_email — the only sanctioned way for a client to learn
-- anything about someone else's profile: profiles RLS is strictly owner-only
-- (id = auth.uid()), so a direct read of another user's row is impossible.
-- This SECURITY DEFINER function deliberately bypasses that, returning only
-- safe public fields (no email/id) for a SINGLE email match.
-- -----------------------------------------------------------------------------
create or replace function public.find_profile_by_email(p_email text)
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

-- =============================================================================
-- Кінець міграції 0004. / End of migration 0004.
-- =============================================================================
