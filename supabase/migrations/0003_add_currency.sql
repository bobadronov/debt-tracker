-- =============================================================================
-- DebtTracker — мультивалютність: грн (UAH) / долари (USD) / злоті (PLN) / євро (EUR)
-- DebtTracker — multi-currency support: UAH / USD / PLN / EUR
--
-- Валюта фіксується на рівні debtors/creditors (вся заборгованість — в одній
-- валюті, без конвертації курсів), а не на кожній транзакції.
-- Currency lives on debtors/creditors (a whole debt is in one currency, no FX
-- conversion), not on individual transactions.
-- =============================================================================

alter table public.debtors
    add column currency text not null default 'UAH' check (currency in ('UAH', 'USD', 'PLN', 'EUR'));

alter table public.creditors
    add column currency text not null default 'UAH' check (currency in ('UAH', 'USD', 'PLN', 'EUR'));

-- =============================================================================
-- Кінець міграції 0003. / End of migration 0003.
-- =============================================================================
