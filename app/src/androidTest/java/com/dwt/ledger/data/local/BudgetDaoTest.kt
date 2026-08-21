package com.dwt.ledger.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {
    private lateinit var db: LedgerDatabase
    private lateinit var dao: BudgetDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java).allowMainThreadQueries().build()
        dao = db.budgetDao()
    }
    @After fun tearDown() = db.close()

    @Test fun observeForMonth_filtersByMonth_andNullCategoryIsTotal() = runTest {
        dao.insert(LocalBudget("t", null, "2026-08", 100))
        dao.insert(LocalBudget("f", "cat_food", "2026-08", 50))
        dao.insert(LocalBudget("old", null, "2026-07", 1))
        val aug = dao.observeForMonth("2026-08").first()
        assertThat(aug.map { it.id }).containsExactly("t", "f")
        assertThat(aug.single { it.categoryId == null }.id).isEqualTo("t")
    }

    @Test fun deleteForMonthAndCategory_handlesNullCategory() = runTest {
        dao.insert(LocalBudget("t", null, "2026-08", 100))
        dao.insert(LocalBudget("f", "cat_food", "2026-08", 50))
        dao.deleteForMonthAndCategory("2026-08", null)
        assertThat(dao.observeForMonth("2026-08").first().map { it.id }).containsExactly("f")
        dao.deleteForMonthAndCategory("2026-08", "cat_food")
        assertThat(dao.observeForMonth("2026-08").first()).isEmpty()
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun sameMonthAndCategoryTwice_violatesUniqueIndex() = runTest {
        dao.insert(LocalBudget("a", "cat_food", "2026-08", 1))
        dao.insert(LocalBudget("b", "cat_food", "2026-08", 2))
    }
}
