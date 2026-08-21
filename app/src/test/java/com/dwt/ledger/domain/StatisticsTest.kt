package com.dwt.ledger.domain

import com.dwt.ledger.domain.logic.shareByCategory
import com.dwt.ledger.domain.logic.trailingMonths
import com.dwt.ledger.domain.logic.trendByMonth
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatisticsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private fun t(kind: TransactionKind, cents: Long, cat: String, date: String = "2026-08-10") =
        Transaction("$cat-$cents-$date", kind, Money(cents), cat, "acc", LocalDate.parse(date).atTime(12, 0).atZone(zone).toInstant())

    @Test fun `share groups by category, sorts desc, fractions sum to one`() {
        val shares = listOf(
            t(TransactionKind.EXPENSE, 300, "food"), t(TransactionKind.EXPENSE, 200, "food"),
            t(TransactionKind.EXPENSE, 400, "rent"), t(TransactionKind.EXPENSE, 100, "bus"),
            t(TransactionKind.INCOME, 9999, "salary"), // 不同类型不计入
        ).shareByCategory(TransactionKind.EXPENSE)
        assertThat(shares.map { it.categoryId }).containsExactly("food", "rent", "bus").inOrder()
        assertThat(shares[0].amount).isEqualTo(Money(500))
        assertThat(shares[0].fraction).isWithin(1e-9).of(0.5)
        assertThat(shares.sumOf { it.fraction }).isWithin(1e-9).of(1.0)
    }

    @Test fun `share is empty when no transactions of that kind`() {
        assertThat(listOf(t(TransactionKind.INCOME, 100, "salary")).shareByCategory(TransactionKind.EXPENSE)).isEmpty()
    }

    @Test fun `trailing months are consecutive ascending and end at given month`() {
        assertThat(trailingMonths(YearMonth.of(2026, 2), 4)).containsExactly(
            YearMonth.of(2025, 11), YearMonth.of(2025, 12), YearMonth.of(2026, 1), YearMonth.of(2026, 2)
        ).inOrder()
    }

    @Test fun `trend fills months without data with zero and buckets by zone`() {
        val months = trailingMonths(YearMonth.of(2026, 8), 3) // 6,7,8
        val points = listOf(
            t(TransactionKind.INCOME, 1000, "salary", "2026-06-30"),
            t(TransactionKind.EXPENSE, 50, "food", "2026-08-01"),
            t(TransactionKind.EXPENSE, 70, "food", "2026-08-21"),
            t(TransactionKind.EXPENSE, 999, "food", "2026-05-31"), // 范围外
        ).trendByMonth(months, zone)
        assertThat(points.map { it.yearMonth }).isEqualTo(months)
        assertThat(points[0].income).isEqualTo(Money(1000)); assertThat(points[0].expense).isEqualTo(Money.ZERO)
        assertThat(points[1].income).isEqualTo(Money.ZERO);  assertThat(points[1].expense).isEqualTo(Money.ZERO)
        assertThat(points[2].expense).isEqualTo(Money(120))
    }
}
