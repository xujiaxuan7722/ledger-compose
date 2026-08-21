package com.dwt.ledger.ui.common

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.TransactionKind
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_FMT = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val DAY_FMT = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)
private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

fun YearMonth.display(): String = format(MONTH_FMT)
fun LocalDate.displayDay(): String = format(DAY_FMT)
fun LocalDate.displayDate(): String = format(DATE_FMT)

/** "+¥1,234.00" / "-¥56.50" */
fun Money.displaySigned(kind: TransactionKind): String =
    (if (kind == TransactionKind.INCOME) "+¥" else "-¥") + format()

/** "¥1,234.00" / 负数 "-¥28.50"（符号在货币符号前） */
fun Money.displayYuan(): String =
    if (cents < 0) "-¥" + Money(-cents).format() else "¥" + format()
