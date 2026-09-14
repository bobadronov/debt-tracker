package org.bigblackowl.debttracker.domain.model

/**
 * Debt direction on the combined "Add entry" screen: [DEBTOR] — someone owes me
 * ("Owed to me"), [CREDITOR] — I owe someone ("My debts"). Determines which domain
 * entity ([Debtor]/[Creditor]) and transaction table the save goes into.
 */
enum class DebtDirection { DEBTOR, CREDITOR }
