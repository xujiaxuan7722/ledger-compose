package com.dwt.ledger.di

import android.content.Context
import androidx.room.Room
import com.dwt.ledger.data.AccountRepository
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.DefaultAccountRepository
import com.dwt.ledger.data.DefaultCategoryRepository
import com.dwt.ledger.data.DefaultTransactionRepository
import com.dwt.ledger.data.TransactionRepository
import com.dwt.ledger.data.local.AccountDao
import com.dwt.ledger.data.local.CategoryDao
import com.dwt.ledger.data.local.LedgerDatabase
import com.dwt.ledger.data.local.TransactionDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindTransactionRepository(impl: DefaultTransactionRepository): TransactionRepository

    @Binds @Singleton
    abstract fun bindCategoryRepository(impl: DefaultCategoryRepository): CategoryRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: DefaultAccountRepository): AccountRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase =
        Room.databaseBuilder(context, LedgerDatabase::class.java, "ledger.db").build()

    @Provides fun provideTransactionDao(db: LedgerDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: LedgerDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideAccountDao(db: LedgerDatabase): AccountDao = db.accountDao()
}
