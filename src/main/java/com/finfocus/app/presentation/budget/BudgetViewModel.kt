package com.finfocus.app.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.domain.model.BudgetGroup
import com.finfocus.app.domain.model.BudgetPeriod
import com.finfocus.app.domain.model.DefaultCategories
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.accounts.AddTransactionUseCase
import com.finfocus.app.domain.usecase.accounts.TransferBetweenAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Данные одной группы бюджета 50/30/20 для отображения в UI.
 *
 * @param group      NEED / WANT / SAVE
 * @param label      "Нужды", "Желания", "Сбережения"
 * @param percent    50 / 30 / 20
 * @param limit      лимит в Br (income * percent/100)
 * @param spent      уже потрачено за период
 * @param categories список категорий этой группы с суммами
 */
data class BudgetGroupData(
    val group: BudgetGroup,
    val label: String,
    val percent: Int,
    val limit: Long,   // Задача 4.2: копейки
    val spent: Long,
    val categories: List<Pair<String, Long>>, // name → spent (kopecks)
)

/**
 * Данные одной группы бюджета 50/30/20 для отображения в UI.
 *
 * @param group      NEED / WANT / SAVE
 * @param label      "Нужды", "Желания", "Сбережения"
 * @param percent    50 / 30 / 20
 * @param limit      лимит в Br (income * percent/100)
 * @param spent      уже потрачено за период
 * @param categories список категорий этой группы с суммами
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val transferUseCase: TransferBetweenAccountsUseCase,
) : ViewModel() {

    private val sub = SharingStarted.WhileSubscribed(5_000)

    private val _selectedPeriod = MutableStateFlow(BudgetPeriod.MONTH)
    val selectedPeriod: StateFlow<BudgetPeriod> = _selectedPeriod

    val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, sub, com.finfocus.app.domain.model.AppSettings())

    val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, sub, emptyList())

    private val categories = repository.observeCategories()
        .stateIn(viewModelScope, sub, DefaultCategories.list)

    private val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, sub, emptyList())

    // ── Расходы по категориям за выбранный период ─────────────

    private val spentByCategory = combine(transactions, _selectedPeriod) { txList, period ->
        val cutoff = period.cutoffMs()
        txList
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= cutoff }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, sub, emptyMap())

    // ── Три группы 50/30/20 ───────────────────────────────────

    /**
     * Вычисляет BudgetGroupData для NEED / WANT / SAVE.
     * Лимит берётся из appSettings.budgetIncomeAmount * процент группы.
     * Расходы группируются по BudgetGroup категорий.
     */
    val budgetGroups = combine(
        appSettings,
        categories,
        spentByCategory,
    ) { settings, cats, spent ->
        val income = settings.budgetIncomeAmount

        listOf(
            Triple(BudgetGroup.NEED, "Нужды",      50),
            Triple(BudgetGroup.WANT, "Желания",     30),
            Triple(BudgetGroup.SAVE, "Сбережения",  20),
        ).map { (group, label, percent) ->
            val limit = income * percent / 100L
            val groupCats = cats.filter { it.group == group }
            val categoryBreakdown = groupCats.map { cat ->
                cat.name to (spent[cat.name] ?: 0L)
            }
            val groupSpent: Long = categoryBreakdown.sumOf { it.second }
            BudgetGroupData(
                group      = group,
                label      = label,
                percent    = percent,
                limit      = limit,
                spent      = groupSpent,
                categories = categoryBreakdown.filter { it.second > 0L },
            )
        }
    }.stateIn(viewModelScope, sub, emptyList())

    // ── Доступные счета для кнопки "Отправить в сбережения" ───

    val savingsAccount = combine(accounts, appSettings) { accs, settings ->
        accs.firstOrNull { it.id == settings.savingsAccountId }
    }.stateIn(viewModelScope, sub, null)

    val primaryAccount = combine(accounts, appSettings) { accs, settings ->
        accs.firstOrNull { it.id == settings.primaryAccountId }
            ?: accs.firstOrNull()
    }.stateIn(viewModelScope, sub, null)

    // ── Мутации ───────────────────────────────────────────────

    fun setPeriod(period: BudgetPeriod) { _selectedPeriod.value = period }

    /**
     * Установить ежемесячный доход и пересчитать лимиты.
     * Лимиты хранятся в AppSettings, а не в отдельной таблице Budget.
     */
    fun setIncome(amount: Long) = viewModelScope.launch {
        repository.saveSettings(appSettings.value.copy(budgetIncomeAmount = amount))
    }

    /**
     * Отправить [amount] Br с основного счёта на счёт сбережений.
     * Создаёт две транзакции: TRANSFER_OUT и TRANSFER_IN.
     * Если счёт сбережений не задан — ничего не делает.
     */
    /**
     * Задача 3.2: рассчитывает 20% от баланса основного счёта.
     */
    fun calculateSavingsAmount(): Long? {
        val primary = primaryAccount.value ?: return null
        val amount  = primary.balance * 20L / 100L  // 20% в копейках
        return if (amount > 0L) amount else null
    }

    fun sendToSavings(amount: Long) = viewModelScope.launch {
        val primary = primaryAccount.value ?: return@launch
        val savings = savingsAccount.value ?: return@launch
        transferUseCase(
            fromAccountId = primary.id,
            toAccountId   = savings.id,
            amount        = amount,
        )
    }

    private fun BudgetPeriod.cutoffMs(): Long {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        return when (this) {
            BudgetPeriod.WEEK  -> now - 7 * dayMs
            BudgetPeriod.MONTH -> now - 30 * dayMs
            BudgetPeriod.YEAR  -> now - 365 * dayMs
        }
    }
}
