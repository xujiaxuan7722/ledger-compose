package com.dwt.ledger.domain.logic

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** CSV 一行对应的业务数据（与存储 id 无关，便于跨设备迁移） */
data class CsvRow(
    val date: LocalDate,
    val kind: TransactionKind,
    val amount: Money,
    val categoryName: String,
    val accountName: String,
    val note: String,
)

data class CsvParseResult(val rows: List<CsvRow>, val errors: List<String>)

object Csv {
    const val HEADER = "日期,类型,金额,分类,账户,备注"
    private val DATE = DateTimeFormatter.ISO_LOCAL_DATE
    private const val INCOME = "收入"
    private const val EXPENSE = "支出"

    /** 导出：UTF-8 BOM + 表头 + 每行一条，金额为元（两位小数） */
    fun export(
        transactions: List<Transaction>,
        categoryNameById: Map<String, String>,
        accountNameById: Map<String, String>,
        zone: ZoneId,
    ): String = buildString {
        append('\uFEFF').append(HEADER).append('\n')
        transactions.forEach { t ->
            val fields = listOf(
                t.occurredAt.atZone(zone).toLocalDate().format(DATE),
                if (t.kind == TransactionKind.INCOME) INCOME else EXPENSE,
                t.amount.format().replace(",", ""),
                categoryNameById[t.categoryId] ?: "",
                accountNameById[t.accountId] ?: "",
                t.note,
            )
            append(fields.joinToString(",") { escape(it) }).append('\n')
        }
    }

    /** 解析：容忍 BOM、CRLF、带引号字段；逐行校验，坏行进 errors，不影响好行 */
    fun parse(text: String): CsvParseResult {
        val lines = text.removePrefix("\uFEFF").split("\r\n", "\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return CsvParseResult(emptyList(), listOf("文件为空"))
        val rows = mutableListOf<CsvRow>(); val errors = mutableListOf<String>()
        val body = if (lines.first().trim() == HEADER) lines.drop(1) else lines
        body.forEachIndexed { i, line ->
            val n = i + 2 // 人类可读行号（含表头）
            val f = splitLine(line)
            if (f.size < 5) { errors += "第 $n 行：列数不足"; return@forEachIndexed }
            val date = try { LocalDate.parse(f[0].trim(), DATE) } catch (e: DateTimeParseException) { errors += "第 $n 行：日期格式应为 yyyy-MM-dd"; return@forEachIndexed }
            val kind = when (f[1].trim()) { INCOME -> TransactionKind.INCOME; EXPENSE -> TransactionKind.EXPENSE; else -> { errors += "第 $n 行：类型应为 收入/支出"; return@forEachIndexed } }
            val amount = Money.parse(f[2].replace(",", ""))
            if (amount == null || !amount.isPositive) { errors += "第 $n 行：金额无效"; return@forEachIndexed }
            val category = f[3].trim(); val account = f[4].trim()
            if (category.isEmpty() || account.isEmpty()) { errors += "第 $n 行：分类/账户不能为空"; return@forEachIndexed }
            rows += CsvRow(date, kind, amount, category, account, f.getOrElse(5) { "" }.trim())
        }
        return CsvParseResult(rows, errors)
    }

    internal fun escape(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    /** 简单 CSV 分词：支持双引号包裹与 "" 转义 */
    internal fun splitLine(line: String): List<String> {
        val out = mutableListOf<String>(); val cur = StringBuilder(); var inQuotes = false; var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { out += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
            i++
        }
        out += cur.toString()
        return out
    }
}
