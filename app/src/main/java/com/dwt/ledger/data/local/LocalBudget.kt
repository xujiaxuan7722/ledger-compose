package com.dwt.ledger.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** category_id 为 null = 总预算；同一月份同一分类只能有一条 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["year_month", "category_id"], unique = true)],
)
data class LocalBudget(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    /** "yyyy-MM" */
    @ColumnInfo(name = "year_month") val yearMonth: String,
    @ColumnInfo(name = "limit_cents") val limitCents: Long,
)
