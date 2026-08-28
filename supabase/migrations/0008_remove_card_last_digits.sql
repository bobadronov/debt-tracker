-- =============================================================================
-- DebtTracker — видалення фічі "останні цифри картки": поле більше не
-- збирається у формах і ніде не відображається, колонки прибрано з обох
-- таблиць транзакцій.
-- DebtTracker — removes the "card last digits" feature: the field is no
-- longer collected in any form or shown anywhere, columns dropped from both
-- transaction tables.
-- =============================================================================

alter table public.debt_transactions drop column if exists card_last_digits;
alter table public.creditor_transactions drop column if exists card_last_digits;

-- =============================================================================
-- Кінець міграції 0008. / End of migration 0008.
-- =============================================================================
