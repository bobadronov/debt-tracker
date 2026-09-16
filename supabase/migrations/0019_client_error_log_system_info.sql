-- =============================================================================
-- DebtTracker — client_error_log: додано system_info (ОС/пристрій репортера).
-- DebtTracker — client_error_log: adds system_info (reporter's OS/device).
--
-- Довільний текст (не jsonb): формат складає кожна платформа сама (див. SystemInfo.<platform>.kt),
-- тому й тут просто обрізаний по довжині рядок — той самий підхід, що вже є для tag/message/
-- stack_trace. report_client_error() перестворюється (не create or replace) — новий параметр
-- змінює сигнатуру функції, тож стару версію спершу дропаємо разом з її grants.
--
-- Free-form text (not jsonb): each platform formats its own string (see
-- SystemInfo.<platform>.kt), so this stays a plain length-capped column — same approach already
-- used for tag/message/stack_trace. report_client_error() is dropped and recreated rather than
-- `create or replace` — the new parameter changes the function's signature, so the old version
-- (and its grants) has to go first.
-- =============================================================================

alter table public.client_error_log add column system_info text;

drop function if exists public.report_client_error(uuid, text, text, text, text, text, text);

create function public.report_client_error(
    p_device_id    uuid,
    p_platform     text,
    p_app_version  text,
    p_severity     text,
    p_tag          text,
    p_message      text,
    p_stack_trace  text,
    p_system_info  text default null
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

    select count(*) into v_recent_count
    from public.client_error_log
    where device_id = p_device_id
      and created_at > now() - interval '1 hour';

    if v_recent_count >= 50 then
        return;
    end if;

    insert into public.client_error_log (
        user_id, device_id, platform, app_version, severity, tag, message, stack_trace, system_info
    ) values (
        auth.uid(),
        p_device_id,
        p_platform,
        left(p_app_version, 50),
        case when p_severity = 'WARN' then 'WARN' else 'ERROR' end,
        left(p_tag, 200),
        left(p_message, 4000),
        left(p_stack_trace, 20000),
        left(p_system_info, 500)
    );
end;
$$;

revoke all on function public.report_client_error(uuid, text, text, text, text, text, text, text) from public;
grant execute on function public.report_client_error(uuid, text, text, text, text, text, text, text) to anon, authenticated;

-- =============================================================================
-- Кінець міграції 0019. / End of migration 0019.
-- =============================================================================
