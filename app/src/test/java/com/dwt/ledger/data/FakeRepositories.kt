package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeTransactionRepository : TransactionRepository {
    private val store = MutableStateFlow<Map<String, Transaction>>(emptyMap())
    private var nextId = 1
    val all: List<Transaction> get() = store.value.values.toList()

    fun seed(vararg transactions: Transaction) = store.update { it + transactions.associateBy { t -> t.id } }

    override fun observeBetween(start: Instant, endExclusive: Instant): Flow<List<Transaction>> =
        store.map { m ->
            m.values.filter { it.occurredAt >= start && it.occurredAt < endExclusive }
                .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.createdAt })
        }

    override fun observeTransaction(id: String): Flow<Transaction?> = store.map { it[id] }
    override suspend fun getTransaction(id: String): Transaction? = store.value[id]

    override suspend fun create(kind: TransactionKind, amount: Money, categoryId: String, accountId: String, occurredAt: Instant, note: String): String {
        val id = "t${nextId++}"
        store.update { it + (id to Transaction(id, kind, amount, categoryId, accountId, occurredAt, note, createdAt = occurredAt)) }
        return id
    }

    override suspend fun update(transaction: Transaction) = store.update { it + (transaction.id to transaction) }
    override suspend fun delete(id: String) = store.update { it - id }
}

class FakeCategoryRepository(initial: List<Category> = DefaultDataSeeder.DEFAULT_CATEGORIES) : CategoryRepository {
    val categories = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Category>> = categories
}

class FakeAccountRepository(initial: List<Account> = DefaultDataSeeder.DEFAULT_ACCOUNTS) : AccountRepository {
    val accounts = MutableStateFlow(initial)
    override fun observeActive(): Flow<List<Account>> = accounts.map { list -> list.filter { !it.archived } }
}
