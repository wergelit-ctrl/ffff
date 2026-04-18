package com.finfocus.app.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.AnalyticsPeriod
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.ui.theme.FinFocusTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ExtraStore key для хранения меток оси X в Vico 2.x. */
private val xLabelKey = ExtraStore.Key<List<String>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val period      = viewModel.selectedPeriod.collectAsStateWithLifecycle().value
    val tx          = viewModel.transactions.collectAsStateWithLifecycle().value
    val balance     = viewModel.balanceOverTime.collectAsStateWithLifecycle().value
    val incExp      = viewModel.incomeVsExpense.collectAsStateWithLifecycle().value
    val forecast    = viewModel.forecastData.collectAsStateWithLifecycle().value
    val byCategory  = viewModel.expenseByCategory.collectAsStateWithLifecycle().value
    val groupSpends = viewModel.budgetGroupSpends.collectAsStateWithLifecycle().value

    // Задача 5.1: отдельные ModelProducer для каждого графика
    val lineModel     = remember { CartesianChartModelProducer() }
    val colModel      = remember { CartesianChartModelProducer() }
    val forecastModel = remember { CartesianChartModelProducer() }

    // Задача 5.1: обновляем модели с метками осей X
    LaunchedEffect(balance) {
        if (balance.size >= 2) {
            lineModel.runTransaction {
                lineSeries { series(balance.map { it.value }) }
                extras { it[xLabelKey] = balance.map { p -> p.xLabel } }
            }
        }
    }
    LaunchedEffect(incExp) {
        if (incExp.isNotEmpty()) {
            colModel.runTransaction {
                columnSeries {
                    series(incExp.map { it.income })
                    series(incExp.map { it.expense })
                }
                extras { it[xLabelKey] = incExp.map { e -> e.xLabel } }
            }
        }
    }
    LaunchedEffect(forecast) {
        if (forecast.size >= 2) {
            forecastModel.runTransaction {
                lineSeries { series(forecast.map { it.value }) }
                extras { it[xLabelKey] = forecast.map { p -> p.xLabel } }
            }
        }
    }

    // Задача 5.1: ValueFormatter читает метки из ExtraStore
    val dateAxisFormatter = remember {
        CartesianValueFormatter { context, value, _ ->
            val labels = context.model.extraStore.getOrNull(xLabelKey)
            labels?.getOrNull(value.toInt()) ?: value.toInt().toString()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.analytics_title)) },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.padding(padding).nestedScroll(scroll.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Фильтр периода ────────────────────────────────
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AnalyticsPeriod.entries) { p ->
                        FilterChip(
                            selected = period == p,
                            onClick  = { viewModel.setPeriod(p) },
                            label    = { Text(stringResource(p.labelRes())) },
                        )
                    }
                }
            }

            // ── Расходы по группам 50/30/20 ───────────────────
            if (groupSpends.isNotEmpty()) {
                item {
                    AnalyticsCard(title = stringResource(R.string.analytics_budget_groups_title)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            groupSpends.forEach { group ->
                                BudgetGroupRow(group = group)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                groupSpends.forEach { g ->
                                    LegendDot(color = Color(g.color), label = g.label)
                                }
                            }
                        }
                    }
                }
            }

            // ── Расходы по категориям ─────────────────────────
            item {
                AnalyticsCard(title = stringResource(R.string.expense_by_category)) {
                    if (byCategory.isEmpty()) {
                        EmptyState(stringResource(R.string.no_data_yet),
                            modifier = Modifier.height(60.dp).fillMaxWidth())
                    } else {
                        val maxSpent = byCategory.values.maxOrNull() ?: 1L
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            byCategory.entries.take(8).forEach { (cat, spent) ->
                                CategorySpendRow(name = cat, spent = spent, maxSpent = maxSpent)
                            }
                        }
                    }
                }
            }

            // ── Баланс во времени (5.1: ось X с датами) ──────
            item {
                AnalyticsCard(title = stringResource(R.string.balance_over_time)) {
                    if (balance.size >= 2) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                // Задача 5.1: нижняя ось с датами
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = dateAxisFormatter,
                                    itemPlacer     = remember {
                                        HorizontalAxis.ItemPlacer.aligned(spacing = { maxOf(1, balance.size / 6) })
                                    },
                                ),
                                startAxis = VerticalAxis.rememberStart(),
                            ),
                            modelProducer = lineModel,
                            modifier      = Modifier.fillMaxWidth().height(180.dp),
                        )
                    } else {
                        EmptyState(stringResource(R.string.no_data_yet),
                            modifier = Modifier.height(80.dp).fillMaxWidth())
                    }
                }
            }

            // ── Доходы vs Расходы (5.1: ось X с датами) ──────
            item {
                AnalyticsCard(title = stringResource(R.string.income_vs_expense)) {
                    if (incExp.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = dateAxisFormatter,
                                    itemPlacer     = remember {
                                        HorizontalAxis.ItemPlacer.aligned(spacing = { maxOf(1, incExp.size / 6) })
                                    },
                                ),
                                startAxis = VerticalAxis.rememberStart(),
                            ),
                            modelProducer = colModel,
                            modifier      = Modifier.fillMaxWidth().height(180.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(MaterialTheme.colorScheme.tertiary, stringResource(R.string.tx_type_income))
                            LegendDot(MaterialTheme.colorScheme.error, stringResource(R.string.tx_type_expense))
                        }
                    } else {
                        EmptyState(stringResource(R.string.no_data_yet),
                            modifier = Modifier.height(80.dp).fillMaxWidth())
                    }
                }
            }

            // ── Прогноз 90 дней (5.1: ось X с датами) ────────
            item {
                AnalyticsCard(title = stringResource(R.string.forecast_90_days)) {
                    if (forecast.size >= 2) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = dateAxisFormatter,
                                    itemPlacer     = remember {
                                        HorizontalAxis.ItemPlacer.aligned(spacing = { maxOf(1, forecast.size / 6) })
                                    },
                                ),
                                startAxis = VerticalAxis.rememberStart(),
                            ),
                            modelProducer = forecastModel,
                            modifier      = Modifier.fillMaxWidth().height(180.dp),
                        )
                    } else {
                        EmptyState(stringResource(R.string.no_data_yet),
                            modifier = Modifier.height(80.dp).fillMaxWidth())
                    }
                }
            }

            // ── Список транзакций за период ───────────────────
            val cutoff     = periodCutoffMs(period)
            val filteredTx = tx.filter { it.timestamp >= cutoff }.sortedByDescending { it.timestamp }

            if (filteredTx.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.analytics_tx_section, filteredTx.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(filteredTx) { t -> AnalyticsTxRow(transaction = t) }
            }
        }
    }
}

// ── Компоненты ────────────────────────────────────────────────

@Composable
private fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}

@Composable
private fun BudgetGroupRow(group: BudgetGroupSpend) {
    val progress = if (group.limit > 0L)
        (group.spent.toFloat() / group.limit.toFloat()).coerceIn(0f, 1f) else 0f
    val barColor = Color(group.color)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(group.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${group.spent.formatByr()} / ${group.limit.formatByr()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth(),
            color      = barColor,
            trackColor = barColor.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun CategorySpendRow(name: String, spent: Long, maxSpent: Long) {
    val progress = if (maxSpent > 0L) (spent.toFloat() / maxSpent.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(spent.formatByr(), style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth(),
            color      = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnalyticsTxRow(transaction: Transaction) {
    val isExpense = transaction.type == TransactionType.EXPENSE ||
        transaction.type == TransactionType.TRANSFER_OUT
    val sign     = if (isExpense) "−" else "+"
    val amtColor = if (isExpense) MaterialTheme.colorScheme.error
                   else MaterialTheme.colorScheme.tertiary
    val dateStr  = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(transaction.timestamp))

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                if (!transaction.note.isNullOrBlank())
                    Text(transaction.note, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$sign${transaction.amount.formatByr()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = amtColor)
                Text(dateStr, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    }
}

// ── Helpers ───────────────────────────────────────────────────

private fun periodCutoffMs(period: AnalyticsPeriod): Long {
    val now = System.currentTimeMillis()
    val d   = 86_400_000L
    return when (period) {
        AnalyticsPeriod.WEEK         -> now -  7 * d
        AnalyticsPeriod.MONTH        -> now - 30 * d
        AnalyticsPeriod.THREE_MONTHS -> now - 90 * d
        AnalyticsPeriod.YEAR         -> now - 365 * d
        AnalyticsPeriod.CUSTOM       -> 0L
    }
}

private fun AnalyticsPeriod.labelRes(): Int = when (this) {
    AnalyticsPeriod.WEEK         -> R.string.period_week
    AnalyticsPeriod.MONTH        -> R.string.period_month
    AnalyticsPeriod.THREE_MONTHS -> R.string.period_three_months
    AnalyticsPeriod.YEAR         -> R.string.period_year
    AnalyticsPeriod.CUSTOM       -> R.string.period_custom
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsPreview() { FinFocusTheme { AnalyticsScreen() } }
