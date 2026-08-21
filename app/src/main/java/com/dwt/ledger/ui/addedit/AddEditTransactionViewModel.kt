package com.dwt.ledger.ui.addedit

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dwt.ledger.R
import com.dwt.ledger.data.AccountRepository
import com.dwt.ledger.data.CategoryRepository
import com.dwt.ledger.data.TransactionRepository
import com.dwt.ledger.domain.model.Account
import com.dwt.ledger.domain.model.Category
import com.dwt.ledger.domain.model.Money
import com.dwt.ledger.domain.model.TransactionKind
import com.dwt.ledger.ui.navigation.LedgerRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class AddEditUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val amountText: String = "",
    val categoryId: String? = null,
    val accountId: String? = null,
    val date: LocalDate,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    @StringRes val errorMessage: Int? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
) {
    /** 只显示当前收/支类型下的分类 */
    val visibleCategories: List<Category> get() = categories.filter { it.kind == kind }
}

private data class FormState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val amountText: String = "",
    val categoryId: String? = null,
    val accountId: String? = null,
    val date: LocalDate,
    val note: String = "",
    val errorMessage: Int? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val transactionId: String? = savedStateHandle[LedgerRoutes.ARG_TRANSACTION_ID]
    private val initialKind: TransactionKind =
        savedStateHandle.get<String>(LedgerRoutes.ARG_KIND)?.let { runCatching { TransactionKind.valueOf(it) }.getOrNull() } ?: TransactionKind.EXPENSE
    private val form = MutableStateFlow(FormState(date = LocalDate.now(clock), kind = initialKind, isEditing = transactionId != null, isLoading = transactionId != null))

    val uiState: StateFlow<AddEditUiState> = combine(
        form, categoryRepository.observeAll(), accountRepository.observeActive(),
    ) { f, categories, accounts ->
        AddEditUiState(
            isEditing = f.isEditing, isLoading = f.isLoading, kind = f.kind, amountText = f.amountText,
            categoryId = f.categoryId, accountId = f.accountId, date = f.date, note = f.note,
            categories = categories, accounts = accounts,
            errorMessage = f.errorMessage, isSaved = f.isSaved, isDeleted = f.isDeleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEditUiState(date = form.value.date, isEditing = form.value.isEditing, isLoading = form.value.isLoading))

    init {
        if (transactionId != null) loadExisting(transactionId)
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val t = transactionRepository.getTransaction(id)
            form.update { f ->
                if (t == null) f.copy(isLoading = false, isDeleted = true)
                else f.copy(
                    isLoading = false,
                    kind = t.kind,
                    amountText = t.amount.format().replace(",", ""),
                    categoryId = t.categoryId,
                    accountId = t.accountId,
                    date = t.occurredAt.atZone(clock.zone).toLocalDate(),
                    note = t.note,
                )
            }
        }
    }

    fun setKind(kind: TransactionKind) = form.update {
        // 切换收/支时清掉不属于新类型的分类选择
        it.copy(kind = kind, categoryId = null)
    }
    fun setAmount(text: String) = form.update { it.copy(amountText = text) }
    fun selectCategory(id: String) = form.update { it.copy(categoryId = id) }
    fun selectAccount(id: String) = form.update { it.copy(accountId = id) }
    fun setDate(date: LocalDate) = form.update { it.copy(date = date) }
    fun setNote(note: String) = form.update { it.copy(note = note) }
    fun consumeError() = form.update { it.copy(errorMessage = null) }

    fun save() {
        val f = form.value
        val amount = Money.parse(f.amountText)
        val error = when {
            amount == null || !amount.isPositive -> R.string.error_amount_invalid
            f.categoryId == null -> R.string.error_category_required
            f.accountId == null -> R.string.error_account_required
            else -> null
        }
        if (error != null) { form.update { it.copy(errorMessage = error) }; return }
        requireNotNull(amount); requireNotNull(f.categoryId); requireNotNull(f.accountId)

        // 同一天内用「当前时间」作为时刻，这样今天记的多笔能按先后排序；改日期则用当天中午，避开时区边界
        val occurredAt = if (f.date == LocalDate.now(clock)) clock.instant()
        else f.date.atTime(LocalTime.NOON).atZone(clock.zone).toInstant()

        viewModelScope.launch {
            if (transactionId == null) {
                transactionRepository.create(f.kind, amount, f.categoryId, f.accountId, occurredAt, f.note.trim())
            } else {
                val existing = transactionRepository.getTransaction(transactionId)
                if (existing != null) {
                    val keepTime = existing.occurredAt.atZone(clock.zone).toLocalDate() == f.date
                    transactionRepository.update(
                        existing.copy(
                            kind = f.kind, amount = amount, categoryId = f.categoryId, accountId = f.accountId,
                            occurredAt = if (keepTime) existing.occurredAt else occurredAt, note = f.note.trim(),
                        )
                    )
                }
            }
            form.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        val id = transactionId ?: return
        viewModelScope.launch {
            transactionRepository.delete(id)
            form.update { it.copy(isDeleted = true) }
        }
    }
}
