-- =============================================================================
-- DebtTracker — гігієна БД за порадами Supabase advisors (без зміни поведінки).
--
--   1. auth_rls_initplan — обгортаємо auth.uid() у (select auth.uid()), щоб
--      планувальник обчислював його один раз на запит, а не на кожен рядок.
--   2. Неіндексовані зовнішні ключі — додаємо покривні індекси.
--   3. multiple_permissive_policies — зливаємо по дві SELECT-політики на
--      pending_link_requests та transaction_corrections в одну (OR).
--
-- Семантика доступу не змінюється: ті самі рядки видно тим самим користувачам.
--
-- DebtTracker — database hygiene per Supabase advisors (no behaviour change).
--   1. auth_rls_initplan — wrap auth.uid() in (select auth.uid()) so the planner
--      evaluates it once per query instead of once per row.
--   2. Unindexed foreign keys — add covering indexes.
--   3. multiple_permissive_policies — collapse the two SELECT policies on
--      pending_link_requests and transaction_corrections into one (OR).
-- Access semantics are unchanged: the same rows are visible to the same users.
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 1. RLS initplan — переставляємо auth.uid() → (select auth.uid()).
-- 1. RLS initplan — recreate policies with (select auth.uid()).
-- -----------------------------------------------------------------------------

-- profiles
drop policy "profiles_owner_all" on public.profiles;
create policy "profiles_owner_all" on public.profiles
    for all
    using (id = (select auth.uid()))
    with check (id = (select auth.uid()));

-- debtors
drop policy "debtors_owner_all" on public.debtors;
create policy "debtors_owner_all" on public.debtors
    for all
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- creditors
drop policy "creditors_owner_all" on public.creditors;
create policy "creditors_owner_all" on public.creditors
    for all
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- user_sessions
drop policy "user_sessions_owner_all" on public.user_sessions;
create policy "user_sessions_owner_all" on public.user_sessions
    for all
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

-- notifications (select / update / delete)
drop policy "notifications_owner_select" on public.notifications;
create policy "notifications_owner_select" on public.notifications
    for select
    using (user_id = (select auth.uid()));

drop policy "notifications_owner_update" on public.notifications;
create policy "notifications_owner_update" on public.notifications
    for update
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy "notifications_owner_delete" on public.notifications;
create policy "notifications_owner_delete" on public.notifications
    for delete
    using (user_id = (select auth.uid()));

-- debt_transactions
drop policy "debt_transactions_owner_all" on public.debt_transactions;
create policy "debt_transactions_owner_all" on public.debt_transactions
    for all
    using (user_id = (select auth.uid()))
    with check (
        user_id = (select auth.uid())
        and exists (
            select 1 from public.debtors d
             where d.id = debt_transactions.debtor_id
               and d.user_id = (select auth.uid())
        )
    );

-- creditor_transactions
drop policy "creditor_transactions_owner_all" on public.creditor_transactions;
create policy "creditor_transactions_owner_all" on public.creditor_transactions
    for all
    using (user_id = (select auth.uid()))
    with check (
        user_id = (select auth.uid())
        and exists (
            select 1 from public.creditors c
             where c.id = creditor_transactions.creditor_id
               and c.user_id = (select auth.uid())
        )
    );

-- -----------------------------------------------------------------------------
-- 3. Злиття дублюючих SELECT-політик (одразу з (select auth.uid())).
-- 3. Merge duplicate SELECT policies (already using (select auth.uid())).
-- -----------------------------------------------------------------------------

-- pending_link_requests: requester OR target
drop policy "pending_link_requests_requester_select" on public.pending_link_requests;
drop policy "pending_link_requests_target_select"    on public.pending_link_requests;
create policy "pending_link_requests_party_select" on public.pending_link_requests
    for select
    using ((select auth.uid()) in (requester_user_id, target_user_id));

-- transaction_corrections: proposer OR target
drop policy "transaction_corrections_proposer_select" on public.transaction_corrections;
drop policy "transaction_corrections_target_select"   on public.transaction_corrections;
create policy "transaction_corrections_party_select" on public.transaction_corrections
    for select
    using ((select auth.uid()) in (proposer_user_id, target_user_id));

-- -----------------------------------------------------------------------------
-- 2. Покривні індекси для зовнішніх ключів без індексу.
--    Nullable-колонки — часткові індекси (як решта FK-індексів у схемі).
-- 2. Covering indexes for unindexed foreign keys.
--    Nullable columns get partial indexes (matching the rest of the schema).
-- -----------------------------------------------------------------------------
create index idx_creditor_transactions_user_id
    on public.creditor_transactions (user_id);

create index idx_debt_transactions_user_id
    on public.debt_transactions (user_id);

create index idx_pending_link_requests_requester
    on public.pending_link_requests (requester_user_id);

create index idx_transaction_corrections_proposer
    on public.transaction_corrections (proposer_user_id);

create index idx_notifications_actor_user_id
    on public.notifications (actor_user_id)
    where actor_user_id is not null;

create index idx_notifications_related_link_request_id
    on public.notifications (related_link_request_id)
    where related_link_request_id is not null;

create index idx_notifications_related_correction_id
    on public.notifications (related_correction_id)
    where related_correction_id is not null;

commit;

-- =============================================================================
-- Кінець міграції 0015. / End of migration 0015.
-- =============================================================================
