package com.dwt.ledger.domain

import com.dwt.ledger.domain.logic.MonthlySummary
import com.dwt.ledger.domain.logic.summarize
import com.dwt.ledger.domain.logic.toRange
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class MonthlySummaryTest {
    private fun t(kind: TransactionKind, cents: Long) =
        Transaction("id$cents", kind, Money(cents), "c", "a", Instant.EPOCH)

    @Test fun `empty list gives zero summary`() {
        assertThat(emptyList<Transaction>().summarize()).isEqualTo(MonthlySummary.EMPTY)
    }

    @Test fun `sums income and expense separately and balance is difference`() {
        val s = listOf(
            t(TransactionKind.INCOME, 10_000), t(TransactionKind.INCOME, 2_500),
            t(TransactionKind.EXPENSE, 3_000), t(TransactionKind.EXPENSE, 125),
        ).summarize()
        assertThat(s.income).isEqualTo(Money(12_500))
        assertThat(s.expense).isEqualTo(Money(3_125))
        assertThat(s.balance).isEqualTo(Money(9_375))
    }

    @Test fun `balance can be negative - spending more than earning is allowed`() {
        val s = listOf(t(TransactionKind.EXPENSE, 500)).summarize()
        assertThat(s.balance).isEqualTo(Money(-500))
    }

    @Test fun `month range covers the whole month in the given zone`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val r = YearMonth.of(2026, 2).toRange(zone)
        assertThat(r.start).isEqualTo(Instant.parse("2026-01-31T16:00:00Z")) // 2026-02-01 00:00 +08:00
        assertThat(r.endExclusive).isEqualTo(Instant.parse("2026-02-28T16:00:00Z")) // 2026-03-01 00:00 +08:00
    }
}
