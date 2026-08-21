package com.dwt.ledger.domain.logic

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind

data class MonthlySummary(
    val income: Money,
    val expense: Money,
) {
    val balance: Money get() = income - expense

    companion object {
        val EMPTY = MonthlySummary(Money.ZERO, Money.ZERO)
    }
}

/** 纯函数：任意一组流水 → 收入/支出合计。不依赖 Android，直接 JUnit 测。 */
fun List<Transaction>.summarize(): MonthlySummary {
    var income = 0L
    var expense = 0L
    for (t in this) {
        when (t.kind) {
            TransactionKind.INCOME -> income += t.amount.cents
            TransactionKind.EXPENSE -> expense += t.amount.cents
        }
    }
    return MonthlySummary(Money(income), Money(expense))
}
