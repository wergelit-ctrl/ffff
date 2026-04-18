package com.finfocus.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Строка фильтр-чипов с текстовыми метками.
 * Используется в экранах где нужно переключение между статусами/категориями.
 *
 * Примечание: BalanceCard и InsightAssistChip удалены — они были дублированы
 * специализированными компонентами в presentation/dashboard/.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterChips(
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { label ->
            FilterChip(
                selected = selected == label,
                onClick = { onSelect(label) },
                label = { Text(label) },
            )
        }
    }
}
