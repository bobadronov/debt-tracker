-- =============================================================================
-- DebtTracker — user_sessions: активні пристрої акаунта + віддалений вихід.
-- DebtTracker — user_sessions: account's active devices + remote logout.
--
-- Кожен пристрій реєструє/оновлює свій рядок при вході (id = локально
-- згенерований UUID, зберігається в AppSettings.deviceSessionId). "Віддалений
-- вихід" — це soft-revoke (revoked_at): клієнт-жертва підписаний на зміни
-- власного рядка через Realtime (SupabaseSessionRepository.revokedElsewhere)
-- і сам себе розлогінює, щойно бачить revoked_at != null. Немає окремого
-- сервера/Edge Function — Supabase Auth SDK не вміє відкликати чужу сесію
-- напряму, тож справжнє миттєве анулювання JWT тут не робиться.
--
-- Each device registers/updates its own row at sign-in (id = a locally
-- generated UUID, persisted in AppSettings.deviceSessionId). "Remote logout"
-- is a soft-revoke (revoked_at): the victim client subscribes to changes on
-- its own row via Realtime (SupabaseSessionRepository.revokedElsewhere) and
-- signs itself out as soon as it observes revoked_at != null. There's no
-- separate server/Edge Function here — the Supabase Auth SDK can't revoke
-- another session's JWT directly, so this does not instantly invalidate the
-- token itself, only kicks the app on its next realtime tick/reconnect.
-- =============================================================================

create table public.user_sessions (
    id            uuid primary key,                        -- клієнтський device id (AppSettings.deviceSessionId) / client device id (AppSettings.deviceSessionId)
    user_id       uuid not null references auth.users (id) on delete cascade,
    device_name   text not null,
    platform      text not null check (platform in ('ANDROID', 'IOS', 'DESKTOP', 'WEB')),
    app_version   text,
    created_at    timestamptz not null default now(),
    last_seen_at  timestamptz not null default now(),
    revoked_at    timestamptz
);

comment on table public.user_sessions is 'Активні пристрої акаунта для екрана "Session management" (спек Settings). Account''s active devices for the "Session management" screen.';

-- Every read here filters by user_id — the RLS check itself, observeSessions()'s query, and
-- registerOrTouchSession()'s upsert all do — so this is the one index the table needs.
create index user_sessions_user_id_idx on public.user_sessions (user_id);

alter table public.user_sessions enable row level security;

create policy "user_sessions_owner_all" on public.user_sessions
    for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- Дозволяє self-revoke власного пристрою бути видимим миттєво іншим вкладкам/пристроям.
-- Lets a self-revoke of one's own device be seen instantly by other tabs/devices.
alter publication supabase_realtime add table public.user_sessions;

-- =============================================================================
-- Кінець міграції 0006. / End of migration 0006.
-- =============================================================================
