package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {
    /** [start, end) 区间内流水，最新在前 */
    fun observeBetween(start: Instant, endExclusive: Instant): Flow<List<Transaction>>
    /** 全部流水，最新在前（搜索用；数据量大时应改为分页查询） */
    fun observeAll(): Flow<List<Transaction>>
    fun observeTransaction(id: String): Flow<Transaction?>
    suspend fun getTransaction(id: String): Transaction?

    /** 新建一笔，返回新 id */
    suspend fun create(
        kind: TransactionKind,
        amount: Money,
        categoryId: String,
        accountId: String,
        occurredAt: Instant,
        note: String,
    ): String

    suspend fun update(transaction: Transaction)
    suspend fun delete(id: String)
}
