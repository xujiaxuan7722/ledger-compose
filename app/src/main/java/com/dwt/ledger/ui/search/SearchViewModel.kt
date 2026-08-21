package com.dwt.ledger.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.data.AccountRepository
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.TransactionRepository
import com.dwt.ledger.domain.logic.MonthlySummary
import com.dwt.ledger.domain.logic.TransactionFilter
import com.dwt.ledger.domain.logic.applyFilter
import com.dwt.ledger.domain.logic.summarize
import com.dwt.ledger.domain.logic.toRange
import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.transactions.TransactionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** 时间范围预设 */
enum class DateRangePreset { ALL, THIS_MONTH, LAST_3_MONTHS, THIS_YEAR }

data class SearchResultRow(val date: LocalDate, val item: TransactionItem)

data class SearchUiState(
    val query: String = "",
    val kind: TransactionKind? = null,
    val categoryIds: Set<String> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val range: DateRangePreset = DateRangePreset.ALL,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val results: List<SearchResultRow> = emptyList(),
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val isLoading: Boolean = true,
) {
    val visibleCategories: List<Category> get() = if (kind == null) categories else categories.filter { it.kind == kind }
    val hasActiveFilter: Boolean get() = query.isNotBlank() || kind != null || categoryIds.isNotEmpty() || accountIds.isNotEmpty() || range != DateRangePreset.ALL
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    private val clock: Clock,
) : ViewModel() {

    private data class Criteria(
        val query: String = "",
        val kind: TransactionKind? = null,
        val categoryIds: Set<String> = emptySet(),
        val accountIds: Set<String> = emptySet(),
        val range: DateRangePreset = DateRangePreset.ALL,
    )

    private val criteria = MutableStateFlow(Criteria())

    val uiState: StateFlow<SearchUiState> = combine(
        criteria, transactionRepository.observeAll(), categoryRepository.observeAll(), accountRepository.observeAll(),
    ) { c, all, categories, accounts ->
        val categoryById = categories.associateBy { it.id }
        val accountById = accounts.associateBy { it.id }
        val filter = c.toFilter()
        val matched = all.applyFilter(filter, categories.associate { it.id to it.name })
        SearchUiState(
            query = c.query, kind = c.kind, categoryIds = c.categoryIds, accountIds = c.accountIds, range = c.range,
            categories = categories, accounts = accounts,
            results = matched.map { t ->
                SearchResultRow(
                    date = t.occurredAt.atZone(clock.zone).toLocalDate(),
                    item = TransactionItem(
                        id = t.id, kind = t.kind, amount = t.amount,
                        categoryName = categoryById[t.categoryId]?.name ?: "未分类",
                        categoryIcon = categoryById[t.categoryId]?.icon ?: "",
                        accountName = accountById[t.accountId]?.name ?: "",
                        note = t.note,
                    ),
                )
            },
            summary = matched.summarize(),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    private fun Criteria.toFilter(): TransactionFilter {
        val now = YearMonth.now(clock)
        val (start, end) = when (range) {
            DateRangePreset.ALL -> null to null
            DateRangePreset.THIS_MONTH -> now.toRange(clock.zone).let { it.start to it.endExclusive }
            DateRangePreset.LAST_3_MONTHS -> now.minusMonths(2).toRange(clock.zone).start to now.toRange(clock.zone).endExclusive
            DateRangePreset.THIS_YEAR -> YearMonth.of(now.year, 1).toRange(clock.zone).start to YearMonth.of(now.year, 12).toRange(clock.zone).endExclusive
        }
        return TransactionFilter(query, kind, categoryIds, accountIds, start, end)
    }

    fun setQuery(q: String) = criteria.update { it.copy(query = q) }
    /** 再点一次已选的类型 = 取消；切换类型时清掉不属于该类型的分类 */
    fun toggleKind(kind: TransactionKind) = criteria.update { c ->
        val next = if (c.kind == kind) null else kind
        c.copy(kind = next, categoryIds = if (next == null) c.categoryIds else emptySet())
    }
    fun toggleCategory(id: String) = criteria.update { it.copy(categoryIds = it.categoryIds.toggle(id)) }
    fun toggleAccount(id: String) = criteria.update { it.copy(accountIds = it.accountIds.toggle(id)) }
    fun setRange(range: DateRangePreset) = criteria.update { it.copy(range = range) }
    fun clearAll() = criteria.update { Criteria() }

    private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id
}
