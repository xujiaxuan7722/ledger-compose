package com.dwt.ledger.ui.budget

import com.dwt.ledger.data.FakeBudgetRepository
import com.dwt.ledger.data.FakeCategoryRepository
import com.dwt.ledger.data.FakeTransactionRepository
import com.dwt.ledger.domain.model.Budget
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
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {
    @get:Rule val mainRule = MainCoroutineRule()
    private val clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), ZoneId.of("Asia/Shanghai"))
    private val ym = YearMonth.of(2026, 8)
    private lateinit var budgets: FakeBudgetRepository
    private lateinit var transactions: FakeTransactionRepository
    private lateinit var viewModel: BudgetViewModel

    @Before fun setUp() {
        budgets = FakeBudgetRepository(); transactions = FakeTransactionRepository()
        transactions.seed(Transaction("1", TransactionKind.EXPENSE, Money(3_500), "cat_food", "acc_cash", Instant.parse("2026-08-02T04:00:00Z")))
        viewModel = BudgetViewModel(budgets, transactions, FakeCategoryRepository(), clock)
    }
    private fun runWith(block: suspend () -> Unit) = runTest(mainRule.dispatcher) { val j = launch { viewModel.uiState.collect {} }; block(); j.cancel() }

    @Test fun `items join category names and flag overspend`() = runWith {
        budgets.seed(Budget("b1", "cat_food", ym, Money(3_000)), Budget("b2", null, ym, Money(10_000)))
        val items = viewModel.uiState.value.items
        assertThat(items.map { it.title }).containsExactly("总预算", "餐饮").inOrder()
        assertThat(items[1].progress.isOver).isTrue()
        assertThat(viewModel.uiState.value.expenseCategories.map { it.kind }.toSet()).containsExactly(TransactionKind.EXPENSE)
    }

    @Test fun `editor validates amount then saves for current month and closes`() = runWith {
        viewModel.openEditor()
        viewModel.editorSelectCategory("cat_food")
        viewModel.editorSetAmount("abc"); viewModel.saveEditor()
        assertThat(viewModel.uiState.value.editor?.error).isTrue()
        viewModel.editorSetAmount("500"); viewModel.saveEditor()
        assertThat(viewModel.uiState.value.editor).isNull()
        val b = budgets.all.single()
        assertThat(b.categoryId).isEqualTo("cat_food"); assertThat(b.yearMonth).isEqualTo(ym); assertThat(b.limit).isEqualTo(Money(50_000))
    }

    @Test fun `saving same scope again replaces instead of duplicating`() = runWith {
        viewModel.openEditor(); viewModel.editorSetAmount("100"); viewModel.saveEditor()   // 总预算 100
        viewModel.openEditor(); viewModel.editorSetAmount("200"); viewModel.saveEditor()   // 总预算 200
        assertThat(budgets.all).hasSize(1); assertThat(budgets.all.single().limit).isEqualTo(Money(20_000))
    }

    @Test fun `opening existing item prefills and delete removes it`() = runWith {
        budgets.seed(Budget("b1", "cat_food", ym, Money(123_456)))
        viewModel.openEditor(viewModel.uiState.value.items.single())
        assertThat(viewModel.uiState.value.editor?.amountText).isEqualTo("1234.56")
        viewModel.deleteEditing()
        assertThat(budgets.all).isEmpty(); assertThat(viewModel.uiState.value.editor).isNull()
    }
}
