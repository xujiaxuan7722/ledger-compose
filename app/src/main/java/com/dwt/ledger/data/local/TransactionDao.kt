package com.dwt.ledger.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    /** [start, end) 区间内的流水，按发生时间倒序（最新在前） */
    @Query(
        "SELECT * FROM transactions WHERE occurred_at >= :startMillis AND occurred_at < :endMillis " +
            "ORDER BY occurred_at DESC, created_at DESC"
    )
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<LocalTransaction>>

    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC, created_at DESC")
    fun observeAll(): Flow<List<LocalTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<LocalTransaction?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): LocalTransaction?

    @Upsert
    suspend fun upsert(transaction: LocalTransaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun countByAccount(accountId: String): Int
}
