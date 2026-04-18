package com.finfocus.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.Severity
import com.finfocus.app.presentation.components.EmptyState

/**
 * Карточка инсайтов / прогнозов FinFocus.
 * Листается горизонтально, показывает до 3 инсайтов.
 * Dot-индикатор отображает текущую страницу.
 *
 * @param insights список инсайтов из PredictionEngine
 */
@Composable
fun InsightsCard(
    insights: List<InsightCard>,
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
                text = stringResource(R.string.prediction_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (insights.isEmpty()) {
                EmptyState(
                    message = stringResource(R.string.no_data_yet),
                    modifier = Modifier.height(80.dp),
                )
            } else {
                val pageCount = insights.size.coerceAtMost(3)
                val pagerState = rememberPagerState(pageCount = { pageCount })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    val insight = insights[page]
                    InsightPage(insight = insight)
                }

                // Dot-индикатор
                if (pageCount > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(pageCount) { index ->
                            val isActive = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isActive) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightPage(insight: InsightCard) {
    // Цвет заголовка зависит от severity
    val titleColor = when (insight.severity) {
        Severity.HIGH -> MaterialTheme.colorScheme.error
        Severity.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Severity.LOW -> MaterialTheme.colorScheme.onSurface
    }

    // Эмодзи-иконка из поля icon
    val iconText = insight.icon.ifBlank { "💡" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = iconText, style = MaterialTheme.typography.titleMedium)
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
        }
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!insight.actionLabel.isNullOrBlank()) {
            TextButton(
                onClick = {},
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Text(text = insight.actionLabel)
            }
        }
    }
}
