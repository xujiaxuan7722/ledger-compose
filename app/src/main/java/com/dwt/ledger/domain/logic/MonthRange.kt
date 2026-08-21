package com.dwt.ledger.domain.logic

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** 某个月在给定时区下的 [start, end) 瞬时区间 */
data class MonthRange(val start: Instant, val endExclusive: Instant)

fun YearMonth.toRange(zone: ZoneId): MonthRange = MonthRange(
    start = atDay(1).atStartOfDay(zone).toInstant(),
    endExclusive = plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(),
)
