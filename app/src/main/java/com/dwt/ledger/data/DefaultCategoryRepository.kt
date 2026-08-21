package com.dwt.ledger.data

import com.dwt.ledger.data.local.CategoryDao
import com.dwt.ledger.data.local.TransactionDao
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    private val transactionDao: TransactionDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { it.toDomain() }

    override suspend fun create(name: String, kind: TransactionKind, icon: String): String {
        require(name.isNotBlank())
        val id = UUID.randomUUID().toString()
        dao.upsert(Category(id, name.trim(), kind, icon, dao.maxSortOrder(kind.name) + 10).toLocal())
        return id
    }

    override suspend fun rename(id: String, name: String, icon: String) {
        require(name.isNotBlank())
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(name = name.trim(), icon = icon))
    }

    override suspend fun delete(id: String) {
        val used = transactionDao.countByCategory(id)
        if (used > 0) throw InUseException(used)
        dao.deleteById(id)
    }

    override suspend fun usageCount(id: String): Int = transactionDao.countByCategory(id)
}
