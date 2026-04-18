package com.finfocus.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.domain.model.AnalyticsPeriod
import com.finfocus.app.domain.model.BudgetGroup
import com.finfocus.app.domain.model.DefaultCategories
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import com.finfocus.app.presentation.util.toRubles
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Расходы по группе 50/30/20 за период. */
data class BudgetGroupSpend(
    val group: BudgetGroup,
    val label: String,
    val color: Long,
    val spent: Long,   // Задача 4.2: копейки
    val limit: Long,   // Задача 4.2: копейки
)

/**
 * Точка на графике с привязкой к дате.
 * Задача 5.1: xLabel — метка оси X (dd.MM или MM.yy).
 */
data class ChartPoint(val xLabel: String, val value: Float, val dateKey: String)

/**
 * Данные для столбчатого графика «Доходы vs Расходы».
 * Задача 5.1+5.2: groupKey = yyyy-MM-dd, xLabel = читаемая дата.
 */
data class BarChartEntry(val xLabel: String, val income: Float, val expense: Float, val dateKey: String)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    repository: FinFocusRepository,
) : ViewModel() {

    private val sub  = SharingStarted.WhileSubscribed(5_000)
    private val zone = ZoneId.systemDefault()

    private val _selectedPeriod = MutableStateFlow(AnalyticsPeriod.MONTH)
    val selectedPeriod: StateFlow<AnalyticsPeriod> = _selectedPeriod

    val transactions = repository.observeTransactions()
        .stateIn(viewModelScope, sub, emptyList())

    private val categories = repository.observeCategories()
        .stateIn(viewModelScope, sub, DefaultCategories.list)

    private val appSettings = repository.observeSettings()
        .stateIn(viewModelScope, sub, com.finfocus.app.domain.model.AppSettings())

    // ── Расходы по категориям ─────────────────────────────────

    /** Задача 4.2: суммируем Long, возвращаем Long. */
    val expenseByCategory = combine(transactions, selectedPeriod) { tx, period ->
        val cutoff = periodCutoffMs(period)
        tx.filter { it.type == TransactionType.EXPENSE && it.timestamp >= cutoff }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }.flowOn(Dispatchers.Default)                               // Задача 5.4
     .stateIn(viewModelScope, sub, emptyMap())

    // ── Расходы по группам 50/30/20 ──────────────────────────

    val budgetGroupSpends = combine(
        transactions, selectedPeriod, categories, appSettings,
    ) { tx, period, cats, settings ->
        val cutoff = periodCutoffMs(period)
        val income = settings.budgetIncomeAmount                // Long
        val spentByCategory = tx
            .filter { it.type == TransactionType.EXPENSE && it.timestamp >= cutoff }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }    // Long

        listOf(
            Triple(BudgetGroup.NEED, "Нужды",     0xFF_4CAF50.toLong()),
            Triple(BudgetGroup.WANT, "Желания",    0xFF_2196F3.toLong()),
            Triple(BudgetGroup.SAVE, "Сбережения", 0xFF_FF9800.toLong()),
        ).mapIndexed { idx, (group, label, color) ->
            val percent   = listOf(50L, 30L, 20L)[idx]
            val groupCats = cats.filter { it.group == group }.map { it.name }.toSet()
            val spent     = spentByCategory.entries
                .filter { it.key in groupCats }.sumOf { it.value }
            BudgetGroupSpend(
                group   = group,
                label   = label,
                color   = color,
                spent   = spent,
                limit   = income * percent / 100L,
            )
        }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, sub, emptyList())

    // ── Доходы vs Расходы (задача 5.1+5.2) ───────────────────

    /**
     * Задача 5.2: группировка по yyyy-MM-dd вместо dd.MM.
     * Задача 5.1: xLabel — dd.MM или MM.yy в зависимости от периода.
     */
    val incomeVsExpense = combine(transactions, selectedPeriod) { tx, period ->
        val cutoff = periodCutoffMs(period)
        tx.filter { it.timestamp >= cutoff }
            .groupBy { isoDateKey(it.timestamp) }              // yyyy-MM-dd
            .entries
            .sortedBy { it.key }
            .map { (key, list) ->
                BarChartEntry(
                    xLabel  = formatAxisLabel(key, period),
                    income  = list.filter { it.type == TransactionType.INCOME }
                                  .sumOf { it.amount }.toRubles().toFloat(),
                    expense = list.filter { it.type == TransactionType.EXPENSE }
                                  .sumOf { it.amount }.toRubles().toFloat(),
                    dateKey = key,
                )
            }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, sub, emptyList())

    // ── Баланс во времени (задача 5.1+5.2) ───────────────────

    /**
     * Задача 5.2: агрегация по дате yyyy-MM-dd — нарастающий баланс.
     * Задача 5.1: xLabel для оси X.
     */
    val balanceOverTime = combine(transactions, selectedPeriod) { tx, period ->
        val cutoff = periodCutoffMs(period)
        var running = 0L                                        // копейки
        tx.sortedBy { it.timestamp }
            .filter { it.timestamp >= cutoff }
            .groupBy { isoDateKey(it.timestamp) }
            .entries
            .sortedBy { it.key }
            .map { (key, dayTx) ->
                dayTx.forEach { t ->
                    running += if (t.type == TransactionType.EXPENSE ||
                                   t.type == TransactionType.TRANSFER_OUT)
                        -t.amount else t.amount
                }
                ChartPoint(
                    xLabel  = formatAxisLabel(key, period),
                    value   = running.toRubles().toFloat(),
                    dateKey = key,
                )
            }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, sub, emptyList())

    // ── Прогноз 90 дней (задача 5.1) ─────────────────────────

    val forecastData = combine(transactions, selectedPeriod) { tx, _ ->
        if (tx.size < 7) return@combine emptyList<ChartPoint>()
        val now    = System.currentTimeMillis()
        val dayMs  = 86_400_000L
        val avgDaily = tx
            .filter { it.type == TransactionType.EXPENSE }
            .sortedBy { it.timestamp }
            .takeLast(30)
            .sumOf { it.amount } / 30.0                        // Long / Int → Double

        var lastBalance = 0L
        tx.sortedBy { it.timestamp }.forEach { t ->
            lastBalance += if (t.type == TransactionType.EXPENSE ||
                               t.type == TransactionType.TRANSFER_OUT)
                -t.amount else t.amount
        }

        (1..90).map { i ->
            val ts  = now + i * dayMs
            val key = isoDateKey(ts)
            ChartPoint(
                xLabel  = formatAxisLabel(key, AnalyticsPeriod.THREE_MONTHS),
                value   = (lastBalance.toRubles() - avgDaily * i).coerceAtLeast(0.0).toFloat(),
                dateKey = key,
            )
        }
    }.flowOn(Dispatchers.Default)
     .stateIn(viewModelScope, sub, emptyList())

    fun setPeriod(period: AnalyticsPeriod) { _selectedPeriod.value = period }

    // ── Helpers ───────────────────────────────────────────────

    private fun periodCutoffMs(period: AnalyticsPeriod): Long {
        val now   = System.currentTimeMillis()
        val dayMs = 86_400_000L
        return when (period) {
            AnalyticsPeriod.WEEK         -> now -  7 * dayMs
            AnalyticsPeriod.MONTH        -> now - 30 * dayMs
            AnalyticsPeriod.THREE_MONTHS -> now - 90 * dayMs
            AnalyticsPeriod.YEAR         -> now - 365 * dayMs
            AnalyticsPeriod.CUSTOM       -> 0L
        }
    }

    /**
     * Задача 5.2: ключ группировки — yyyy-MM-dd.
     * Учитывает системный часовой пояс, не даёт смешивать дни разных лет.
     */
    private fun isoDateKey(ts: Long): String =
        Instant.ofEpochMilli(ts)
            .atZone(zone)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)          // yyyy-MM-dd

    /**
     * Задача 5.1: форматирует yyyy-MM-dd в удобочитаемую метку для оси X.
     * - WEEK → "dd"
     * - MONTH → "dd.MM"
     * - THREE_MONTHS / YEAR → "MM.yy"
     */
    internal fun formatAxisLabel(isoDate: String, period: AnalyticsPeriod): String {
        return runCatching {
            val date = LocalDate.parse(isoDate)
            when (period) {
                AnalyticsPeriod.WEEK         -> date.format(DateTimeFormatter.ofPattern("dd"))
                AnalyticsPeriod.MONTH        -> date.format(DateTimeFormatter.ofPattern("dd.MM"))
                AnalyticsPeriod.THREE_MONTHS,
                AnalyticsPeriod.YEAR,
                AnalyticsPeriod.CUSTOM       -> date.format(DateTimeFormatter.ofPattern("MM.yy"))
            }
        }.getOrDefault(isoDate)
    }
}
