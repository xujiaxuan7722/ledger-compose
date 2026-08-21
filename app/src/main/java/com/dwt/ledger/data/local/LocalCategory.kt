package com.dwt.ledger.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class LocalCategory(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val icon: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val builtin: Boolean,
)
