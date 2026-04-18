package com.finfocus.app.presentation.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.presentation.components.EmptyState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

/**
 * Мини-график расходов за 30 дней.
 *
 * @param expenseData суммы расходов отсортированные по timestamp
 * @param compact     true — без обёртки Card и заголовка (встраивается в другую карточку)
 */
@Composable
fun MiniChartCard(
    expenseData: List<Float>,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val producer = remember { CartesianChartModelProducer() }

    LaunchedEffect(expenseData) {
        if (expenseData.size >= 2) {
            producer.runTransaction {
                lineSeries { series(expenseData) }
            }
        }
    }

    val chartContent: @Composable () -> Unit = {
        if (expenseData.size >= 2) {
            CartesianChartHost(
                chart         = rememberCartesianChart(rememberLineCartesianLayer()),
                modelProducer = producer,
                modifier      = modifier.fillMaxWidth(),
            )
        } else if (!compact) {
            EmptyState(
                message  = stringResource(R.string.no_data_yet),
                modifier = Modifier.height(140.dp),
            )
        }
    }

    if (compact) {
        // Без Card — встраивается в AccountBalanceCard
        chartContent()
    } else {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text  = stringResource(R.string.spending_30_days),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (expenseData.size >= 2) {
                    CartesianChartHost(
                        chart         = rememberCartesianChart(rememberLineCartesianLayer()),
                        modelProducer = producer,
                        modifier      = Modifier.fillMaxWidth().height(140.dp),
                    )
                } else {
                    EmptyState(
                        message  = stringResource(R.string.no_data_yet),
                        modifier = Modifier.height(140.dp),
                    )
                }
            }
        }
    }
}
