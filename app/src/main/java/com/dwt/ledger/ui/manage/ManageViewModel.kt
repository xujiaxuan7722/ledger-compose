package com.dwt.ledger.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.data.AccountRepository
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.InUseException
import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.TransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ManageTab { CATEGORIES, ACCOUNTS }

/** 新建/编辑弹窗：[id] 为 null 表示新建；[kind] 仅分类有意义 */
data class ItemEditor(
    val tab: ManageTab,
    val id: String? = null,
    val name: String = "",
    val icon: String = "more_horiz",
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val archived: Boolean = false,
    val nameError: Boolean = false,
)

data class ManageUiState(
    val tab: ManageTab = ManageTab.CATEGORIES,
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val editor: ItemEditor? = null,
    /** 一次性提示（如"有 3 笔记录，无法删除"），消费后置空 */
    val message: UiMessage? = null,
)

sealed interface UiMessage {
    data class InUse(val count: Int) : UiMessage
}

@HiltViewModel
class ManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val tab = MutableStateFlow(ManageTab.CATEGORIES)
    private val editor = MutableStateFlow<ItemEditor?>(null)
    private val message = MutableStateFlow<UiMessage?>(null)

    val uiState: StateFlow<ManageUiState> = combine(
        tab, categoryRepository.observeAll(), accountRepository.observeAll(), editor, message,
    ) { t, categories, accounts, ed, msg ->
        ManageUiState(
            tab = t,
            expenseCategories = categories.filter { it.kind == TransactionKind.EXPENSE },
            incomeCategories = categories.filter { it.kind == TransactionKind.INCOME },
            accounts = accounts,
            editor = ed,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageUiState())

    fun selectTab(value: ManageTab) = tab.update { value }
    fun consumeMessage() = message.update { null }

    fun openCreate() = editor.update { ItemEditor(tab = tab.value) }
    fun openEdit(category: Category) = editor.update { ItemEditor(ManageTab.CATEGORIES, category.id, category.name, category.icon, category.kind) }
    fun openEdit(account: Account) = editor.update { ItemEditor(ManageTab.ACCOUNTS, account.id, account.name, account.icon, archived = account.archived) }
    fun closeEditor() = editor.update { null }
    fun editorSetName(v: String) = editor.update { it?.copy(name = v, nameError = false) }
    fun editorSetIcon(v: String) = editor.update { it?.copy(icon = v) }
    fun editorSetKind(v: TransactionKind) = editor.update { it?.copy(kind = v) }

    fun saveEditor() {
        val ed = editor.value ?: return
        if (ed.name.isBlank()) { editor.update { it?.copy(nameError = true) }; return }
        viewModelScope.launch {
            when (ed.tab) {
                ManageTab.CATEGORIES ->
                    if (ed.id == null) categoryRepository.create(ed.name, ed.kind, ed.icon) else categoryRepository.rename(ed.id, ed.name, ed.icon)
                ManageTab.ACCOUNTS ->
                    if (ed.id == null) accountRepository.create(ed.name, ed.icon) else accountRepository.rename(ed.id, ed.name, ed.icon)
            }
            editor.update { null }
        }
    }

    fun deleteEditing() {
        val ed = editor.value ?: return
        val id = ed.id ?: return
        viewModelScope.launch {
            try {
                when (ed.tab) {
                    ManageTab.CATEGORIES -> categoryRepository.delete(id)
                    ManageTab.ACCOUNTS -> accountRepository.delete(id)
                }
                editor.update { null }
            } catch (e: InUseException) {
                message.update { UiMessage.InUse(e.usageCount) }
            }
        }
    }

    fun toggleArchiveEditing() {
        val ed = editor.value ?: return
        val id = ed.id ?: return
        if (ed.tab != ManageTab.ACCOUNTS) return
        viewModelScope.launch {
            accountRepository.setArchived(id, !ed.archived)
            editor.update { null }
        }
    }
}
