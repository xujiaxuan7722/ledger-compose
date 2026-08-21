package com.dwt.ledger.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.data.BudgetRepository
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.TransactionRepository
import com.dwt.ledger.domain.logic.BudgetProgress
import com.dwt.ledger.domain.logic.computeBudgetProgress
import com.dwt.ledger.domain.logic.toRange
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.TransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

data class BudgetItem(
    val progress: BudgetProgress,
    /** 总预算显示「总预算」，否则分类名 */
    val title: String,
    val icon: String,
)

/** 编辑弹窗的状态；null 表示弹窗关闭 */
data class BudgetEditor(
    val budgetId: String? = null,
    val categoryId: String? = null,
    val amountText: String = "",
    val error: Boolean = false,
)

data class BudgetUiState(
    val yearMonth: YearMonth,
    val items: List<BudgetItem> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val editor: BudgetEditor? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(YearMonth.now(clock))
    private val editor = MutableStateFlow<BudgetEditor?>(null)

    private val budgets = yearMonth.flatMapLatest { budgetRepository.observeForMonth(it) }
    private val monthTransactions = yearMonth.flatMapLatest { ym ->
        val r = ym.toRange(clock.zone); transactionRepository.observeBetween(r.start, r.endExclusive)
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        yearMonth, budgets, monthTransactions, categoryRepository.observeAll(), editor,
    ) { ym, budgetList, transactions, categories, ed ->
        val byId = categories.associateBy { it.id }
        BudgetUiState(
            yearMonth = ym,
            items = computeBudgetProgress(budgetList, transactions).map { p ->
                val c = p.budget.categoryId?.let { byId[it] }
                BudgetItem(p, title = if (p.budget.isTotal) "总预算" else c?.name ?: "未分类", icon = c?.icon ?: "savings")
            },
            expenseCategories = categories.filter { it.kind == TransactionKind.EXPENSE },
            editor = ed,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState(yearMonth = yearMonth.value))

    fun previousMonth() = yearMonth.update { it.minusMonths(1) }
    fun nextMonth() = yearMonth.update { it.plusMonths(1) }

    fun openEditor(item: BudgetItem? = null) = editor.update {
        if (item == null) BudgetEditor()
        else BudgetEditor(item.progress.budget.id, item.progress.budget.categoryId, item.progress.budget.limit.format().replace(",", ""))
    }
    fun closeEditor() = editor.update { null }
    fun editorSelectCategory(categoryId: String?) = editor.update { it?.copy(categoryId = categoryId, error = false) }
    fun editorSetAmount(text: String) = editor.update { it?.copy(amountText = text, error = false) }

    fun saveEditor() {
        val ed = editor.value ?: return
        val limit = Money.parse(ed.amountText)
        if (limit == null || !limit.isPositive) { editor.update { it?.copy(error = true) }; return }
        viewModelScope.launch {
            budgetRepository.setBudget(yearMonth.value, ed.categoryId, limit)
            editor.update { null }
        }
    }

    fun deleteEditing() {
        val id = editor.value?.budgetId ?: return
        viewModelScope.launch { budgetRepository.delete(id); editor.update { null } }
    }
}
