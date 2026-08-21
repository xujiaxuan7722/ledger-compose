package com.dwt.ledger.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.TransactionRepository
import com.dwt.ledger.domain.logic.MonthPoint
import com.dwt.ledger.domain.logic.shareByCategory
import com.dwt.ledger.domain.logic.toRange
import com.dwt.ledger.domain.logic.trailingMonths
import com.dwt.ledger.domain.logic.trendByMonth
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
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

data class CategoryShareItem(
    val categoryId: String,
    val name: String,
    val icon: String,
    val amount: Money,
    val fraction: Double,
)

data class StatisticsUiState(
    val yearMonth: YearMonth,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val total: Money = Money.ZERO,
    val shares: List<CategoryShareItem> = emptyList(),
    val trend: List<MonthPoint> = emptyList(),
    val isLoading: Boolean = true,
)

const val TREND_MONTHS = 6

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val yearMonth = MutableStateFlow(YearMonth.now(clock))
    private val kind = MutableStateFlow(TransactionKind.EXPENSE)

    /** 近 6 个月的流水一次取出，本月占比和趋势都从这份数据算 */
    private val trailingTransactions = yearMonth.flatMapLatest { ym ->
        val months = trailingMonths(ym, TREND_MONTHS)
        val start = months.first().toRange(clock.zone).start
        val end = months.last().toRange(clock.zone).endExclusive
        transactionRepository.observeBetween(start, end)
    }

    val uiState: StateFlow<StatisticsUiState> = combine(
        yearMonth, kind, trailingTransactions, categoryRepository.observeAll(),
    ) { ym, k, transactions, categories ->
        val range = ym.toRange(clock.zone)
        val thisMonth = transactions.filter { it.occurredAt >= range.start && it.occurredAt < range.endExclusive }
        val categoryById = categories.associateBy { it.id }
        val shares = thisMonth.shareByCategory(k).map { s ->
            val c = categoryById[s.categoryId]
            CategoryShareItem(s.categoryId, c?.name ?: "未分类", c?.icon ?: "", s.amount, s.fraction)
        }
        StatisticsUiState(
            yearMonth = ym,
            kind = k,
            total = Money(shares.sumOf { it.amount.cents }),
            shares = shares,
            trend = transactions.trendByMonth(trailingMonths(ym, TREND_MONTHS), clock.zone),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState(yearMonth = yearMonth.value))

    fun previousMonth() = yearMonth.update { it.minusMonths(1) }
    fun nextMonth() = yearMonth.update { it.plusMonths(1) }
    fun setKind(value: TransactionKind) = kind.update { value }
}
