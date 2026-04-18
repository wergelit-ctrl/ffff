package com.finfocus.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.formatByr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Карточка последних 5 транзакций на дашборде.
 * Расходы — красным со знаком минус, доходы — зелёным со знаком плюс.
 *
 * @param transactions список уже отсортированных транзакций (descending timestamp, из VM)
 */
@Composable
fun RecentTransactionsCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_recent_tx_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.no_data_yet),
                    modifier = Modifier.height(60.dp),
                )
            } else {
                Column {
                    transactions.forEachIndexed { index, tx ->
                        TransactionRow(tx = tx)
                        if (index < transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val isExpense = tx.type == TransactionType.EXPENSE || tx.type == TransactionType.TRANSFER_OUT
    val amountColor = if (isExpense)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.tertiary
    val sign = if (isExpense) "−" else "+"
    val dateLabel = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(tx.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!tx.note.isNullOrBlank()) {
                Text(
                    text = tx.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sign${tx.amount.formatByr()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
