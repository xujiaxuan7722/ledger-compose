package com.dwt.ledger.data

import com.dwt.ledger.data.local.AccountDao
import com.dwt.ledger.data.local.TransactionDao
import com.dwt.ledger.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val dao: AccountDao,
    private val transactionDao: TransactionDao,
) : AccountRepository {
    override fun observeActive(): Flow<List<Account>> = dao.observeActive().map { it.toDomain() }
    override fun observeAll(): Flow<List<Account>> = dao.observeAll().map { it.toDomain() }

    override suspend fun create(name: String, icon: String): String {
        require(name.isNotBlank())
        val id = UUID.randomUUID().toString()
        dao.upsert(Account(id, name.trim(), icon, dao.maxSortOrder() + 10).toLocal())
        return id
    }

    override suspend fun rename(id: String, name: String, icon: String) {
        require(name.isNotBlank())
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(name = name.trim(), icon = icon))
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(archived = archived))
    }

    override suspend fun delete(id: String) {
        val used = transactionDao.countByAccount(id)
        if (used > 0) throw InUseException(used)
        dao.deleteById(id)
    }

    override suspend fun usageCount(id: String): Int = transactionDao.countByAccount(id)
}
