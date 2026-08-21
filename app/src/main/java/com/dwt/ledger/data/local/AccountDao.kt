package com.dwt.ledger.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sort_order ASC")
    fun observeActive(): Flow<List<LocalAccount>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM accounts ORDER BY archived ASC, sort_order ASC")
    fun observeAll(): Flow<List<LocalAccount>>

    @Upsert
    suspend fun upsertAll(accounts: List<LocalAccount>)

    @Upsert
    suspend fun upsert(account: LocalAccount)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): LocalAccount?

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM accounts")
    suspend fun maxSortOrder(): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)
}
