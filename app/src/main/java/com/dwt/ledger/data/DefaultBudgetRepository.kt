package com.dwt.ledger.data

import com.dwt.ledger.data.local.BudgetDao
import com.dwt.ledger.domain.model.Budget
import com.dwt.ledger.domain.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBudgetRepository @Inject constructor(
    private val dao: BudgetDao,
) : BudgetRepository {
    override fun observeForMonth(yearMonth: YearMonth): Flow<List<Budget>> =
        dao.observeForMonth(yearMonth.toString()).map { it.toDomain() }

    override suspend fun getBudget(id: String): Budget? = dao.getById(id)?.toDomain()

    override suspend fun setBudget(yearMonth: YearMonth, categoryId: String?, limit: Money): String {
        require(limit.isPositive) { "limit must be > 0" }
        dao.deleteForMonthAndCategory(yearMonth.toString(), categoryId)
        val id = UUID.randomUUID().toString()
        dao.insert(Budget(id, categoryId, yearMonth, limit).toLocal())
        return id
    }

    override suspend fun delete(id: String) = dao.deleteById(id)
}
