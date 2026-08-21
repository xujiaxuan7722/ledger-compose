package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    /** 未归档账户（记账时可选） */
    fun observeActive(): Flow<List<Account>>
    /** 全部账户（管理页），未归档在前 */
    fun observeAll(): Flow<List<Account>>
    suspend fun create(name: String, icon: String): String
    suspend fun rename(id: String, name: String, icon: String)
    suspend fun setArchived(id: String, archived: Boolean)
    /** @throws InUseException 有流水引用时 */
    suspend fun delete(id: String)
    suspend fun usageCount(id: String): Int
}
