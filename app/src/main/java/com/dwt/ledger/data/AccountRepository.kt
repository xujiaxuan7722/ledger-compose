package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeActive(): Flow<List<Account>>
}
