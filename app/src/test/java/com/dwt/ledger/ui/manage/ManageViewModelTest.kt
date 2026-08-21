package com.dwt.ledger.ui.manage

import com.dwt.ledger.data.FakeAccountRepository
import com.dwt.ledger.data.FakeCategoryRepository
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.util.MainCoroutineRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManageViewModelTest {
    @get:Rule val mainRule = MainCoroutineRule()
    private lateinit var categories: FakeCategoryRepository
    private lateinit var accounts: FakeAccountRepository
    private lateinit var viewModel: ManageViewModel

    @Before fun setUp() {
        categories = FakeCategoryRepository(); accounts = FakeAccountRepository()
        viewModel = ManageViewModel(categories, accounts)
    }
    private fun runWith(block: suspend () -> Unit) = runTest(mainRule.dispatcher) { val j = launch { viewModel.uiState.collect {} }; block(); j.cancel() }

    @Test fun `categories are split by kind`() = runWith {
        val s = viewModel.uiState.value
        assertThat(s.expenseCategories.map { it.kind }.toSet()).containsExactly(TransactionKind.EXPENSE)
        assertThat(s.incomeCategories.map { it.kind }.toSet()).containsExactly(TransactionKind.INCOME)
        assertThat(s.accounts).hasSize(4)
    }

    @Test fun `create category requires name and appends to its kind`() = runWith {
        viewModel.openCreate()
        viewModel.saveEditor()
        assertThat(viewModel.uiState.value.editor?.nameError).isTrue()
        viewModel.editorSetName(" 宠物 "); viewModel.editorSetIcon("home"); viewModel.editorSetKind(TransactionKind.EXPENSE)
        viewModel.saveEditor()
        assertThat(viewModel.uiState.value.editor).isNull()
        val created = viewModel.uiState.value.expenseCategories.last()
        assertThat(created.name).isEqualTo("宠物"); assertThat(created.icon).isEqualTo("home")
    }

    @Test fun `rename category keeps id and kind`() = runWith {
        val food = viewModel.uiState.value.expenseCategories.first { it.id == "cat_food" }
        viewModel.openEdit(food); viewModel.editorSetName("吃饭"); viewModel.saveEditor()
        val renamed = viewModel.uiState.value.expenseCategories.first { it.id == "cat_food" }
        assertThat(renamed.name).isEqualTo("吃饭"); assertThat(renamed.kind).isEqualTo(TransactionKind.EXPENSE)
    }

    @Test fun `delete category in use shows message and keeps it`() = runWith {
        categories.usage["cat_food"] = 3
        viewModel.openEdit(viewModel.uiState.value.expenseCategories.first { it.id == "cat_food" })
        viewModel.deleteEditing()
        assertThat(viewModel.uiState.value.message).isEqualTo(UiMessage.InUse(3))
        assertThat(viewModel.uiState.value.editor).isNotNull() // 弹窗仍打开
        assertThat(viewModel.uiState.value.expenseCategories.any { it.id == "cat_food" }).isTrue()
        viewModel.consumeMessage()
        assertThat(viewModel.uiState.value.message).isNull()
    }

    @Test fun `delete unused category removes it and closes editor`() = runWith {
        viewModel.openEdit(viewModel.uiState.value.expenseCategories.first { it.id == "cat_fun" })
        viewModel.deleteEditing()
        assertThat(viewModel.uiState.value.editor).isNull()
        assertThat(viewModel.uiState.value.expenseCategories.any { it.id == "cat_fun" }).isFalse()
    }

    @Test fun `archive account hides it from active list but keeps it in manage list`() = runWith {
        viewModel.selectTab(ManageTab.ACCOUNTS)
        viewModel.openEdit(viewModel.uiState.value.accounts.first { it.id == "acc_cash" })
        viewModel.toggleArchiveEditing()
        assertThat(viewModel.uiState.value.accounts.first { it.id == "acc_cash" }.archived).isTrue()
        assertThat(viewModel.uiState.value.accounts.last().id).isEqualTo("acc_cash") // 归档的排最后
        assertThat(accounts.observeActive().first().map { it.id }).doesNotContain("acc_cash")
    }

    @Test fun `create account`() = runWith {
        viewModel.selectTab(ManageTab.ACCOUNTS); viewModel.openCreate()
        viewModel.editorSetName("信用卡"); viewModel.editorSetIcon("credit_card"); viewModel.saveEditor()
        assertThat(viewModel.uiState.value.accounts.map { it.name }).contains("信用卡")
    }
}
