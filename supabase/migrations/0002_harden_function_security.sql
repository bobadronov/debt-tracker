-- =============================================================================
-- DebtTracker — Фаза 6: усунення security-попереджень Supabase advisor
-- DebtTracker — Phase 6: fix Supabase advisor security warnings
--
-- Після застосування 0001_init.sql advisor (get_advisors, type=security)
-- показав: 1) touch_updated_at мав mutable search_path; 2) тригер-функції
-- (handle_new_user/recalc_*_status/touch_updated_at) були доступні як
-- публічні RPC-ендпоінти через PostgREST, хоча призначені лише для тригерів.
--
-- After applying 0001_init.sql, the advisor (get_advisors, type=security)
-- flagged: 1) touch_updated_at had a mutable search_path; 2) trigger-only
-- functions (handle_new_user/recalc_*_status/touch_updated_at) were reachable
-- as public RPC endpoints via PostgREST, even though they're trigger-only.
--
-- Тригери спрацьовують незалежно від прав EXECUTE на функцію-тригер, тому
-- відкликання EXECUTE в anon/authenticated/public не ламає тригери.
-- Triggers fire regardless of EXECUTE grants on the function, so revoking
-- EXECUTE from anon/authenticated/public does not break them.
-- =============================================================================

alter function public.touch_updated_at() set search_path = public;

revoke all on function public.touch_updated_at() from public, anon, authenticated;
revoke all on function public.handle_new_user() from public, anon, authenticated;
revoke all on function public.recalc_debtor_status() from public, anon, authenticated;
revoke all on function public.recalc_creditor_status() from public, anon, authenticated;

-- delete_all_user_data має лишатись доступним authenticated (SettingsScreen
-- "Видалити всі дані"), але не anon — прописуємо явно, а не покладаємось
-- лише на revoke-from-public.
-- delete_all_user_data must stay callable by authenticated (Settings "Delete
-- all data"), but not anon — spelled out explicitly rather than relying on
-- revoke-from-public alone.
revoke all on function public.delete_all_user_data(uuid) from public, anon, authenticated;
grant execute on function public.delete_all_user_data(uuid) to authenticated;

-- =============================================================================
-- Залишкове попередження advisor'а — очікуване й навмисне:
-- Remaining advisor warning — expected and intentional:
-- "delete_all_user_data can be executed by authenticated" — саме так і
-- задумано (SettingsScreen викликає цю RPC для авторизованого користувача).
-- "delete_all_user_data can be executed by authenticated" — this is exactly
-- the intended design (SettingsScreen calls this RPC for the signed-in user).
-- =============================================================================
