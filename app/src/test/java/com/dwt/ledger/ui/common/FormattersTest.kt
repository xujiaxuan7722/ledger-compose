package com.dwt.ledger.ui.common

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.YearMonth

class FormattersTest {
    @Test fun `yuan puts minus sign before currency symbol`() {
        assertThat(Money(2850).displayYuan()).isEqualTo("¥28.50")
        assertThat(Money(-2850).displayYuan()).isEqualTo("-¥28.50")
        assertThat(Money.ZERO.displayYuan()).isEqualTo("¥0.00")
    }
    @Test fun `signed display by kind`() {
        assertThat(Money(100).displaySigned(TransactionKind.INCOME)).isEqualTo("+¥1.00")
        assertThat(Money(100).displaySigned(TransactionKind.EXPENSE)).isEqualTo("-¥1.00")
    }
    @Test fun `month label in chinese`() {
        assertThat(YearMonth.of(2026, 8).display()).isEqualTo("2026年8月")
    }
}
