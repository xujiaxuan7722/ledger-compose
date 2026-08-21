package com.dwt.ledger.ui.search

import com.dwt.ledger.data.FakeAccountRepository
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val mainRule = MainCoroutineRule()
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), zone)
    private lateinit var viewModel: SearchViewModel
    private fun at(d: String) = LocalDate.parse(d).atTime(12, 0).atZone(zone).toInstant()

    @Before fun setUp() {
        val tx = FakeTransactionRepository()
        tx.seed(
            Transaction("a", TransactionKind.EXPENSE, Money(1500), "cat_food", "acc_cash", at("2026-08-20"), "早饭"),
            Transaction("b", TransactionKind.EXPENSE, Money(3000), "cat_transport", "acc_wechat", at("2026-07-02"), "地铁"),
            Transaction("c", TransactionKind.INCOME, Money(800000), "cat_salary", "acc_bank", at("2026-05-10")),
            Transaction("d", TransactionKind.EXPENSE, Money(999), "cat_food", "acc_cash", at("2025-12-31"), "跨年饭"),
        )
        viewModel = SearchViewModel(tx, FakeCategoryRepository(), FakeAccountRepository(), clock)
    }
    private fun runWith(block: suspend () -> Unit) = runTest(mainRule.dispatcher) { val j = launch { viewModel.uiState.collect {} }; block(); j.cancel() }

    @Test fun `no filter lists everything newest first with summary`() = runWith {
        val s = viewModel.uiState.value
        assertThat(s.results.map { it.item.id }).containsExactly("a", "b", "c", "d").inOrder()
        assertThat(s.summary.income).isEqualTo(Money(800000)); assertThat(s.summary.expense).isEqualTo(Money(5499))
        assertThat(s.hasActiveFilter).isFalse()
    }

    @Test fun `query matches note and category name`() = runWith {
        viewModel.setQuery("饭")
        assertThat(viewModel.uiState.value.results.map { it.item.id }).containsExactly("a", "d").inOrder()
        viewModel.setQuery("交通")
        assertThat(viewModel.uiState.value.results.map { it.item.id }).containsExactly("b")
    }

    @Test fun `date presets are relative to the clock`() = runWith {
        viewModel.setRange(DateRangePreset.THIS_MONTH)
        assertThat(viewModel.uiState.value.results.map { it.item.id }).containsExactly("a")
        viewModel.setRange(DateRangePreset.LAST_3_MONTHS)   // 6,7,8 月
        assertThat(viewModel.uiState.value.results.map { it.item.id }).containsExactly("a", "b").inOrder()
        viewModel.setRange(DateRangePreset.THIS_YEAR)
        assertThat(viewModel.uiState.value.results.map { it.item.id }).containsExactly("a", "b", "c").inOrder()
    }

    @Test fun `toggling kind twice clears it and categories follow kind`() = runWith {
        viewModel.toggleCategory("cat_food")
        viewModel.toggleKind(TransactionKind.INCOME)
        val s1 = viewModel.uiState.value
        assertThat(s1.kind).isEqualTo(TransactionKind.INCOME)
        assertThat(s1.categoryIds).isEmpty()
        assertThat(s1.visibleCategories.map { it.kind }.toSet()).containsExactly(TransactionKind.INCOME)
        assertThat(s1.results.map { it.item.id }).containsExactly("c")
        viewModel.toggleKind(TransactionKind.INCOME)
        assertThat(viewModel.uiState.value.kind).isNull()
        assertThat(viewModel.uiState.value.results).hasSize(4)
    }

    @Test fun `account filter and clearAll`() = runWith {
        viewModel.toggleAccount("acc_cash"); viewModel.toggleAccount("acc_wechat")
        assertThat(viewModel.uiState.value.results.map { it.item.id }).containsExactly("a", "b", "d").inOrder()
        assertThat(viewModel.uiState.value.hasActiveFilter).isTrue()
        viewModel.clearAll()
        assertThat(viewModel.uiState.value.hasActiveFilter).isFalse()
        assertThat(viewModel.uiState.value.results).hasSize(4)
    }
}
