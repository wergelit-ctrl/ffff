package com.finfocus.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.BudgetGroup
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.presentation.util.CategoryIcons

/**
 * Экран управления категориями расходов (вызывается из Настроек).
 * Позволяет: добавлять категории, задавать иконку, менять группу 50/30/20, удалять.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val categories = viewModel.categories.collectAsStateWithLifecycle().value
    val showAddSheet = remember { mutableStateOf(false) }
    val deleteTarget = remember { mutableStateOf<ExpenseCategory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet.value = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_action))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Группируем по BudgetGroup
            val grouped = categories.groupBy { it.group }
            listOf(BudgetGroup.NEED, BudgetGroup.WANT, BudgetGroup.SAVE, BudgetGroup.UNSET)
                .forEach { group ->
                    val cats = grouped[group] ?: return@forEach
                    item {
                        Text(
                            text = group.label(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(cats) { cat ->
                        CategorySettingsRow(
                            category = cat,
                            onGroupChange = { newGroup ->
                                viewModel.updateCategoryGroup(cat.id, newGroup)
                            },
                            onDelete = if (!cat.isDefault) {
                                { deleteTarget.value = cat }
                            } else null,
                        )
                    }
                }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Диалог подтверждения удаления
    deleteTarget.value?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteTarget.value = null },
            title = { Text(stringResource(R.string.categories_delete_confirm_title)) },
            text = { Text(stringResource(R.string.categories_delete_confirm_body, cat.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(cat.id)
                    deleteTarget.value = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget.value = null }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    // Шит добавления новой категории
    if (showAddSheet.value) {
        AddCategorySheet(
            onDismiss = { showAddSheet.value = false },
            onAdd = { name, iconKey, group ->
                viewModel.addCategory(name, iconKey, group)
            },
        )
    }
}

@Composable
private fun CategorySettingsRow(
    category: ExpenseCategory,
    onGroupChange: (BudgetGroup) -> Unit,
    onDelete: (() -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = CategoryIcons.get(category.iconKey),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                // Чипы смены группы
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(BudgetGroup.NEED, BudgetGroup.WANT, BudgetGroup.SAVE).forEach { g ->
                        FilterChip(
                            selected = category.group == g,
                            onClick = { onGroupChange(g) },
                            label = {
                                Text(
                                    text = g.shortLabel(),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategorySheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, iconKey: String, group: BudgetGroup) -> Unit,
) {
    val name = remember { mutableStateOf("") }
    val selectedIcon = remember { mutableStateOf("more_horiz") }
    val selectedGroup = remember { mutableStateOf(BudgetGroup.UNSET) }
    val nameError = remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.categories_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it; nameError.value = false },
                label = { Text(stringResource(R.string.categories_name_label)) },
                isError = nameError.value,
                supportingText = if (nameError.value) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Группа бюджета
            Text(
                text = stringResource(R.string.categories_group_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(BudgetGroup.NEED, BudgetGroup.WANT, BudgetGroup.SAVE, BudgetGroup.UNSET)
                    .forEach { g ->
                        FilterChip(
                            selected = selectedGroup.value == g,
                            onClick = { selectedGroup.value = g },
                            label = { Text(g.shortLabel()) },
                        )
                    }
            }

            // Выбор иконки
            Text(
                text = stringResource(R.string.categories_icon_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CategoryIcons.keys) { key ->
                    val isSelected = selectedIcon.value == key
                    IconButton(
                        onClick = { selectedIcon.value = key },
                        modifier = Modifier
                            .size(40.dp)
                            .then(
                                if (isSelected) Modifier.padding(0.dp) else Modifier
                            ),
                    ) {
                        Icon(
                            imageVector = CategoryIcons.get(key),
                            contentDescription = key,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (name.value.isBlank()) { nameError.value = true; return@Button }
                    onAdd(name.value.trim(), selectedIcon.value, selectedGroup.value)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_action))
            }
        }
    }
}

// ── Extension helpers ─────────────────────────────────────────

private fun BudgetGroup.label(): String = when (this) {
    BudgetGroup.NEED  -> "Нужды (50%)"
    BudgetGroup.WANT  -> "Желания (30%)"
    BudgetGroup.SAVE  -> "Сбережения (20%)"
    BudgetGroup.UNSET -> "Без группы"
}

private fun BudgetGroup.shortLabel(): String = when (this) {
    BudgetGroup.NEED  -> "Нужды"
    BudgetGroup.WANT  -> "Желания"
    BudgetGroup.SAVE  -> "Сбережения"
    BudgetGroup.UNSET -> "—"
}
