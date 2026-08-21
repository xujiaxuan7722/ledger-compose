package com.dwt.ledger.domain

import com.dwt.ledger.domain.logic.computeBudgetProgress
import com.dwt.ledger.domain.model.Budget
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class BudgetProgressTest {
    private val ym = YearMonth.of(2026, 8)
    private fun t(kind: TransactionKind, cents: Long, cat: String) = Transaction("$cat$cents", kind, Money(cents), cat, "a", Instant.EPOCH)

    @Test fun `total budget counts all expenses, category budget only its category, income ignored`() {
        val budgets = listOf(Budget("total", null, ym, Money(10_000)), Budget("food", "cat_food", ym, Money(3_000)))
        val tx = listOf(t(TransactionKind.EXPENSE, 2_000, "cat_food"), t(TransactionKind.EXPENSE, 1_500, "cat_bus"), t(TransactionKind.INCOME, 99_999, "cat_salary"))
        val result = computeBudgetProgress(budgets, tx)
        assertThat(result.map { it.budget.id }).containsExactly("total", "food").inOrder() // 总预算永远在前
        assertThat(result[0].spent).isEqualTo(Money(3_500)); assertThat(result[0].remaining).isEqualTo(Money(6_500)); assertThat(result[0].isOver).isFalse()
        assertThat(result[1].spent).isEqualTo(Money(2_000)); assertThat(result[1].fraction).isWithin(1e-9).of(2.0 / 3)
    }

    @Test fun `over budget is flagged and fraction exceeds one`() {
        val p = computeBudgetProgress(listOf(Budget("food", "cat_food", ym, Money(1_000))), listOf(t(TransactionKind.EXPENSE, 1_250, "cat_food"))).single()
        assertThat(p.isOver).isTrue(); assertThat(p.remaining).isEqualTo(Money(-250)); assertThat(p.fraction).isWithin(1e-9).of(1.25)
    }

    @Test fun `category budgets sorted by usage desc after total`() {
        val budgets = listOf(Budget("a", "cat_a", ym, Money(100)), Budget("b", "cat_b", ym, Money(100)), Budget("t", null, ym, Money(1_000)))
        val tx = listOf(t(TransactionKind.EXPENSE, 10, "cat_a"), t(TransactionKind.EXPENSE, 90, "cat_b"))
        assertThat(computeBudgetProgress(budgets, tx).map { it.budget.id }).containsExactly("t", "b", "a").inOrder()
    }

    @Test fun `no spending gives zero fraction`() {
        val p = computeBudgetProgress(listOf(Budget("t", null, ym, Money(500))), emptyList()).single()
        assertThat(p.spent).isEqualTo(Money.ZERO); assertThat(p.fraction).isEqualTo(0.0)
    }
}
