package com.dwt.ledger.data

import com.dwt.ledger.data.local.AccountDao
import com.dwt.ledger.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAccountRepository @Inject constructor(
    private val dao: AccountDao,
) : AccountRepository {
    override fun observeActive(): Flow<List<Account>> = dao.observeActive().map { it.toDomain() }
}
