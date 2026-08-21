package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
}
