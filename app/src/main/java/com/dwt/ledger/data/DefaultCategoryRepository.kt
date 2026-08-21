package com.dwt.ledger.data

import com.dwt.ledger.data.local.CategoryDao
import com.dwt.ledger.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { it.toDomain() }
}
