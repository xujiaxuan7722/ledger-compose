package com.dwt.ledger.data

import com.dwt.ledger.data.local.TransactionDao
import com.dwt.ledger.di.IoDispatcher
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun observeBetween(start: Instant, endExclusive: Instant): Flow<List<Transaction>> =
        dao.observeBetween(start.toEpochMilli(), endExclusive.toEpochMilli()).map { it.toDomain() }

    override fun observeTransaction(id: String): Flow<Transaction?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getTransaction(id: String): Transaction? =
        dao.getById(id)?.toDomain()

    override suspend fun create(
        kind: TransactionKind,
        amount: Money,
        categoryId: String,
        accountId: String,
        occurredAt: Instant,
        note: String,
    ): String {
        require(amount.isPositive) { "amount must be > 0" }
        val id = withContext(ioDispatcher) { UUID.randomUUID().toString() }
        val now = Instant.now()
        dao.upsert(
            Transaction(id, kind, amount, categoryId, accountId, occurredAt, note, createdAt = now).toLocal()
        )
        return id
    }

    override suspend fun update(transaction: Transaction) {
        require(transaction.amount.isPositive) { "amount must be > 0" }
        dao.upsert(transaction.toLocal())
    }

    override suspend fun delete(id: String) = dao.deleteById(id)
}
