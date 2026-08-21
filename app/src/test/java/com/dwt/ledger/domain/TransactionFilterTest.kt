package com.dwt.ledger.domain

import com.dwt.ledger.domain.logic.TransactionFilter
import com.dwt.ledger.domain.logic.applyFilter
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class TransactionFilterTest {
    private val names = mapOf("cat_food" to "餐饮", "cat_salary" to "工资")
    private val list = listOf(
        Transaction("1", TransactionKind.EXPENSE, Money(100), "cat_food", "acc_cash", Instant.ofEpochSecond(100), "Lunch at school"),
        Transaction("2", TransactionKind.EXPENSE, Money(200), "cat_bus", "acc_wechat", Instant.ofEpochSecond(200), ""),
        Transaction("3", TransactionKind.INCOME, Money(5000), "cat_salary", "acc_bank", Instant.ofEpochSecond(300), "8月"),
    )

    @Test fun `empty filter keeps everything`() {
        assertThat(list.applyFilter(TransactionFilter())).hasSize(3)
        assertThat(TransactionFilter().isEmpty).isTrue()
    }
    @Test fun `query matches note case-insensitively`() {
        assertThat(list.applyFilter(TransactionFilter(query = "LUNCH")).map { it.id }).containsExactly("1")
    }
    @Test fun `query matches category name`() {
        assertThat(list.applyFilter(TransactionFilter(query = "工资"), names).map { it.id }).containsExactly("3")
        assertThat(list.applyFilter(TransactionFilter(query = "工资")).map { it.id }).isEmpty() // 没给名字映射就匹配不到
    }
    @Test fun `kind category account filters combine with AND`() {
        assertThat(list.applyFilter(TransactionFilter(kind = TransactionKind.EXPENSE)).map { it.id }).containsExactly("1", "2")
        assertThat(list.applyFilter(TransactionFilter(kind = TransactionKind.EXPENSE, accountIds = setOf("acc_wechat"))).map { it.id }).containsExactly("2")
        assertThat(list.applyFilter(TransactionFilter(categoryIds = setOf("cat_food", "cat_salary"))).map { it.id }).containsExactly("1", "3")
        assertThat(list.applyFilter(TransactionFilter(categoryIds = setOf("cat_food"), accountIds = setOf("acc_bank")))).isEmpty()
    }
    @Test fun `time range is half open`() {
        val r = list.applyFilter(TransactionFilter(start = Instant.ofEpochSecond(200), endExclusive = Instant.ofEpochSecond(300)))
        assertThat(r.map { it.id }).containsExactly("2")
    }
}
