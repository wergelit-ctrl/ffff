package com.finfocus.app.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.DefaultCategories
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TxType { EXPENSE, INCOME }

data class TransactionFormState(
    val type: TxType = TxType.EXPENSE,
    val selectedAccountId: String? = null,
    val amountText: String = "",
    val selectedCategoryId: String = "cat_food",
    val note: String = "",
    val amountError: Boolean = false,
    val accountError: Boolean = false,
)

sealed interface TransactionEvent {
    data object Saved : TransactionEvent
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Категории расходов из репозитория (пользователь может менять в настройках). */
    val expenseCategories: StateFlow<List<ExpenseCategory>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultCategories.list)

    private val _form = MutableStateFlow(TransactionFormState())
    val form: StateFlow<TransactionFormState> = _form.asStateFlow()

    private val _event = MutableStateFlow<TransactionEvent?>(null)
    val event: StateFlow<TransactionEvent?> = _event.asStateFlow()

    fun setType(type: TxType) {
        // При смене типа сбрасываем категорию на первую подходящую
        val firstCat = if (type == TxType.EXPENSE)
            expenseCategories.value.firstOrNull()?.id ?: "cat_food"
        else "cat_other"
        _form.value = _form.value.copy(type = type, selectedCategoryId = firstCat)
    }

    fun setAccount(accountId: String) {
        _form.value = _form.value.copy(selectedAccountId = accountId, accountError = false)
    }

    fun setAmount(text: String) {
        _form.value = _form.value.copy(amountText = text, amountError = false)
    }

    fun setCategory(categoryId: String) {
        _form.value = _form.value.copy(selectedCategoryId = categoryId)
    }

    fun setNote(note: String) {
        _form.value = _form.value.copy(note = note)
    }

    /** Вызывается из CategoryPickerDialog при нажатии Enter или кнопки Сохранить. */
    fun submitFromDialog(categoryId: String, amount: Long, note: String) {
        val state = _form.value
        if (state.selectedAccountId == null) {
            _form.value = state.copy(accountError = true)
            return
        }
        val categoryName = expenseCategories.value
            .firstOrNull { it.id == categoryId }?.name ?: "Другое"
        val txType = if (state.type == TxType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME

        viewModelScope.launch {
            addTransactionUseCase(
                accountId = state.selectedAccountId,
                amount = amount,
                category = categoryName,
                type = txType,
                note = note.ifBlank { null },
            )
            _event.value = TransactionEvent.Saved
        }
    }

    fun submit() {
        val state = _form.value
        val amount = state.amountText.replace(',', '.').toDoubleOrNull()
        val amountInvalid = amount == null || amount <= 0.0
        val accountInvalid = state.selectedAccountId == null
        if (amountInvalid || accountInvalid) {
            _form.value = state.copy(amountError = amountInvalid, accountError = accountInvalid)
            return
        }
        val categoryName = expenseCategories.value
            .firstOrNull { it.id == state.selectedCategoryId }?.name ?: "Другое"
        val txType = if (state.type == TxType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
        viewModelScope.launch {
            addTransactionUseCase(
                accountId = state.selectedAccountId!!,
                amount = (amount!! * 100).toLong(),
                category = categoryName,
                type = txType,
                note = state.note.ifBlank { null },
            )
            _event.value = TransactionEvent.Saved
        }
    }

    fun consumeEvent() { _event.value = null }

    fun initAccount(accounts: List<Account>, preferredId: String = "") {
        if (_form.value.selectedAccountId == null) {
            val target = if (preferredId.isNotBlank())
                accounts.firstOrNull { it.id == preferredId }
            else
                accounts.firstOrNull()
            if (target != null) _form.value = _form.value.copy(selectedAccountId = target.id)
        }
    }
}
