package com.finfocus.app.presentation.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.finfocus.app.R
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.presentation.util.CategoryIcons

/**
 * Диалог выбора категории расходов.
 * Открывается по центру экрана (Dialog), а не снизу.
 * Сразу показывает цифровую клавиатуру для ввода суммы.
 * Enter (ImeAction.Done) сохраняет транзакцию и закрывает диалог.
 *
 * @param categories     список категорий из репозитория
 * @param selectedId     id текущей выбранной категории
 * @param initialAmount  начальное значение поля суммы (если уже вводили)
 * @param onDismiss      закрытие диалога без сохранения
 * @param onConfirm      колбэк с (categoryId, amount, note)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerDialog(
    categories: List<ExpenseCategory>,
    selectedId: String,
    initialAmount: String = "",
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, amount: Long, note: String) -> Unit,
) {
    val selectedCategory = remember { mutableStateOf(selectedId) }
    val amountText = remember { mutableStateOf(initialAmount) }
    val noteText = remember { mutableStateOf("") }
    val amountError = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Автофокус на поле суммы — сразу открывает цифровую клавиатуру
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun tryConfirm() {
        val amountDbl = amountText.value.replace(',', '.').toDoubleOrNull()
        if (amountDbl == null || amountDbl <= 0.0) {
            amountError.value = true
            return
        }
        val amount: Long = (amountDbl * 100).toLong()
        keyboard?.hide()
        onConfirm(selectedCategory.value, amount, noteText.value.trim())  // amount already Long
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Заголовок
                Text(
                    text = stringResource(R.string.spend_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                // Поле суммы — СРАЗУ открывает цифровую клавиатуру
                OutlinedTextField(
                    value = amountText.value,
                    onValueChange = {
                        val filtered = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        if (filtered.count { c -> c == '.' || c == ',' } <= 1)
                            amountText.value = filtered
                        amountError.value = false
                    },
                    label = { Text(stringResource(R.string.amount_byn)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        // Enter закрывает клавиатуру и сохраняет
                        onDone = { tryConfirm() },
                    ),
                    isError = amountError.value,
                    supportingText = if (amountError.value) {
                        { Text(stringResource(R.string.error_amount_positive)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )

                // Сетка категорий с иконками — 4 колонки
                Text(
                    text = stringResource(R.string.spend_choose_category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { cat ->
                        CategoryCell(
                            category = cat,
                            isSelected = cat.id == selectedCategory.value,
                            onClick = { selectedCategory.value = cat.id },
                        )
                    }
                }

                // Поле заметки (необязательное)
                OutlinedTextField(
                    value = noteText.value,
                    onValueChange = { noteText.value = it },
                    label = { Text(stringResource(R.string.tx_note_label)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { tryConfirm() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.cancel_action))
                    }
                    Button(
                        onClick = { tryConfirm() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.tx_save_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon = CategoryIcons.get(category.iconKey)
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else bgColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.name,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
