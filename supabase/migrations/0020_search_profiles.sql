-- =============================================================================
-- DebtTracker — пошук зареєстрованих користувачів за імʼям/телефоном для
-- автозаповнення форми боржника/кредитора (поряд із find_profile_by_email).
-- DebtTracker — registered-user search by name/phone for the debtor/creditor
-- form autofill (alongside find_profile_by_email).
--
-- Як і find_profile_by_email, повертає лише безпечні публічні поля (імʼя, аватар),
-- без id/email/phone. Щоб не бути довідником усіх користувачів:
--   * телефон — лише точний збіг (>= 9 цифр);
--   * імʼя — щонайменше 2 слова по >= 2 символи, кожне — префікс слова імені;
--   * максимум 5 рядків, сам викликач виключений.
-- Like find_profile_by_email it returns only safe public fields (name, avatar).
-- To avoid acting as a user directory: phone is exact-match only (>= 9 digits);
-- name needs >= 2 words of >= 2 chars, each a prefix of a word in the display
-- name; at most 5 rows; the caller is excluded.
-- =============================================================================
create or replace function public.search_profiles(p_name text default null, p_phone text default null)
returns table (
    display_name text,
    avatar_url text
)
language plpgsql
stable
security definer
set search_path = public
as $$
declare
    v_tokens text[];
begin
    if auth.uid() is null then
        return;
    end if;

    if p_phone is not null and length(regexp_replace(p_phone, '\D', '', 'g')) >= 9 then
        return query
        select p.display_name, p.avatar_url
          from public.profiles p
         where p.phone = p_phone
           and p.id <> auth.uid()
         limit 5;
        if found then
            return;
        end if;
    end if;

    select array_agg(lower(t)) into v_tokens
      from regexp_split_to_table(trim(coalesce(p_name, '')), '\s+') as t
     where length(t) >= 2;

    if coalesce(array_length(v_tokens, 1), 0) < 2 then
        return;
    end if;

    return query
    select p.display_name, p.avatar_url
      from public.profiles p
     where p.id <> auth.uid()
       and p.display_name is not null
       and not exists (
           select 1
             from unnest(v_tokens) as tok
            where not exists (
                select 1
                  from regexp_split_to_table(lower(p.display_name), '\s+') as w
                 where starts_with(w, tok)
            )
       )
     limit 5;
end;
$$;

revoke all on function public.search_profiles(text, text) from public, anon;
grant execute on function public.search_profiles(text, text) to authenticated;

-- =============================================================================
-- Кінець міграції 0020. / End of migration 0020.
-- =============================================================================
