package com.dwt.ledger.domain.model

import java.time.Instant

data class Transaction(
    val id: String,
    val kind: TransactionKind,
    val amount: Money,
    val categoryId: String,
    val accountId: String,
    val occurredAt: Instant,
    val note: String = "",
    val createdAt: Instant = occurredAt,
) {
    /** 对结余的贡献：收入为正、支出为负 */
    val signedAmount: Money get() = if (kind == TransactionKind.INCOME) amount else -amount
}
