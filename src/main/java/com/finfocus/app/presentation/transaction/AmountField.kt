package com.finfocus.app.presentation.transaction

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.finfocus.app.R

/**
 * Поле ввода суммы с цифровой клавиатурой и inline-ошибкой.
 * Принимает запятую как разделитель (заменяется на точку при парсинге в VM).
 *
 * @param value    текущий текст
 * @param isError  показывать ли ошибку
 * @param onChange колбэк при вводе
 * @param modifier внешний модификатор
 */
@Composable
fun AmountField(
    value: String,
    isError: Boolean = false,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Разрешаем только цифры, точку и запятую
            // и не более одного разделителя
            val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
            val separatorCount = filtered.count { it == '.' || it == ',' }
            if (separatorCount <= 1) onChange(filtered)
        },
        label = { Text(stringResource(R.string.tx_amount_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.error_amount_positive)) }
        } else null,
        singleLine = true,
        modifier = modifier,
    )
}
