package com.dwt.ledger.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<LocalCategory>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(categories: List<LocalCategory>)
}
