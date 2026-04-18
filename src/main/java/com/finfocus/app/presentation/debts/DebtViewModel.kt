package com.finfocus.app.presentation.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import com.finfocus.app.domain.usecase.debts.CreateDebtUseCase
import com.finfocus.app.domain.usecase.debts.SettleDebtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    private val createDebtUseCase: CreateDebtUseCase,
    private val settleDebtUseCase: SettleDebtUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
) : ViewModel() {

    private val sub = SharingStarted.WhileSubscribed(5_000)

    private val debts = repository.observeDebts()
        .stateIn(viewModelScope, sub, emptyList())

    val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, sub, emptyList())

    val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, sub, com.finfocus.app.domain.model.AppSettings())

    val debtsOwedToMe = debts.map { it.filter { d -> d.type == DebtType.OWED_TO_ME } }
        .stateIn(viewModelScope, sub, emptyList())

    val debtsIOwe = debts.map { it.filter { d -> d.type == DebtType.I_OWE } }
        .stateIn(viewModelScope, sub, emptyList())

    val totalOwedToMe = debtsOwedToMe.map { it.sumOf { d -> d.amount - d.paidAmount } }
        .stateIn(viewModelScope, sub, 0L)

    val totalIOwe = debtsIOwe.map { it.sumOf { d -> d.amount - d.paidAmount } }
        .stateIn(viewModelScope, sub, 0L)

    val nearestDueDebt = debts.map { all ->
        all.filter { it.dueDate != null && it.status != DebtStatus.CLOSED }
            .minByOrNull { it.dueDate!! }
    }.stateIn(viewModelScope, sub, null)

    fun addDebt(
        personName: String,
        amount: Long,
        type: DebtType,
        dueDate: Long?,
        comment: String,
    ) = viewModelScope.launch {
        createDebtUseCase(personName, amount, type, dueDate, comment)
    }

    /**
     * Частичная/полная оплата долга.
     * Деньги списываются с основного счёта → записывается расход категории "Долги".
     * Затем обновляется статус самого долга.
     *
     * @param accountId счёт списания (основной или выбранный пользователем)
     */
    fun settle(debtId: String, amount: Long, accountId: String? = null) =
        viewModelScope.launch {
            // 1. Найти счёт для списания
            val acctId = accountId
                ?: appSettings.value.primaryAccountId.ifBlank { null }
                ?: accounts.value.firstOrNull()?.id

            // 2. Задача 3.4: тип транзакции зависит от направления долга
            // OWED_TO_ME (мне вернули долг) → доход на мой счёт
            // I_OWE (я возвращаю долг) → расход с моего счёта
            if (acctId != null) {
                val debt     = repository.observeDebts().first().firstOrNull { it.id == debtId }
                val isIncome = debt?.type == DebtType.OWED_TO_ME
                addTransactionUseCase(
                    accountId = acctId,
                    amount    = amount,
                    category  = "Долги",
                    type      = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    note      = debt?.personName?.let {
                        if (isIncome) "Возврат долга от: $it" else "Погашение долга: $it"
                    },
                )
            }

            // 3. Обновить статус долга
            settleDebtUseCase(debtId, amount)
        }

    fun deleteDebt(id: String) = viewModelScope.launch {
        repository.deleteDebt(id)
    }
}
