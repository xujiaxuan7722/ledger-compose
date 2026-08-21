package com.dwt.ledger.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.data.AccountRepository
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.TransactionRepository
import com.dwt.ledger.domain.logic.MonthlySummary
import com.dwt.ledger.domain.logic.summarize
import com.dwt.ledger.domain.logic.toRange
import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
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
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TransactionItem(
    val id: String,
    val kind: TransactionKind,
    val amount: Money,
    val categoryName: String,
    val categoryIcon: String,
    val accountName: String,
    val note: String,
)

data class DayGroup(
    val date: LocalDate,
    /** 当日净额：收入 - 支出 */
    val net: Money,
    val items: List<TransactionItem>,
)

data class TransactionsUiState(
    val yearMonth: YearMonth,
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val days: List<DayGroup> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && days.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    private val clock: Clock,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(YearMonth.now(clock))

    private val transactionsForMonth = yearMonth.flatMapLatest { ym ->
        val range = ym.toRange(clock.zone)
        transactionRepository.observeBetween(range.start, range.endExclusive)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        yearMonth,
        transactionsForMonth,
        categoryRepository.observeAll(),
        accountRepository.observeActive(),
    ) { ym, transactions, categories, accounts ->
        TransactionsUiState(
            yearMonth = ym,
            summary = transactions.summarize(),
            days = groupByDay(transactions, categories, accounts),
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionsUiState(yearMonth = yearMonth.value),
    )

    fun previousMonth() = yearMonth.update { it.minusMonths(1) }
    fun nextMonth() = yearMonth.update { it.plusMonths(1) }

    private fun groupByDay(
        transactions: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>,
    ): List<DayGroup> {
        val categoryById = categories.associateBy { it.id }
        val accountById = accounts.associateBy { it.id }
        return transactions
            .groupBy { it.occurredAt.atZone(clock.zone).toLocalDate() }
            .toSortedMap(compareByDescending { it })
            .map { (date, list) ->
                DayGroup(
                    date = date,
                    net = list.fold(Money.ZERO) { acc, t -> acc + t.signedAmount },
                    items = list.map { t ->
                        TransactionItem(
                            id = t.id,
                            kind = t.kind,
                            amount = t.amount,
                            categoryName = categoryById[t.categoryId]?.name ?: "未分类",
                            categoryIcon = categoryById[t.categoryId]?.icon ?: "",
                            accountName = accountById[t.accountId]?.name ?: "",
                            note = t.note,
                        )
                    },
                )
            }
    }
}
