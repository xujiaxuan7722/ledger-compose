package com.dwt.ledger.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dwt.ledger.data.DefaultDataSeeder
import com.dwt.ledger.data.toLocal
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: TransactionDao

    @Before fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java)
            .allowMainThreadQueries().build()
        dao = db.transactionDao()
        db.categoryDao().upsertAll(DefaultDataSeeder.DEFAULT_CATEGORIES.map { it.toLocal() })
        db.accountDao().upsertAll(DefaultDataSeeder.DEFAULT_ACCOUNTS.map { it.toLocal() })
    }

    @After fun tearDown() = db.close()

    private fun tx(id: String, at: Long, created: Long = at) =
        LocalTransaction(id, "EXPENSE", 100, "cat_food", "acc_cash", at, "", created)

    @Test fun observeBetween_isHalfOpenAndNewestFirst() = runTest {
        dao.upsert(tx("before", 999))
        dao.upsert(tx("start", 1000))
        dao.upsert(tx("mid", 1500))
        dao.upsert(tx("end", 2000))
        val got = dao.observeBetween(1000, 2000).first().map { it.id }
        assertThat(got).containsExactly("mid", "start").inOrder()
    }

    @Test fun sameInstant_orderedByCreatedAtDesc() = runTest {
        dao.upsert(tx("first", at = 1000, created = 1))
        dao.upsert(tx("second", at = 1000, created = 2))
        assertThat(dao.observeBetween(0, 5000).first().map { it.id }).containsExactly("second", "first").inOrder()
    }

    @Test fun upsert_updatesExistingRow_andDeleteRemoves() = runTest {
        dao.upsert(tx("a", 1000))
        dao.upsert(tx("a", 1000).copy(amountCents = 999, note = "changed"))
        assertThat(dao.count()).isEqualTo(1)
        assertThat(dao.getById("a")!!.note).isEqualTo("changed")
        dao.deleteById("a")
        assertThat(dao.getById("a")).isNull()
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun insertWithUnknownCategory_isRejectedByForeignKey() = runTest {
        dao.upsert(tx("bad", 1000).copy(categoryId = "nope"))
    }

    @Test fun countByCategoryAndAccount() = runTest {
        dao.upsert(tx("a", 1000)); dao.upsert(tx("b", 1001)); dao.upsert(tx("c", 1002).copy(categoryId = "cat_transport", accountId = "acc_wechat"))
        assertThat(dao.countByCategory("cat_food")).isEqualTo(2)
        assertThat(dao.countByCategory("cat_transport")).isEqualTo(1)
        assertThat(dao.countByCategory("nope")).isEqualTo(0)
        assertThat(dao.countByAccount("acc_cash")).isEqualTo(2)
        assertThat(dao.countByAccount("acc_wechat")).isEqualTo(1)
    }
}
