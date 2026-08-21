package com.dwt.ledger.data

import com.dwt.ledger.domain.logic.Csv
import com.dwt.ledger.domain.logic.CsvRow
import com.dwt.ledger.domain.model.TransactionKind
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

data class ImportSummary(val imported: Int, val errors: List<String>)

/** CSV 导入/导出的用例：把领域纯函数与仓库串起来 */
@Singleton
class CsvTransfer @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock,
) {
    suspend fun export(): String {
        val transactions = transactionRepository.observeAll().first()
        val categories = categoryRepository.observeAll().first().associate { it.id to it.name }
        val accounts = accountRepository.observeAll().first().associate { it.id to it.name }
        return Csv.export(transactions, categories, accounts, clock.zone)
    }

    /** 按名称匹配分类（同类型）/账户，缺失则创建；逐行写入。 */
    suspend fun import(text: String): ImportSummary {
        val parsed = Csv.parse(text)
        if (parsed.rows.isEmpty()) return ImportSummary(0, parsed.errors)
        val categoryIds = mutableMapOf<Pair<TransactionKind, String>, String>()
        categoryRepository.observeAll().first().forEach { categoryIds[it.kind to it.name] = it.id }
        val accountIds = mutableMapOf<String, String>()
        accountRepository.observeAll().first().forEach { accountIds[it.name] = it.id }

        var imported = 0
        parsed.rows.forEach { row -> importRow(row, categoryIds, accountIds); imported++ }
        return ImportSummary(imported, parsed.errors)
    }

    private suspend fun importRow(
        row: CsvRow,
        categoryIds: MutableMap<Pair<TransactionKind, String>, String>,
        accountIds: MutableMap<String, String>,
    ) {
        val categoryId = categoryIds.getOrPut(row.kind to row.categoryName) {
            categoryRepository.create(row.categoryName, row.kind, "more_horiz")
        }
        val accountId = accountIds.getOrPut(row.accountName) {
            accountRepository.create(row.accountName, "wallet")
        }
        val occurredAt = row.date.atTime(LocalTime.NOON).atZone(clock.zone).toInstant()
        transactionRepository.create(row.kind, row.amount, categoryId, accountId, occurredAt, row.note)
    }
}
