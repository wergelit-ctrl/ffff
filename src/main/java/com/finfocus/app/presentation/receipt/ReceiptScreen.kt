package com.finfocus.app.presentation.receipt

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.presentation.util.formatByr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экран-чек после успешной операции.
 *
 * Задача 2.6: красивый банковский чек с анимацией появления,
 * кнопка-галочка внизу по центру.
 * Задача 2.7: semantics для accessibility.
 *
 * @param amount       сумма в копейках (Long, задача 4.2)
 * @param categoryName название категории
 * @param isExpense    true = расход (красный), false = доход (зелёный)
 * @param isSavings    true = операция сбережения (иконка копилки)
 * @param onDone       возврат на главный экран
 */
@Composable
fun ReceiptScreen(
    amount:       Long,
    categoryName: String,
    isExpense:    Boolean = true,
    isSavings:    Boolean = false,
    onDone:       () -> Unit = {},
) {
    // Анимация появления чека
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue    = if (visible) 1f else 0.7f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label          = "receipt-scale",
    )
    LaunchedEffect(Unit) { visible = true }

    val amountDouble = amount / 100.0
    val sign    = if (isExpense) "−" else "+"
    val color   = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val dateStr = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault()).format(Date())

    Scaffold { padding ->
        Box(
            modifier         = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            // ── Чек ───────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .scale(scale)
                    .semantics { contentDescription = "Чек операции: $sign${amountDouble.formatByr()}, категория $categoryName" },
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Иконка операции
                    Box(
                        modifier         = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isExpense) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = if (isSavings) Icons.Default.Savings else Icons.Default.Check,
                            contentDescription = null,
                            modifier           = Modifier.size(32.dp),
                            tint               = color,
                        )
                    }

                    Text(
                        text  = if (isExpense) stringResource(R.string.receipt_expense_title)
                                else stringResource(R.string.receipt_income_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Сумма
                    Text(
                        text       = "$sign${amountDouble.formatByr()}",
                        style      = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color      = color,
                        textAlign  = TextAlign.Center,
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Строки чека
                    ReceiptRow(label = stringResource(R.string.receipt_category), value = categoryName)
                    ReceiptRow(label = stringResource(R.string.receipt_date),     value = dateStr)
                    ReceiptRow(
                        label = stringResource(R.string.receipt_type),
                        value = if (isExpense) stringResource(R.string.tx_type_expense)
                                else stringResource(R.string.tx_type_income),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Пунктирная линия — как на реальном чеке
                    Text(
                        text  = "· · · · · · · · · · · · · · · · · · · ·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )

                    Text(
                        text      = "FinFocus",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── Кнопка-галочка внизу по центру (2.6) ─────────────
            FloatingActionButton(
                onClick           = onDone,
                modifier          = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .semantics { contentDescription = "Готово, вернуться на главный экран" },
                containerColor    = MaterialTheme.colorScheme.primary,
                contentColor      = Color.White,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier           = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
    }
}
