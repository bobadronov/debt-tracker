-- =============================================================================
-- DebtTracker — телефон акаунта на екрані "Мій акаунт" (Settings), окремо від
-- телефонів контактів (debtors/creditors), які вже мають свою колонку.
-- DebtTracker — account phone number for the "My account" screen, distinct
-- from debtor/creditor contact phones, which already have their own column.
-- =============================================================================

alter table public.profiles
    add column if not exists phone text;

-- =============================================================================
-- Кінець міграції 0005. / End of migration 0005.
-- =============================================================================
