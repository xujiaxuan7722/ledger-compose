package com.dwt.ledger.domain

import com.dwt.ledger.domain.model.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoneyTest {
    @Test fun `parse plain integer`() { assertThat(Money.parse("12")).isEqualTo(Money(1200)) }
    @Test fun `parse two decimals`() { assertThat(Money.parse("12.34")).isEqualTo(Money(1234)) }
    @Test fun `parse one decimal`() { assertThat(Money.parse("0.5")).isEqualTo(Money(50)) }
    @Test fun `parse rounds extra decimals half up`() { assertThat(Money.parse("1.005")).isEqualTo(Money(101)) }
    @Test fun `parse trims whitespace`() { assertThat(Money.parse(" 3 ")).isEqualTo(Money(300)) }
    @Test fun `parse rejects garbage`() {
        assertThat(Money.parse("")).isNull()
        assertThat(Money.parse("abc")).isNull()
        assertThat(Money.parse("1.2.3")).isNull()
    }
    @Test fun `format groups thousands and keeps two decimals`() {
        assertThat(Money(123456789).format()).isEqualTo("1,234,567.89")
        assertThat(Money(50).format()).isEqualTo("0.50")
        assertThat(Money.ZERO.format()).isEqualTo("0.00")
    }
    @Test fun `arithmetic`() {
        assertThat(Money(100) + Money(250)).isEqualTo(Money(350))
        assertThat(Money(100) - Money(250)).isEqualTo(Money(-150))
        assertThat(-Money(7)).isEqualTo(Money(-7))
        assertThat(Money(1) < Money(2)).isTrue()
    }
}
