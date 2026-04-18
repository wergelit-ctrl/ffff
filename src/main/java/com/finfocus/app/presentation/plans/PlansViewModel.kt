package com.finfocus.app.presentation.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.domain.model.Priority
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.model.WishStatus
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import com.finfocus.app.domain.usecase.plans.CreateWishItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    private val createWishItemUseCase: CreateWishItemUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
) : ViewModel() {

    private val sub = SharingStarted.WhileSubscribed(5_000)

    val wishItems = repository.observeWishlist()
        .stateIn(viewModelScope, sub, emptyList())

    val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, sub, emptyList())

    val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, sub, com.finfocus.app.domain.model.AppSettings())

    fun addItem(
        name: String,
        price: Long,
        priority: Priority,
        category: String,
        comment: String,
        monthlySaving: Long,
        planCategory: String = "",
        isUncertain: Boolean = false,
    ) = viewModelScope.launch {
        createWishItemUseCase(name, price, priority, category, comment, monthlySaving,
            planCategory, isUncertain)
    }

    /**
     * Пополнить накопления на мечту.
     * Деньги списываются с основного/выбранного счёта → расход "Мечты".
     */
    fun addSaving(
        itemId: String,
        amount: Long,
        accountId: String? = null,
    ) = viewModelScope.launch {
        val item = wishItems.value.firstOrNull { it.id == itemId } ?: return@launch
        val newSaved = (item.saved + amount).coerceAtMost(item.price)

        // Списываем расход со счёта
        val acctId = accountId
            ?: appSettings.value.primaryAccountId.ifBlank { null }
            ?: accounts.value.firstOrNull()?.id
        if (acctId != null) {
            addTransactionUseCase(
                accountId = acctId,
                amount    = amount,
                category  = "Мечты",
                type      = TransactionType.EXPENSE,
                note      = "Накопление: ${item.name}",
            )
        }

        // Обновляем мечту
        repository.updateWishItem(
            item.copy(saved = newSaved, status = WishStatus.SAVING)
        )
    }

    /**
     * Снять деньги с мечты — возвращает на счёт.
     * Требует подтверждения из UI (предупреждение).
     */
    fun withdrawSaving(
        itemId: String,
        amount: Long,
        accountId: String? = null,
    ) = viewModelScope.launch {
        val item = wishItems.value.firstOrNull { it.id == itemId } ?: return@launch
        val withdrawn: Long = amount.coerceAtMost(item.saved)
        val newSaved  = item.saved - withdrawn
        val newStatus = when {
            newSaved <= 0L   -> WishStatus.PLANNED
            else             -> WishStatus.SAVING
        }

        // Возвращаем деньги на счёт
        val acctId = accountId
            ?: appSettings.value.primaryAccountId.ifBlank { null }
            ?: accounts.value.firstOrNull()?.id
        if (acctId != null) {
            addTransactionUseCase(
                accountId = acctId,
                amount    = withdrawn,
                category  = "Мечты",
                type      = TransactionType.INCOME,
                note      = "Снятие с мечты: ${item.name}",
            )
        }

        repository.updateWishItem(
            item.copy(saved = newSaved, status = newStatus)
        )
    }

    fun markBought(id: String) = viewModelScope.launch {
        val item = wishItems.value.firstOrNull { it.id == id } ?: return@launch
        repository.updateWishItem(item.copy(saved = item.price, status = WishStatus.BOUGHT))
    }

    fun deleteItem(id: String) = viewModelScope.launch {
        repository.deleteWishItem(id)
    }

    fun monthsToGoal(item: WishItem): Int =
        if (item.monthlySaving <= 0L) 0
        else kotlin.math.ceil((item.price - item.saved).toDouble() / item.monthlySaving.toDouble()).toInt().coerceAtLeast(0)
}
