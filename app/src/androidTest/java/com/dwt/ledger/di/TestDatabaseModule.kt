package com.dwt.ledger.di

import android.content.Context
import androidx.room.Room
import com.dwt.ledger.data.local.AccountDao
import com.dwt.ledger.data.local.BudgetDao
import com.dwt.ledger.data.local.CategoryDao
import com.dwt.ledger.data.local.LedgerDatabase
import com.dwt.ledger.data.local.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** UI 测试用内存数据库替换真实数据库，每个测试进程独立、不落盘 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase =
        Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).allowMainThreadQueries().build()

    @Provides fun provideTransactionDao(db: LedgerDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: LedgerDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideAccountDao(db: LedgerDatabase): AccountDao = db.accountDao()
    @Provides fun provideBudgetDao(db: LedgerDatabase): BudgetDao = db.budgetDao()
}
