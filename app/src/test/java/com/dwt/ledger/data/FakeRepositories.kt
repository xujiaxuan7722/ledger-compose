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
    /** 模拟"被 N 笔流水引用"：id -> 笔数 */
    val usage = mutableMapOf<String, Int>()
    private var nextId = 1
    override fun observeAll(): Flow<List<Category>> = categories
    override suspend fun create(name: String, kind: TransactionKind, icon: String): String {
        val id = "c${nextId++}"
        categories.update { it + Category(id, name.trim(), kind, icon, (it.maxOfOrNull { c -> c.sortOrder } ?: 0) + 10) }
        return id
    }
    override suspend fun rename(id: String, name: String, icon: String) =
        categories.update { l -> l.map { if (it.id == id) it.copy(name = name.trim(), icon = icon) else it } }
    override suspend fun delete(id: String) {
        val used = usage[id] ?: 0
        if (used > 0) throw InUseException(used)
        categories.update { l -> l.filterNot { it.id == id } }
    }
    override suspend fun usageCount(id: String): Int = usage[id] ?: 0
}

class FakeAccountRepository(initial: List<Account> = DefaultDataSeeder.DEFAULT_ACCOUNTS) : AccountRepository {
    val accounts = MutableStateFlow(initial)
    val usage = mutableMapOf<String, Int>()
    private var nextId = 1
    override fun observeActive(): Flow<List<Account>> = accounts.map { list -> list.filter { !it.archived } }
    override fun observeAll(): Flow<List<Account>> = accounts.map { list -> list.sortedBy { it.archived } }
    override suspend fun create(name: String, icon: String): String {
        val id = "a${nextId++}"
        accounts.update { it + Account(id, name.trim(), icon, (it.maxOfOrNull { a -> a.sortOrder } ?: 0) + 10) }
        return id
    }
    override suspend fun rename(id: String, name: String, icon: String) =
        accounts.update { l -> l.map { if (it.id == id) it.copy(name = name.trim(), icon = icon) else it } }
    override suspend fun setArchived(id: String, archived: Boolean) =
        accounts.update { l -> l.map { if (it.id == id) it.copy(archived = archived) else it } }
    override suspend fun delete(id: String) {
        val used = usage[id] ?: 0
        if (used > 0) throw InUseException(used)
        accounts.update { l -> l.filterNot { it.id == id } }
    }
    override suspend fun usageCount(id: String): Int = usage[id] ?: 0
}

class FakeBudgetRepository : BudgetRepository {
    private val store = MutableStateFlow<Map<String, com.dwt.ledger.domain.model.Budget>>(emptyMap())
    private var nextId = 1
    val all: List<com.dwt.ledger.domain.model.Budget> get() = store.value.values.toList()
    fun seed(vararg budgets: com.dwt.ledger.domain.model.Budget) = store.update { it + budgets.associateBy { b -> b.id } }

    override fun observeForMonth(yearMonth: java.time.YearMonth): Flow<List<com.dwt.ledger.domain.model.Budget>> =
        store.map { m -> m.values.filter { it.yearMonth == yearMonth } }
    override suspend fun getBudget(id: String) = store.value[id]
    override suspend fun setBudget(yearMonth: java.time.YearMonth, categoryId: String?, limit: Money): String {
        val id = "b${nextId++}"
        store.update { m -> m.filterValues { !(it.yearMonth == yearMonth && it.categoryId == categoryId) } + (id to com.dwt.ledger.domain.model.Budget(id, categoryId, yearMonth, limit)) }
        return id
    }
    override suspend fun delete(id: String) = store.update { it - id }
}
