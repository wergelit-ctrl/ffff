package com.finfocus.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.formatByr

/**
 * Карточка топ-3 активных целей на дашборде.
 * Показывает название, прогресс-бар и накоплено/цена.
 *
 * Называется DashboardWishlistCard (не WishlistCard) — чтобы не конфликтовать
 * с возможными одноимёнными функциями в пакете plans/.
 *
 * @param wishItems список топ-3 желаний из VM (уже отфильтрован и отсортирован)
 */
@Composable
fun DashboardWishlistCard(
    wishItems: List<WishItem>,
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
                text = stringResource(R.string.wishlist_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (wishItems.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.empty_wishlist),
                    modifier = Modifier.height(60.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    wishItems.forEach { item ->
                        WishItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun WishItemRow(item: WishItem) {
    val progress = if (item.price > 0)
(item.saved.toFloat() / item.price.toFloat()).coerceIn(0f, 1f)
    else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${item.saved.formatByr()} / ${item.price.formatByr()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        )
    }
}
