package com.dwt.ledger.data

import com.dwt.ledger.data.local.LocalAccount
import com.dwt.ledger.data.local.LocalBudget
import com.dwt.ledger.data.local.LocalCategory
import com.dwt.ledger.data.local.LocalTransaction
import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Budget
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import java.time.Instant
import java.time.YearMonth

// 领域模型 <-> Room 实体。Room 注解不进入领域层。

fun LocalTransaction.toDomain() = Transaction(
    id = id,
    kind = TransactionKind.valueOf(kind),
    amount = Money(amountCents),
    categoryId = categoryId,
    accountId = accountId,
    occurredAt = Instant.ofEpochMilli(occurredAtMillis),
    note = note,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun Transaction.toLocal() = LocalTransaction(
    id = id,
    kind = kind.name,
    amountCents = amount.cents,
    categoryId = categoryId,
    accountId = accountId,
    occurredAtMillis = occurredAt.toEpochMilli(),
    note = note,
    createdAtMillis = createdAt.toEpochMilli(),
)

fun LocalCategory.toDomain() = Category(id, name, TransactionKind.valueOf(kind), icon, sortOrder, builtin)
fun Category.toLocal() = LocalCategory(id, name, kind.name, icon, sortOrder, builtin)

fun LocalAccount.toDomain() = Account(id, name, icon, sortOrder, archived)
fun Account.toLocal() = LocalAccount(id, name, icon, sortOrder, archived)

@JvmName("localTransactionsToDomain")
fun List<LocalTransaction>.toDomain() = map(LocalTransaction::toDomain)
@JvmName("localCategoriesToDomain")
fun List<LocalCategory>.toDomain() = map(LocalCategory::toDomain)
@JvmName("localAccountsToDomain")
fun List<LocalAccount>.toDomain() = map(LocalAccount::toDomain)

fun LocalBudget.toDomain() = Budget(id, categoryId, YearMonth.parse(yearMonth), Money(limitCents))
fun Budget.toLocal() = LocalBudget(id, categoryId, yearMonth.toString(), limit.cents)
@JvmName("localBudgetsToDomain")
fun List<LocalBudget>.toDomain() = map(LocalBudget::toDomain)
