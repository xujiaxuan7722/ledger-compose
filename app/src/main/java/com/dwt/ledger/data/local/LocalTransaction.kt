package com.dwt.ledger.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = LocalCategory::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = LocalAccount::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("occurred_at"), Index("category_id"), Index("account_id")],
)
data class LocalTransaction(
    @PrimaryKey val id: String,
    val kind: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    @ColumnInfo(name = "occurred_at") val occurredAtMillis: Long,
    val note: String,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long,
)
