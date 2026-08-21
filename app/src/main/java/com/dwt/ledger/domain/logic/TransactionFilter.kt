package com.dwt.ledger.domain.logic

import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import java.time.Instant

/** 搜索条件；所有字段为空/null 表示不限制 */
data class TransactionFilter(
    val query: String = "",
    val kind: TransactionKind? = null,
    val categoryIds: Set<String> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val start: Instant? = null,
    /** 不含 */
    val endExclusive: Instant? = null,
) {
    val isEmpty: Boolean
        get() = query.isBlank() && kind == null && categoryIds.isEmpty() && accountIds.isEmpty() && start == null && endExclusive == null
}

/**
 * 纯函数：按条件过滤。关键词不区分大小写，匹配备注或分类名（分类名由 [categoryNameById] 提供）。
 */
fun List<Transaction>.applyFilter(
    filter: TransactionFilter,
    categoryNameById: Map<String, String> = emptyMap(),
): List<Transaction> {
    val q = filter.query.trim().lowercase()
    return filter { t ->
        (filter.kind == null || t.kind == filter.kind) &&
            (filter.categoryIds.isEmpty() || t.categoryId in filter.categoryIds) &&
            (filter.accountIds.isEmpty() || t.accountId in filter.accountIds) &&
            (filter.start == null || t.occurredAt >= filter.start) &&
            (filter.endExclusive == null || t.occurredAt < filter.endExclusive) &&
            (q.isEmpty() || t.note.lowercase().contains(q) || (categoryNameById[t.categoryId]?.lowercase()?.contains(q) == true))
    }
}
