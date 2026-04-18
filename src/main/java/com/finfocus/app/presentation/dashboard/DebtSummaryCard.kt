package com.finfocus.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.presentation.util.formatByr

/**
 * Карточка сводки долгов:
 * — левая колонка: мне должны (зелёный)
 * — правая колонка: я должен (красный)
 *
 * Не показывается если оба значения равны нулю.
 *
 * @param owedToMe  сумма которую должны мне
 * @param iOwe      сумма которую должен я
 */
@Composable
fun DebtSummaryCard(
    owedToMe: Long,
    iOwe: Long,
    modifier: Modifier = Modifier,
) {
    // Скрываем карточку если долгов нет
    if (owedToMe <= 0.0 && iOwe <= 0.0) return

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.debts_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Мне должны
                if (owedToMe > 0.0) {
                    Column {
                        Text(
                            text = stringResource(R.string.debt_tab_owed_to_me),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+${owedToMe.formatByr()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                // Я должен
                if (iOwe > 0.0) {
                    Column(
                        horizontalAlignment = if (owedToMe > 0.0)
                            androidx.compose.ui.Alignment.End
                        else
                            androidx.compose.ui.Alignment.Start,
                    ) {
                        Text(
                            text = stringResource(R.string.debt_tab_i_owe),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "-${iOwe.formatByr()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
