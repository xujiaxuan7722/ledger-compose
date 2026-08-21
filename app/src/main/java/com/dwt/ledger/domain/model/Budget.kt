package com.dwt.ledger.domain.model

import java.time.YearMonth

/**
 * 月度预算。[categoryId] 为 null 表示该月总预算（所有支出），否则是某个支出分类的预算。
 */
data class Budget(
    val id: String,
    val categoryId: String?,
    val yearMonth: YearMonth,
    val limit: Money,
) {
    val isTotal: Boolean get() = categoryId == null
}
