package com.finfocus.app.presentation.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import com.finfocus.app.domain.model.Priority

/**
 * Шит добавления нового желания/цели.
 * Поля: название, цена, ежемесячный взнос, категория, комментарий, приоритет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, price: Long, priority: Priority, category: String, comment: String, monthlySaving: Long) -> Unit,
) {
    val name = remember { mutableStateOf("") }
    val price = remember { mutableStateOf("") }
    val monthly = remember { mutableStateOf("") }
    val category = remember { mutableStateOf("") }
    val comment = remember { mutableStateOf("") }
    val priority = remember { mutableStateOf(Priority.MEDIUM) }

    val nameError = remember { mutableStateOf(false) }
    val priceError = remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.plans_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Название
            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it; nameError.value = false },
                label = { Text(stringResource(R.string.plans_name_label)) },
                isError = nameError.value,
                supportingText = if (nameError.value) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Цена и ежемесячный взнос — в одной строке
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = price.value,
                    onValueChange = { price.value = it; priceError.value = false },
                    label = { Text(stringResource(R.string.plans_price_label)) },
                    isError = priceError.value,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = monthly.value,
                    onValueChange = { monthly.value = it },
                    label = { Text(stringResource(R.string.plans_monthly_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // Категория
            OutlinedTextField(
                value = category.value,
                onValueChange = { category.value = it },
                label = { Text(stringResource(R.string.plans_category_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Комментарий
            OutlinedTextField(
                value = comment.value,
                onValueChange = { comment.value = it },
                label = { Text(stringResource(R.string.comment_optional)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
            )

            // Приоритет — три чипа
            Text(
                text = stringResource(R.string.plans_priority_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val label = when (p) {
                        Priority.HIGH -> stringResource(R.string.priority_high)
                        Priority.MEDIUM -> stringResource(R.string.priority_medium)
                        Priority.LOW -> stringResource(R.string.priority_low)
                    }
                    FilterChip(
                        selected = priority.value == p,
                        onClick = { priority.value = p },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    var hasError = false
                    if (name.value.isBlank()) { nameError.value = true; hasError = true }
                    val parsedPrice = price.value.replace(',', '.').toDoubleOrNull()
                    if (parsedPrice == null || parsedPrice <= 0.0) { priceError.value = true; hasError = true }
                    if (hasError) return@Button

                    onAdd(
                        name.value.trim(),
                        (parsedPrice!! * 100).toLong(),
                        priority.value,
                        category.value.trim(),
                        comment.value.trim(),
                        ((monthly.value.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toLong(),
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_action))
            }
        }
    }
}

/**
 * Шит пополнения накоплений по конкретному желанию (запасной вариант, не используется в основном PlansScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingSheet(
    itemName: String,
    onDismiss: () -> Unit,
    onAdd: (amount: Long) -> Unit,
) {
    val amount = remember { mutableStateOf("") }
    val amountError = remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.plans_add_saving_for, itemName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = amount.value,
                onValueChange = { amount.value = it; amountError.value = false },
                label = { Text(stringResource(R.string.amount_byn)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError.value,
                supportingText = if (amountError.value) {
                    { Text(stringResource(R.string.error_amount_positive)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = {
                    val parsed = amount.value.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) { amountError.value = true; return@Button }
                    onAdd((parsed * 100).toLong())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.apply_action))
            }
        }
    }
}
