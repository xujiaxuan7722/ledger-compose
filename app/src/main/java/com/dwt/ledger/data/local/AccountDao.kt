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

    @Upsert
    suspend fun upsertAll(accounts: List<LocalAccount>)
}
