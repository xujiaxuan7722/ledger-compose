package com.dwt.ledger.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalTransaction::class, LocalCategory::class, LocalAccount::class],
    version = 1,
    exportSchema = true,
)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
}
