package com.finfocus.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.model.WishStatus
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.domain.usecase.prediction.PredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FinFocusRepository,
    predictionEngine: PredictionEngine,
) : ViewModel() {

    private val sub = SharingStarted.WhileSubscribed(5_000)

    // ── Сырые данные ──────────────────────────────────────────

    val accounts = repository.observeAccounts()
        .stateIn(viewModelScope, sub, emptyList())

    private val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, sub, emptyList())

    val debts = repository.observeDebts()
        .stateIn(viewModelScope, sub, emptyList())

    private val wishItems = repository.observeWishlist()
        .stateIn(viewModelScope, sub, emptyList())

    private val budgets = repository.observeBudgets()
        .stateIn(viewModelScope, sub, emptyList())

    val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, sub, com.finfocus.app.domain.model.AppSettings())

    /** Категории расходов для CategoryPickerDialog на Dashboard. */
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, sub, com.finfocus.app.domain.model.DefaultCategories.list)

    // ── Выбранный счёт ────────────────────────────────────────

    /**
     * Id счёта выбранного на дашборде.
     * null = показываем общий баланс всех счетов.
     */
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId = _selectedAccountId

    fun selectAccount(id: String?) {
        _selectedAccountId.value = id
    }

    /** Текущий выбранный объект Account (или null = "все"). */
    val selectedAccount = combine(accounts, _selectedAccountId) { accs, id ->
        accs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, sub, null)

    // ── Баланс выбранного счёта (или сумма всех) ──────────────

    val displayBalance = combine(accounts, _selectedAccountId) { accs, id ->
        if (id == null) accs.sumOf { it.balance }
        else accs.firstOrNull { it.id == id }?.balance ?: 0L
    }.stateIn(viewModelScope, sub, 0L)

    val totalBalance = accounts
        .map { it.sumOf { a -> a.balance } }
        .stateIn(viewModelScope, sub, 0L)

    // ── График: расходы выбранного счёта за 30 дней ───────────

    val expenseChartData = combine(transactions, _selectedAccountId) { all, id ->
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1_000
        all
            .filter { tx ->
                tx.type == TransactionType.EXPENSE
                    && tx.timestamp >= cutoff
                    && (id == null || tx.accountId == id)
            }
            .sortedBy { it.timestamp }
            .map { it.amount.toFloat() }
    }.stateIn(viewModelScope, sub, emptyList())

    // ── Доход / расход за 30 дней ─────────────────────────────

    private val last30 = combine(transactions, _selectedAccountId) { all, id ->
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1_000
        all.filter { tx ->
            tx.timestamp >= cutoff && (id == null || tx.accountId == id)
        }
    }.stateIn(viewModelScope, sub, emptyList())

    val monthlyIncome = last30
        .map { list -> list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }
        .stateIn(viewModelScope, sub, 0L)

    val monthlyExpense = last30
        .map { list -> list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }
        .stateIn(viewModelScope, sub, 0L)

    // ── Прогресс бюджета ──────────────────────────────────────

    val budgetProgress = combine(budgets, transactions) { b, t ->
        val limit: Long = b.sumOf { it.limit }
        val spent: Long = t.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        if (limit <= 0L) 0f else (spent.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, sub, 0f)

    // ── Долги ─────────────────────────────────────────────────

    val debtsOwedToMe = debts
        .map { list -> list.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.amount - it.paidAmount } }
        .stateIn(viewModelScope, sub, 0L)

    val debtsIOwe = debts
        .map { list -> list.filter { it.type == DebtType.I_OWE }.sumOf { it.amount - it.paidAmount } }
        .stateIn(viewModelScope, sub, 0L)

    // ── Мечты ─────────────────────────────────────────────────

    val topWishItems: kotlinx.coroutines.flow.StateFlow<List<WishItem>> = wishItems
        .map { list ->
            list.filter { it.status != WishStatus.BOUGHT }
                .sortedBy { it.priority.ordinal }
                .take(3)
        }
        .stateIn(viewModelScope, sub, emptyList())

    // ── Последние транзакции ──────────────────────────────────

    val recentTransactions: kotlinx.coroutines.flow.StateFlow<List<Transaction>> =
        combine(transactions, _selectedAccountId) { all, id ->
            all.filter { id == null || it.accountId == id }
                .sortedByDescending { it.timestamp }
                .take(5)
        }.stateIn(viewModelScope, sub, emptyList())

    // ── Инсайты ───────────────────────────────────────────────

    // Задача 5.4: тяжёлые вычисления на Dispatchers.Default
    val insights = combine(transactions, debts, wishItems, totalBalance, budgets) { tx, d, w, bal, b ->
        predictionEngine.generateInsights(tx, d, w, bal, b)
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, sub, emptyList())

}
