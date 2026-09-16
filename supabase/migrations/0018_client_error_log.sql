-- =============================================================================
-- DebtTracker — client_error_log: власний ерор-лог клієнта.
-- DebtTracker — client_error_log: the app's own client-side error log.
--
-- Пишеться з ClientErrorReporter (Napier-хук на WARNING/ERROR/ASSERT +, де можливо,
-- глобальний перехоплювач необроблених винятків) для БУДЬ-ЯКОГО користувача, залогіненого
-- чи ні — саме тому єдиний шлях запису це report_client_error(), а не прямий insert:
--   * RLS на таблиці не має жодної policy → anon/authenticated не можуть читати, писати,
--     міняти чи видаляти рядки напряму взагалі (safe for the user: ніхто, включно з
--     автором репорту, не може прочитати чужі чи навіть власні записи через API).
--   * report_client_error() — security definer, тому обходить RLS і сама відповідає за
--     валідацію: обрізає довжину полів і рейт-лімітить по device_id (не по user_id, щоб
--     працювало для незалогінених) — захист від зациклених/спамлячих клієнтів (safe for
--     the DB: розмір запису й швидкість запису обмежені на сервері, а не довірою клієнту).
--   * user_id береться з auth.uid() всередині функції, а не з параметра — клієнт не може
--     підписати помилку чужим user_id.
--   * pg_cron прибирає записи старші 30 днів — таблиця не росте необмежено.
--
-- Written from ClientErrorReporter (a Napier hook on WARNING/ERROR/ASSERT, plus a
-- best-effort uncaught-exception handler where available) for ANY user, signed in or not —
-- which is exactly why the only write path is report_client_error(), never a direct insert:
--   * RLS has zero policies on the table → anon/authenticated get no direct read/write/
--     update/delete at all (safe for the user: nobody, including the reporter themselves,
--     can read anyone's rows back through the API).
--   * report_client_error() is security definer, so it bypasses RLS and owns validation
--     itself: truncates field lengths and rate-limits per device_id (not user_id, so it
--     still works signed out) — guards against a crash-looping/spamming client (safe for
--     the DB: row size and write rate are bounded server-side, not left to client trust).
--   * user_id comes from auth.uid() inside the function, never a client-supplied
--     parameter — a caller can't attribute an error to someone else's account.
--   * pg_cron prunes rows older than 30 days so the table doesn't grow unbounded.
-- =============================================================================

create table public.client_error_log (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid references auth.users (id) on delete set null,  -- null = anonymous/local-only reporter
    device_id    uuid not null,                                        -- AppSettings.deviceSessionId, for correlation + rate limiting
    platform     text not null check (platform in ('ANDROID', 'IOS', 'DESKTOP', 'WEB')),
    app_version  text,
    severity     text not null check (severity in ('WARN', 'ERROR')),
    tag          text,
    message      text not null,
    stack_trace  text,
    created_at   timestamptz not null default now()
);

comment on table public.client_error_log is
    'Власний ерор-лог клієнта; пишеться лише через report_client_error() (RLS без policy блокує прямий доступ). Own client-side error log; write-only via report_client_error() (RLS has no policy, blocking direct access).';

-- Обидва запити, що торкаються цієї таблиці (рейт-ліміт всередині report_client_error()
-- і денний pg_cron cleanup), фільтрують саме по цих колонках.
-- Both queries that touch this table (the rate-limit check inside report_client_error()
-- and the daily pg_cron cleanup) filter on exactly these columns.
create index client_error_log_device_id_created_at_idx on public.client_error_log (device_id, created_at desc);
create index client_error_log_created_at_idx on public.client_error_log (created_at);

alter table public.client_error_log enable row level security;
-- Свідомо без жодної policy: anon/authenticated не мають прямого доступу до цієї таблиці
-- взагалі, в жодному напрямку. Deliberately no policy at all: anon/authenticated get no
-- direct access to this table whatsoever, in either direction.

-- -----------------------------------------------------------------------------
-- report_client_error — єдиний спосіб писати в client_error_log.
-- report_client_error — the only way to write to client_error_log.
-- -----------------------------------------------------------------------------
create function public.report_client_error(
    p_device_id   uuid,
    p_platform    text,
    p_app_version text,
    p_severity    text,
    p_tag         text,
    p_message     text,
    p_stack_trace text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_recent_count int;
begin
    if p_message is null or length(trim(p_message)) = 0 then
        raise exception 'message is required';
    end if;
    if p_platform not in ('ANDROID', 'IOS', 'DESKTOP', 'WEB') then
        raise exception 'invalid platform';
    end if;

    -- Рейт-ліміт по device_id (не user_id — має працювати й для незалогінених):
    -- максимум 50 записів на пристрій за годину. Зациклений клієнт мовчки ігнорується,
    -- а не отримує помилку — це best-effort телеметрія, вона не повинна щось ламати
    -- користувачу далі по стеку.
    -- Rate limit by device_id (not user_id — must work for signed-out users too):
    -- at most 50 rows per device per hour. A looping client is silently dropped, not
    -- raised — this is best-effort telemetry and must never surface to the caller.
    select count(*) into v_recent_count
    from public.client_error_log
    where device_id = p_device_id
      and created_at > now() - interval '1 hour';

    if v_recent_count >= 50 then
        return;
    end if;

    insert into public.client_error_log (
        user_id, device_id, platform, app_version, severity, tag, message, stack_trace
    ) values (
        auth.uid(),
        p_device_id,
        p_platform,
        left(p_app_version, 50),
        case when p_severity = 'WARN' then 'WARN' else 'ERROR' end,
        left(p_tag, 200),
        left(p_message, 4000),
        left(p_stack_trace, 20000)
    );
end;
$$;

revoke all on function public.report_client_error(uuid, text, text, text, text, text, text) from public;
grant execute on function public.report_client_error(uuid, text, text, text, text, text, text) to anon, authenticated;

-- -----------------------------------------------------------------------------
-- pg_cron — щоденне прибирання записів старших 30 днів.
-- pg_cron — daily cleanup of rows older than 30 days.
-- -----------------------------------------------------------------------------
create extension if not exists pg_cron with schema pg_catalog;

select cron.schedule(
    'client_error_log_cleanup',
    '17 3 * * *',
    $$ delete from public.client_error_log where created_at < now() - interval '30 days' $$
);

-- =============================================================================
-- Кінець міграції 0018. / End of migration 0018.
-- =============================================================================
