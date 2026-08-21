package com.dwt.ledger

import android.app.Application
import com.dwt.ledger.data.DefaultDataSeeder
import com.dwt.ledger.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LedgerApplication : Application() {
    @Inject lateinit var seeder: DefaultDataSeeder
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seeder.seedIfEmpty() }
    }
}
