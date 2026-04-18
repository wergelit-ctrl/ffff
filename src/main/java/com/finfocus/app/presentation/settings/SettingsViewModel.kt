package com.finfocus.app.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.data.settings.ISettingsStore
import com.finfocus.app.domain.model.AppSettings
import com.finfocus.app.domain.model.BudgetGroup
import com.finfocus.app.domain.model.DefaultCategories
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.domain.repository.FinFocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: ISettingsStore,
    private val repository: FinFocusRepository,
) : ViewModel() {

    // ── SAF folder ────────────────────────────────────────────

    val contractsFolderLabel = settingsStore.contractsTreeUri
        .map { raw ->
            if (raw.isNullOrBlank()) return@map ""
            runCatching {
                Uri.parse(raw).lastPathSegment?.substringAfterLast(':') ?: raw
            }.getOrDefault(raw)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val isFolderSet = settingsStore.contractsTreeUri
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun resetOnboarding() = viewModelScope.launch { settingsStore.resetOnboarding() }

    // ── Categories ────────────────────────────────────────────

    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultCategories.list)

    val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Добавить новую пользовательскую категорию. */
    fun addCategory(name: String, iconKey: String, group: BudgetGroup) = viewModelScope.launch {
        val current = categories.value.toMutableList()
        current.add(
            ExpenseCategory(
                id = "cat_${UUID.randomUUID()}",
                name = name.trim(),
                iconKey = iconKey,
                group = group,
                isDefault = false,
            )
        )
        repository.saveCategories(current)
    }

    /** Удалить категорию (нельзя удалить isDefault). */
    fun deleteCategory(id: String) = viewModelScope.launch {
        val current = categories.value.filter { it.id != id || it.isDefault }
        repository.saveCategories(current)
    }

    /** Изменить группу бюджета у категории (Нужды / Желания / Сбережения). */
    fun updateCategoryGroup(id: String, group: BudgetGroup) = viewModelScope.launch {
        val updated = categories.value.map {
            if (it.id == id) it.copy(group = group) else it
        }
        repository.saveCategories(updated)
    }

    // ── App settings ──────────────────────────────────────────

    fun setPrimaryAccount(accountId: String) = viewModelScope.launch {
        repository.saveSettings(appSettings.value.copy(primaryAccountId = accountId))
    }

    fun setSavingsAccount(accountId: String) = viewModelScope.launch {
        repository.saveSettings(appSettings.value.copy(savingsAccountId = accountId))
    }

    fun setBudgetIncome(amount: Long) = viewModelScope.launch {
        repository.saveSettings(appSettings.value.copy(budgetIncomeAmount = amount))
    }
}
