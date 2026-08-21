package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Budget
import com.dwt.ledger.domain.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface BudgetRepository {
    fun observeForMonth(yearMonth: YearMonth): Flow<List<Budget>>
    suspend fun getBudget(id: String): Budget?
    /** 设置某月某分类（null=总预算）的额度；已存在则覆盖。返回 id */
    suspend fun setBudget(yearMonth: YearMonth, categoryId: String?, limit: Money): String
    suspend fun delete(id: String)
}
