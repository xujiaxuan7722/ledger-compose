package com.dwt.ledger.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 验证 v1 → v2 自动迁移：旧数据保留，新表 budgets 可用 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LedgerDatabase::class.java)

    @Test fun migrate1To2_keepsTransactions_andAddsBudgets() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO categories (id, name, kind, icon, sort_order, builtin) VALUES ('c1','餐饮','EXPENSE','restaurant',1,1)")
            execSQL("INSERT INTO accounts (id, name, icon, sort_order, archived) VALUES ('a1','现金','wallet',1,0)")
            execSQL("INSERT INTO transactions (id, kind, amount_cents, category_id, account_id, occurred_at, note, created_at) VALUES ('t1','EXPENSE',1234,'c1','a1',1000,'x',1000)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 2, true)
        db.query("SELECT amount_cents FROM transactions WHERE id = 't1'").use { c ->
            assertThat(c.moveToFirst()).isTrue(); assertThat(c.getLong(0)).isEqualTo(1234)
        }
        db.execSQL("INSERT INTO budgets (id, category_id, year_month, limit_cents) VALUES ('b1', NULL, '2026-08', 500)")
        db.query("SELECT COUNT(*) FROM budgets").use { c -> c.moveToFirst(); assertThat(c.getInt(0)).isEqualTo(1) }
        db.close()
    }
}
