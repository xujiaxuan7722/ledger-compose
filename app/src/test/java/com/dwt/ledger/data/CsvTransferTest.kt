package com.dwt.ledger.data

import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CsvTransferTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-21T06:00:00Z"), ZoneId.of("Asia/Shanghai"))
    private val tx = FakeTransactionRepository(); private val cats = FakeCategoryRepository(); private val accs = FakeAccountRepository()
    private val transfer = CsvTransfer(tx, cats, accs, clock)

    @Test fun `import matches existing names, creates missing ones, and reports bad rows`() = runTest {
        val summary = transfer.import(
            "日期,类型,金额,分类,账户,备注\n" +
                "2026-08-01,支出,12.5,餐饮,现金,早饭\n" +          // 已有分类/账户
                "2026-08-02,支出,30,宠物,现金,猫粮\n" +           // 新分类
                "2026-08-03,收入,100,餐饮,新账户,\n" +            // 收入类型下没有"餐饮" → 新建一个收入分类；新账户
                "bad line\n"
        )
        assertThat(summary.imported).isEqualTo(3)
        assertThat(summary.errors).hasSize(1)
        assertThat(tx.all).hasSize(3)
        val categories = cats.observeAll().first()
        assertThat(categories.filter { it.name == "宠物" }.single().kind).isEqualTo(TransactionKind.EXPENSE)
        assertThat(categories.filter { it.name == "餐饮" }.map { it.kind }).containsExactly(TransactionKind.EXPENSE, TransactionKind.INCOME)
        assertThat(accs.observeAll().first().map { it.name }).contains("新账户")
        assertThat(tx.all.first { it.note == "早饭" }.categoryId).isEqualTo("cat_food")
        assertThat(tx.all.first { it.note == "早饭" }.amount).isEqualTo(Money(1250))
    }

    @Test fun `export then import yields same count`() = runTest {
        tx.create(TransactionKind.EXPENSE, Money(100), "cat_food", "acc_cash", Instant.parse("2026-08-10T04:00:00Z"), "a")
        tx.create(TransactionKind.INCOME, Money(200), "cat_salary", "acc_bank", Instant.parse("2026-08-11T04:00:00Z"), "b,c")
        val csv = transfer.export()
        assertThat(csv.lines().filter { it.isNotBlank() }).hasSize(3)
        val summary = transfer.import(csv)
        assertThat(summary.imported).isEqualTo(2); assertThat(summary.errors).isEmpty()
        assertThat(tx.all).hasSize(4)
    }
}
