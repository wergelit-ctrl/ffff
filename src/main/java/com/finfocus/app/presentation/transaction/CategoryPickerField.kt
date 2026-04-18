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

/**
 * Дропдаун выбора категории.
 * Список категорий передаётся снаружи — экран подставляет нужный
 * в зависимости от типа транзакции (доход / расход).
 *
 * @param categories      список строк-категорий
 * @param selectedCategory текущая выбранная категория
 * @param onSelect        колбэк при выборе
 * @param modifier        внешний модификатор
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerField(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = remember { mutableStateOf(false) }

    // Если выбранная категория не входит в текущий список (например,
    // после переключения типа) — сбрасываем отображение на первый элемент
    val displayValue = if (selectedCategory in categories) selectedCategory
    else categories.firstOrNull() ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.tx_category_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded.value) },
            modifier = Modifier
                .then(modifier)
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat) },
                    onClick = {
                        onSelect(cat)
                        expanded.value = false
                    },
                )
            }
        }
    }
}
