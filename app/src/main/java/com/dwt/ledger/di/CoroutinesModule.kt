package com.dwt.ledger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton @ApplicationScope
    fun provideApplicationScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + io)
}

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    fun provideClock(): java.time.Clock = java.time.Clock.systemDefaultZone()
}
