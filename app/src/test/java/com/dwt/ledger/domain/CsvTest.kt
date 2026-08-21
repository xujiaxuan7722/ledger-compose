package com.dwt.ledger.domain

import com.dwt.ledger.domain.logic.Csv
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CsvTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val tx = listOf(
        Transaction("1", TransactionKind.EXPENSE, Money(1250), "c1", "a1", LocalDate.of(2026, 8, 21).atTime(9, 0).atZone(zone).toInstant(), "午饭, 面条"),
        Transaction("2", TransactionKind.INCOME, Money(500000), "c2", "a2", LocalDate.of(2026, 8, 1).atTime(9, 0).atZone(zone).toInstant(), "说\"谢谢\""),
    )
    private val names = mapOf("c1" to "餐饮", "c2" to "工资"); private val accounts = mapOf("a1" to "现金", "a2" to "银行卡")

    @Test fun `export writes BOM header and escapes commas and quotes`() {
        val csv = Csv.export(tx, names, accounts, zone)
        val lines = csv.split("\n")
        assertThat(lines[0]).isEqualTo("\uFEFF" + Csv.HEADER)
        assertThat(lines[1]).isEqualTo("2026-08-21,支出,12.50,餐饮,现金,\"午饭, 面条\"")
        assertThat(lines[2]).isEqualTo("2026-08-01,收入,5000.00,工资,银行卡,\"说\"\"谢谢\"\"\"")
    }

    @Test fun `parse round-trips export output`() {
        val r = Csv.parse(Csv.export(tx, names, accounts, zone))
        assertThat(r.errors).isEmpty()
        assertThat(r.rows).hasSize(2)
        assertThat(r.rows[0].note).isEqualTo("午饭, 面条"); assertThat(r.rows[0].amount).isEqualTo(Money(1250)); assertThat(r.rows[0].date).isEqualTo(LocalDate.of(2026, 8, 21))
        assertThat(r.rows[1].note).isEqualTo("说\"谢谢\""); assertThat(r.rows[1].kind).isEqualTo(TransactionKind.INCOME)
    }

    @Test fun `parse tolerates CRLF missing header and thousands separators`() {
        val r = Csv.parse("2026-01-02,支出,\"1,234.5\",购物,微信\r\n2026-01-03,收入,10,奖金,现金,年终\r\n")
        assertThat(r.errors).isEmpty()
        assertThat(r.rows.map { it.amount }).containsExactly(Money(123450), Money(1000)).inOrder()
        assertThat(r.rows[0].note).isEmpty(); assertThat(r.rows[1].note).isEqualTo("年终")
    }

    @Test fun `bad rows are reported individually and good rows kept`() {
        val r = Csv.parse(Csv.HEADER + "\n2026-13-01,支出,1,a,b\n2026-01-01,转账,1,a,b\n2026-01-01,支出,0,a,b\n2026-01-01,支出,1,,b\n2026-01-01,支出,1,a,b,ok\n")
        assertThat(r.rows).hasSize(1)
        assertThat(r.errors).hasSize(4)
        assertThat(r.errors[0]).contains("第 2 行"); assertThat(r.errors[1]).contains("收入/支出"); assertThat(r.errors[2]).contains("金额"); assertThat(r.errors[3]).contains("分类/账户")
    }

    @Test fun `empty file`() { assertThat(Csv.parse("\uFEFF\n").errors).containsExactly("文件为空") }
}
