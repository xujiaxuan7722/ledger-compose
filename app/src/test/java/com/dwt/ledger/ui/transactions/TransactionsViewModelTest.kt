package com.dwt.ledger.ui.transactions

import com.dwt.ledger.data.FakeAccountRepository
import com.dwt.ledger.data.FakeCategoryRepository
import com.dwt.ledger.data.FakeTransactionRepository
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.util.MainCoroutineRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    @get:Rule val mainRule = MainCoroutineRule()

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-08-21T06:00:00Z") // 2026-08-21 14:00 北京
    private val clock = Clock.fixed(now, zone)
    private lateinit var transactions: FakeTransactionRepository
    private lateinit var viewModel: TransactionsViewModel

    private fun at(date: String, hour: Int = 12): Instant =
        LocalDate.parse(date).atTime(hour, 0).atZone(zone).toInstant()

    @Before fun setUp() {
        transactions = FakeTransactionRepository()
        viewModel = TransactionsViewModel(transactions, FakeCategoryRepository(), FakeAccountRepository(), clock)
    }

    /** stateIn(WhileSubscribed) 需要有订阅者才会开始计算 */
    private fun runTest(block: suspend () -> Unit) = kotlinx.coroutines.test.runTest(mainRule.dispatcher) {
        val job: Job = launch { viewModel.uiState.collect {} }
        block()
        job.cancel()
    }

    @Test fun `starts on current month and empty`() = runTest {
        val s = viewModel.uiState.value
        assertThat(s.yearMonth).isEqualTo(YearMonth.of(2026, 8))
        assertThat(s.isLoading).isFalse()
        assertThat(s.isEmpty).isTrue()
    }

    @Test fun `shows only this month, grouped by day newest first, with summary`() = runTest {
        transactions.seed(
            Transaction("a", TransactionKind.EXPENSE, Money(1500), "cat_food", "acc_cash", at("2026-08-21", 9), "早饭"),
            Transaction("b", TransactionKind.EXPENSE, Money(3000), "cat_transport", "acc_wechat", at("2026-08-20")),
            Transaction("c", TransactionKind.INCOME, Money(800000), "cat_salary", "acc_bank", at("2026-08-10")),
            Transaction("old", TransactionKind.EXPENSE, Money(99999), "cat_food", "acc_cash", at("2026-07-31", 23)),
        )
        val s = viewModel.uiState.value
        assertThat(s.days.map { it.date }).containsExactly(
            LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 10)
        ).inOrder()
        assertThat(s.summary.income).isEqualTo(Money(800000))
        assertThat(s.summary.expense).isEqualTo(Money(4500))
        assertThat(s.summary.balance).isEqualTo(Money(795500))

        val first = s.days.first()
        assertThat(first.net).isEqualTo(Money(-1500))
        assertThat(first.items.single().categoryName).isEqualTo("餐饮")
        assertThat(first.items.single().accountName).isEqualTo("现金")
        assertThat(first.items.single().note).isEqualTo("早饭")
    }

    @Test fun `previous and next month move the window`() = runTest {
        transactions.seed(Transaction("old", TransactionKind.EXPENSE, Money(100), "cat_food", "acc_cash", at("2026-07-15")))
        viewModel.previousMonth()
        assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 7))
        assertThat(viewModel.uiState.value.days).hasSize(1)
        viewModel.nextMonth()
        assertThat(viewModel.uiState.value.yearMonth).isEqualTo(YearMonth.of(2026, 8))
        assertThat(viewModel.uiState.value.days).isEmpty()
    }

    @Test fun `unknown category falls back to placeholder instead of crashing`() = runTest {
        transactions.seed(Transaction("x", TransactionKind.EXPENSE, Money(100), "cat_deleted", "acc_cash", at("2026-08-01")))
        val item = viewModel.uiState.value.days.single().items.single()
        assertThat(item.categoryName).isEqualTo("未分类")
    }
}
