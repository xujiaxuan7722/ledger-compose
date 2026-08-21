package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow

/** 删除被流水引用的分类/账户时抛出 */
class InUseException(val usageCount: Int) : IllegalStateException("in use by $usageCount transactions")

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun create(name: String, kind: TransactionKind, icon: String): String
    suspend fun rename(id: String, name: String, icon: String)
    /** @throws InUseException 有流水引用时 */
    suspend fun delete(id: String)
    suspend fun usageCount(id: String): Int
}
