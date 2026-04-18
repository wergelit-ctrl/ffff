package com.finfocus.app.presentation.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DepositEvent {
    data class Saved(val categoryName: String, val amountKopecks: Long) : DepositEvent
    data class Error(val message: String) : DepositEvent
}

@HiltViewModel
class DepositViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
) : ViewModel() {

    private val _event = MutableStateFlow<DepositEvent?>(null)
    val event = _event.asStateFlow()

    fun saveDeposit(
        accountId: String,
        categoryName: String,
        amountKopecks: Long,
        note: String = "",
    ) = viewModelScope.launch {
        if (accountId.isBlank()) {
            _event.value = DepositEvent.Error("Не выбран счёт")
            return@launch
        }
        addTransactionUseCase(
            accountId = accountId,
            amount    = amountKopecks,
            category  = categoryName,
            type      = TransactionType.INCOME,
            note      = note.ifBlank { null },
        )
        _event.value = DepositEvent.Saved(categoryName, amountKopecks)
    }

    fun consumeEvent() { _event.value = null }
}
