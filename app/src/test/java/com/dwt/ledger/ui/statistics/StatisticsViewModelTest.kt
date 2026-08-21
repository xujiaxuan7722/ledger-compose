package com.dwt.ledger.ui.statistics

import com.dwt.ledger.data.FakeCategoryRepository
import com.dwt.ledger.data.FakeTransactionRepository
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.util.MainCoroutineRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class StatisticsViewModelTest {
    @get:Rule val mainRule = MainCoroutineRule()
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), zone)
    private lateinit var transactions: FakeTransactionRepository
    private lateinit var viewModel: StatisticsViewModel

    private fun at(date: String): Instant = LocalDate.parse(date).atTime(12, 0).atZone(zone).toInstant()

    @Before fun setUp() {
        transactions = FakeTransactionRepository()
        transactions.seed(
            Transaction("1", TransactionKind.EXPENSE, Money(3000), "cat_food", "acc_cash", at("2026-08-02")),
            Transaction("2", TransactionKind.EXPENSE, Money(1000), "cat_transport", "acc_cash", at("2026-08-03")),
            Transaction("3", TransactionKind.INCOME, Money(800000), "cat_salary", "acc_bank", at("2026-08-05")),
            Transaction("4", TransactionKind.EXPENSE, Money(500), "cat_food", "acc_cash", at("2026-07-15")),
            Transaction("5", TransactionKind.EXPENSE, Money(999), "cat_food", "acc_cash", at("2026-01-15")), // 6 个月窗口外
        )
        viewModel = StatisticsViewModel(transactions, FakeCategoryRepository(), clock)
    }

    private fun runWith(block: suspend () -> Unit) = runTest(mainRule.dispatcher) {
        val job = launch { viewModel.uiState.collect {} }; block(); job.cancel()
    }

    @Test fun `expense shares for current month with names and total`() = runWith {
        val s = viewModel.uiState.value
        assertThat(s.yearMonth).isEqualTo(YearMonth.of(2026, 8))
        assertThat(s.kind).isEqualTo(TransactionKind.EXPENSE)
        assertThat(s.total).isEqualTo(Money(4000))
        assertThat(s.shares.map { it.name }).containsExactly("餐饮", "交通").inOrder()
        assertThat(s.shares[0].fraction).isWithin(1e-9).of(0.75)
    }

    @Test fun `switching kind shows income shares`() = runWith {
        viewModel.setKind(TransactionKind.INCOME)
        val s = viewModel.uiState.value
        assertThat(s.shares.single().name).isEqualTo("工资")
        assertThat(s.total).isEqualTo(Money(800000))
    }

    @Test fun `trend covers six months ending at selected month`() = runWith {
        val trend = viewModel.uiState.value.trend
        assertThat(trend.map { it.yearMonth.monthValue }).containsExactly(3, 4, 5, 6, 7, 8).inOrder()
        assertThat(trend.last().expense).isEqualTo(Money(4000))
        assertThat(trend[4].expense).isEqualTo(Money(500)) // 7 月
        assertThat(trend[0].expense).isEqualTo(Money.ZERO)  // 3 月无数据
    }

    @Test fun `previous month moves both shares and trend window`() = runWith {
        viewModel.previousMonth()
        val s = viewModel.uiState.value
        assertThat(s.yearMonth).isEqualTo(YearMonth.of(2026, 7))
        assertThat(s.total).isEqualTo(Money(500))
        assertThat(s.trend.map { it.yearMonth.monthValue }).containsExactly(2, 3, 4, 5, 6, 7).inOrder()
    }
}
