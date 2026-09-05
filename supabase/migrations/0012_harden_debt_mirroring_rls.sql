-- =============================================================================
-- DebtTracker — усунення двох дір у RLS/мірруванні боргів (security review):
--
-- B1. debt_transactions_owner_all / creditor_transactions_owner_all перевіряли
--     лише user_id = auth.uid(), але НЕ те, що debtor_id/creditor_id належить
--     цьому ж юзеру. Знаючи чужий debtor_id/creditor_id (UUID), можна було
--     вставити транзакцію зі своїм user_id, але чужим debtor_id/creditor_id —
--     RLS пропускала це, а тригер recalc_*_status() чесно перераховував
--     БАЛАНС ЧУЖОГО боржника/кредитора за довільними підкинутими сумами.
--
-- B2. debtors.linked_user_id/mirror_creditor_id і creditors.linked_user_id/
--     mirror_debtor_id — звичайні колонки під загальним "for all" policy
--     (debtors_owner_all/creditors_owner_all). Власник рядка міг PATCH-нути
--     ці поля напряму (в обхід link_debtor_to_registered_user/
--     link_creditor_to_registered_user), вказавши ДОВІЛЬНИЙ user_id/чужий
--     creditor_id/debtor_id як ціль дзеркалювання — без жодної згоди чи
--     перевірки phone/email збігу. Кожна наступна транзакція власника після
--     цього автоматично пропагувалась у чужий акаунт через
--     propagate_debt_transaction/propagate_creditor_transaction.
--
-- B1. debt_transactions_owner_all / creditor_transactions_owner_all only
--     checked user_id = auth.uid(), never that debtor_id/creditor_id actually
--     belongs to that same user. Knowing a stranger's debtor_id/creditor_id
--     (a UUID) was enough to insert a transaction with your own user_id but
--     their debtor_id/creditor_id — RLS let it through, and
--     recalc_*_status() dutifully recalculated the STRANGER'S balance from
--     whatever amount was smuggled in.
--
-- B2. debtors.linked_user_id/mirror_creditor_id and creditors.linked_user_id/
--     mirror_debtor_id are plain columns covered by the blanket "for all"
--     policy (debtors_owner_all/creditors_owner_all). A row's owner could
--     PATCH these fields directly (bypassing link_debtor_to_registered_user/
--     link_creditor_to_registered_user entirely), pointing mirroring at ANY
--     user_id / someone else's creditor_id/debtor_id — no consent, no
--     phone/email match check. Every subsequent transaction they entered
--     would then auto-propagate into the stranger's account via
--     propagate_debt_transaction/propagate_creditor_transaction.
--
-- Обидва фікси навмисно НЕ чіпають SECURITY DEFINER-функції з 0007/0009 —
-- ті виконуються від імені власника функції (не 'authenticated'/'anon'),
-- тож і нова WITH CHECK-умова, і новий тригер-guard їх пропускають без змін.
-- Both fixes are deliberately transparent to the SECURITY DEFINER functions
-- from 0007/0009 — those run as the function owner (never 'authenticated'/
-- 'anon'), so both the new WITH CHECK clause and the new guard trigger let
-- them through unchanged.
-- =============================================================================

begin;

-- -----------------------------------------------------------------------------
-- 1. B1 — WITH CHECK тепер вимагає, щоб debtor_id/creditor_id належав тому ж
--    user_id, що й сама транзакція.
-- 1. B1 — WITH CHECK now requires debtor_id/creditor_id to belong to the same
--    user_id as the transaction row itself.
-- -----------------------------------------------------------------------------
drop policy "debt_transactions_owner_all" on public.debt_transactions;

create policy "debt_transactions_owner_all" on public.debt_transactions
    for all
    using (user_id = auth.uid())
    with check (
        user_id = auth.uid()
        and exists (
            select 1 from public.debtors d
             where d.id = debt_transactions.debtor_id
               and d.user_id = auth.uid()
        )
    );

drop policy "creditor_transactions_owner_all" on public.creditor_transactions;

create policy "creditor_transactions_owner_all" on public.creditor_transactions
    for all
    using (user_id = auth.uid())
    with check (
        user_id = auth.uid()
        and exists (
            select 1 from public.creditors c
             where c.id = creditor_transactions.creditor_id
               and c.user_id = auth.uid()
        )
    );

-- -----------------------------------------------------------------------------
-- 2. B2 — guard-тригер: linked_user_id/mirror_creditor_id (debtors) і
--    linked_user_id/mirror_debtor_id (creditors) може змінювати лише
--    SECURITY DEFINER-контекст (link_*_to_registered_user). Прямий
--    INSERT/UPDATE від 'authenticated'/'anon' (тобто напряму від клієнта
--    через PostgREST) на ці поля — заборонено.
--
--    current_user відрізняє контексти коректно: PostgREST виконує запит під
--    роллю 'authenticated'/'anon' (SET ROLE за JWT), а SECURITY DEFINER
--    функція під час виконання змінює current_user на власника функції —
--    тобто НЕ 'authenticated'/'anon', і тригер її пропускає без змін.
--
-- 2. B2 — guard trigger: linked_user_id/mirror_creditor_id (debtors) and
--    linked_user_id/mirror_debtor_id (creditors) may only be changed from a
--    SECURITY DEFINER context (link_*_to_registered_user). A direct
--    INSERT/UPDATE from 'authenticated'/'anon' (i.e. straight from the
--    client via PostgREST) touching these fields is rejected.
--
--    current_user correctly tells the two contexts apart: PostgREST runs the
--    request as role 'authenticated'/'anon' (SET ROLE per the JWT), while a
--    SECURITY DEFINER function changes current_user to the function's owner
--    for the duration of the call — never 'authenticated'/'anon' — so the
--    trigger lets it through unchanged.
-- -----------------------------------------------------------------------------
create function public.guard_mirror_link_columns()
returns trigger
language plpgsql
set search_path = public
as $$
begin
    -- Довірений контекст (SECURITY DEFINER RPC, service_role, міграції) — пропускаємо.
    -- Trusted context (SECURITY DEFINER RPC, service_role, migrations) — pass through.
    if current_user not in ('authenticated', 'anon') then
        return new;
    end if;

    if TG_TABLE_NAME = 'debtors' then
        if TG_OP = 'INSERT' then
            -- Клієнт ніколи не створює вже прив'язаного боржника напряму.
            -- A client never creates an already-linked debtor directly.
            new.linked_user_id := null;
            new.mirror_creditor_id := null;
        elsif new.linked_user_id is distinct from old.linked_user_id
           or new.mirror_creditor_id is distinct from old.mirror_creditor_id then
            raise exception 'debtors.linked_user_id / mirror_creditor_id can only be set by link_debtor_to_registered_user()';
        end if;
    elsif TG_TABLE_NAME = 'creditors' then
        if TG_OP = 'INSERT' then
            new.linked_user_id := null;
            new.mirror_debtor_id := null;
        elsif new.linked_user_id is distinct from old.linked_user_id
           or new.mirror_debtor_id is distinct from old.mirror_debtor_id then
            raise exception 'creditors.linked_user_id / mirror_debtor_id can only be set by link_creditor_to_registered_user()';
        end if;
    end if;

    return new;
end;
$$;

comment on function public.guard_mirror_link_columns() is
    'Блокує пряму зміну клієнтом linked_user_id/mirror_*_id — лише через link_*_to_registered_user(). '
    'Blocks a client from directly changing linked_user_id/mirror_*_id — only via link_*_to_registered_user().';

create trigger trg_guard_debtors_mirror_columns
    before insert or update on public.debtors
    for each row execute function public.guard_mirror_link_columns();

create trigger trg_guard_creditors_mirror_columns
    before insert or update on public.creditors
    for each row execute function public.guard_mirror_link_columns();

revoke all on function public.guard_mirror_link_columns() from public, anon, authenticated;

commit;

-- =============================================================================
-- Кінець міграції 0012. / End of migration 0012.
--
-- Ручна перевірка існуючих даних на вже експлуатовані наслідки B1/B2
-- (запустити ОКРЕМО, вручну — не частина цієї міграції):
-- Manual check for data already corrupted via B1/B2 before this fix
-- (run SEPARATELY, by hand — not part of this migration):
--
--   -- B1: транзакції, чий debtor_id/creditor_id не належить їхньому user_id
--   -- B1: transactions whose debtor_id/creditor_id doesn't belong to their user_id
--   select dt.* from public.debt_transactions dt
--     join public.debtors d on d.id = dt.debtor_id
--    where d.user_id <> dt.user_id;
--
--   select ct.* from public.creditor_transactions ct
--     join public.creditors c on c.id = ct.creditor_id
--    where c.user_id <> ct.user_id;
--
--   -- B2: mirror-посилання без відповідного консент-запису у notifications
--   -- B2: mirror links with no matching consent row in notifications
--   select * from public.debtors
--    where linked_user_id is not null
--      and not exists (
--          select 1 from public.notifications n
--           where n.type = 'DEBTOR_LINKED' and n.related_creditor_id = debtors.mirror_creditor_id
--      );
-- =============================================================================
