package com.dwt.ledger.domain.logic

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import java.time.YearMonth
import java.time.ZoneId

/** 某分类在某类流水中的占比 */
data class CategoryShare(
    val categoryId: String,
    val amount: Money,
    /** 0.0 ~ 1.0；总额为 0 时为 0.0 */
    val fraction: Double,
)

/** 按分类汇总指定类型（收入/支出）的流水，金额降序；总额为 0 时返回空列表 */
fun List<Transaction>.shareByCategory(kind: TransactionKind): List<CategoryShare> {
    val filtered = filter { it.kind == kind }
    val total = filtered.sumOf { it.amount.cents }
    if (total == 0L) return emptyList()
    return filtered
        .groupBy { it.categoryId }
        .map { (categoryId, list) ->
            val sum = list.sumOf { it.amount.cents }
            CategoryShare(categoryId, Money(sum), sum.toDouble() / total)
        }
        .sortedWith(compareByDescending<CategoryShare> { it.amount.cents }.thenBy { it.categoryId })
}

/** 某个月的收入/支出合计（用于趋势图） */
data class MonthPoint(val yearMonth: YearMonth, val income: Money, val expense: Money)

/** 连续 [count] 个月（含 [endInclusive]）的列表，升序 */
fun trailingMonths(endInclusive: YearMonth, count: Int): List<YearMonth> =
    (count - 1 downTo 0).map { endInclusive.minusMonths(it.toLong()) }

/** 把流水按月份汇总；[months] 里没有流水的月份也会出现（金额为 0），保证图表横轴连续 */
fun List<Transaction>.trendByMonth(months: List<YearMonth>, zone: ZoneId): List<MonthPoint> {
    val byMonth = groupBy { YearMonth.from(it.occurredAt.atZone(zone)) }
    return months.map { ym ->
        val s = (byMonth[ym] ?: emptyList()).summarize()
        MonthPoint(ym, s.income, s.expense)
    }
}
