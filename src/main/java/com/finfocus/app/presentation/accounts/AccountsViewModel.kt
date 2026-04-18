package com.finfocus.app.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AccountType
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import com.finfocus.app.domain.usecase.accounts.CreateAccountUseCase
import com.finfocus.app.domain.usecase.accounts.TransferBetweenAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    private val createAccountUseCase: CreateAccountUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val transferBetweenAccountsUseCase: TransferBetweenAccountsUseCase,
) : ViewModel() {

    val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
            com.finfocus.app.domain.model.AppSettings())

    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount

    fun select(account: Account?) { _selectedAccount.value = account }

    /** Создать счёт с иконкой из AccountIcons. */
    fun addAccount(
        name: String,
        type: AccountType,
        balance: Long,
        iconKey: String = "wallet",
        color: Long = 0xFF_4CAF50,
    ) = viewModelScope.launch {
        createAccountUseCase(name, type, balance, iconKey, color)
    }

    fun deposit(accountId: String, amount: Long, incomeType: String, note: String?) =
        viewModelScope.launch {
            addTransactionUseCase(accountId, amount, incomeType, TransactionType.INCOME, note)
        }

    fun withdraw(accountId: String, amount: Long, category: String, note: String?) =
        viewModelScope.launch {
            addTransactionUseCase(accountId, amount, category, TransactionType.EXPENSE, note)
        }

    fun transfer(fromId: String, toId: String, amount: Long) = viewModelScope.launch {
        transferBetweenAccountsUseCase(fromId, toId, amount)
    }

    fun deleteAccount(id: String) = viewModelScope.launch {
        repository.deleteAccount(id)
    }

    /** Пометить счёт как основной. */
    fun setPrimaryAccount(accountId: String) = viewModelScope.launch {
        repository.saveSettings(appSettings.value.copy(primaryAccountId = accountId))
    }

    /** Пометить счёт как сберегательный. */
    fun setSavingsAccount(accountId: String) = viewModelScope.launch {
        repository.saveSettings(appSettings.value.copy(savingsAccountId = accountId))
    }
}
