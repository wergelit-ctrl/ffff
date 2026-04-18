package com.finfocus.app.presentation.spend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.domain.model.DefaultCategories
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SpendEvent {
    /** Транзакция сохранена, можно переходить на чек. */
    data class Saved(val categoryName: String, val amountKopecks: Long) : SpendEvent
    data class Error(val message: String) : SpendEvent
}

@HiltViewModel
class SpendViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
) : ViewModel() {

    /** Категории расходов из репозитория. */
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultCategories.list)

    private val _event = MutableStateFlow<SpendEvent?>(null)
    val event = _event.asStateFlow()

    /**
     * Сохраняет расход и эмитирует SpendEvent.Saved.
     * @param accountId   id счёта списания
     * @param categoryName название категории
     * @param amountKopecks сумма в копейках (задача 4.2)
     * @param note заметка
     */
    fun saveExpense(
        accountId: String,
        categoryName: String,
        amountKopecks: Long,
        note: String = "",
    ) = viewModelScope.launch {
        if (accountId.isBlank()) {
            _event.value = SpendEvent.Error("Не выбран счёт")
            return@launch
        }
        addTransactionUseCase(
            accountId = accountId,
            amount    = amountKopecks,
            category  = categoryName,
            type      = TransactionType.EXPENSE,
            note      = note.ifBlank { null },
        )
        _event.value = SpendEvent.Saved(categoryName, amountKopecks)
    }

    fun consumeEvent() { _event.value = null }
}
