package com.dwt.ledger.ui.addedit

import androidx.lifecycle.SavedStateHandle
import com.dwt.ledger.R
import com.dwt.ledger.data.FakeAccountRepository
import com.dwt.ledger.data.FakeCategoryRepository
import com.dwt.ledger.data.FakeTransactionRepository
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.Transaction
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.navigation.LedgerRoutes
import com.dwt.ledger.util.MainCoroutineRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditTransactionViewModelTest {
    @get:Rule val mainRule = MainCoroutineRule()

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-08-21T06:00:00Z")
    private val clock = Clock.fixed(now, zone)
    private val transactions = FakeTransactionRepository()

    private fun vm(transactionId: String? = null) = AddEditTransactionViewModel(
        transactions, FakeCategoryRepository(), FakeAccountRepository(), clock,
        SavedStateHandle(if (transactionId == null) emptyMap() else mapOf(LedgerRoutes.ARG_TRANSACTION_ID to transactionId)),
    )

    private fun runWith(viewModel: AddEditTransactionViewModel, block: suspend () -> Unit) = runTest(mainRule.dispatcher) {
        val job = launch { viewModel.uiState.collect {} }
        block()
        job.cancel()
    }

    @Test fun `defaults to expense today with categories filtered by kind`() {
        val viewModel = vm()
        runWith(viewModel) {
            val s = viewModel.uiState.value
            assertThat(s.kind).isEqualTo(TransactionKind.EXPENSE)
            assertThat(s.date).isEqualTo(LocalDate.of(2026, 8, 21))
            assertThat(s.visibleCategories.map { it.kind }.toSet()).containsExactly(TransactionKind.EXPENSE)
            assertThat(s.isEditing).isFalse()
        }
    }

    @Test fun `save validates amount, category, account in order`() {
        val viewModel = vm()
        runWith(viewModel) {
            viewModel.save()
            assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_amount_invalid)
            viewModel.consumeError()

            viewModel.setAmount("0")
            viewModel.save()
            assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_amount_invalid)
            viewModel.consumeError()

            viewModel.setAmount("12.5")
            viewModel.save()
            assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_category_required)
            viewModel.consumeError()

            viewModel.selectCategory("cat_food")
            viewModel.save()
            assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_account_required)
            assertThat(transactions.all).isEmpty()
        }
    }

    @Test fun `save creates a transaction with current instant when date is today`() {
        val viewModel = vm()
        runWith(viewModel) {
            viewModel.setAmount("12.5")
            viewModel.selectCategory("cat_food")
            viewModel.selectAccount("acc_wechat")
            viewModel.setNote("  午饭 ")
            viewModel.save()
            assertThat(viewModel.uiState.value.isSaved).isTrue()
            val t = transactions.all.single()
            assertThat(t.amount).isEqualTo(Money(1250))
            assertThat(t.kind).isEqualTo(TransactionKind.EXPENSE)
            assertThat(t.categoryId).isEqualTo("cat_food")
            assertThat(t.accountId).isEqualTo("acc_wechat")
            assertThat(t.note).isEqualTo("午饭")
            assertThat(t.occurredAt).isEqualTo(now)
        }
    }

    @Test fun `switching kind clears category selection`() {
        val viewModel = vm()
        runWith(viewModel) {
            viewModel.selectCategory("cat_food")
            viewModel.setKind(TransactionKind.INCOME)
            val s = viewModel.uiState.value
            assertThat(s.categoryId).isNull()
            assertThat(s.visibleCategories.map { it.kind }.toSet()).containsExactly(TransactionKind.INCOME)
        }
    }

    @Test fun `edit mode loads existing and update keeps time when date unchanged`() {
        val occurred = LocalDate.of(2026, 8, 3).atTime(8, 30).atZone(zone).toInstant()
        transactions.seed(Transaction("t1", TransactionKind.INCOME, Money(500000), "cat_salary", "acc_bank", occurred, "工资"))
        val viewModel = vm("t1")
        runWith(viewModel) {
            val s = viewModel.uiState.value
            assertThat(s.isEditing).isTrue()
            assertThat(s.amountText).isEqualTo("5000.00")
            assertThat(s.kind).isEqualTo(TransactionKind.INCOME)
            assertThat(s.date).isEqualTo(LocalDate.of(2026, 8, 3))

            viewModel.setAmount("5200")
            viewModel.save()
            val updated = transactions.all.single()
            assertThat(updated.amount).isEqualTo(Money(520000))
            assertThat(updated.occurredAt).isEqualTo(occurred) // 日期没改，保留原时刻
            assertThat(updated.createdAt).isEqualTo(occurred)
        }
    }

    @Test fun `delete removes and signals done`() {
        transactions.seed(Transaction("t1", TransactionKind.EXPENSE, Money(100), "cat_food", "acc_cash", now))
        val viewModel = vm("t1")
        runWith(viewModel) {
            viewModel.delete()
            assertThat(transactions.all).isEmpty()
            assertThat(viewModel.uiState.value.isDeleted).isTrue()
        }
    }
}
