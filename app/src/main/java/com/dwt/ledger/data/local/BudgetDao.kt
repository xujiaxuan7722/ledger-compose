package com.dwt.ledger.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE year_month = :yearMonth")
    fun observeForMonth(yearMonth: String): Flow<List<LocalBudget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): LocalBudget?

    /** 同月同分类已存在则覆盖（靠唯一索引 + 先删后插，避免 id 不同导致冲突） */
    @Query("DELETE FROM budgets WHERE year_month = :yearMonth AND ((:categoryId IS NULL AND category_id IS NULL) OR category_id = :categoryId)")
    suspend fun deleteForMonthAndCategory(yearMonth: String, categoryId: String?)

    /** 同月同分类重复插入会触发唯一索引冲突而抛错；仓库层负责先删旧记录 */
    @Insert
    suspend fun insert(budget: LocalBudget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: String)
}
