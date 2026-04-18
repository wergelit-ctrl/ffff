package com.finfocus.app.presentation.transaction

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.finfocus.app.R
import com.finfocus.app.domain.model.Account
import com.finfocus.app.presentation.util.formatByr

/**
 * Дропдаун для выбора счёта.
 * Показывает название + отформатированный баланс в каждом пункте.
 *
 * @param accounts   список доступных счетов
 * @param selectedId id выбранного счёта (или null)
 * @param isError    показывать ли рамку ошибки
 * @param onSelect   колбэк при выборе
 * @param modifier   внешний модификатор
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerField(
    accounts: List<Account>,
    selectedId: String?,
    isError: Boolean = false,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf(false) }
    val selectedAccount = accounts.firstOrNull { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            // Показываем "Название · 42,00 Br" или пустую строку
            value = selectedAccount?.let { "${it.name} · ${it.balance.formatByr()}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.tx_account_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded.value) },
            isError = isError,
            supportingText = if (isError) {
                { Text(stringResource(R.string.error_select_account)) }
            } else null,
            modifier = Modifier
                .then(modifier)
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Text("${account.name} · ${account.balance.formatByr()}")
                    },
                    onClick = {
                        onSelect(account.id)
                        expanded.value = false
                    },
                )
            }
        }
    }
}
