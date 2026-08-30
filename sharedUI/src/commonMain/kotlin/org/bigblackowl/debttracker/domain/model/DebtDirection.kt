package org.bigblackowl.debttracker.domain.model

/**
 * Напрямок боргу на об'єднаному екрані «Додати запис»: [DEBTOR] — людина винна мені
 * («Мені винні»), [CREDITOR] — я винен людині («Мої борги»). Обирає, у яку доменну
 * сутність ([Debtor]/[Creditor]) і таблицю транзакцій піде збереження.
 */
enum class DebtDirection { DEBTOR, CREDITOR }
