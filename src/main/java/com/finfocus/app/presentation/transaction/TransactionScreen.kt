package com.finfocus.app.presentation.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.ui.theme.FinFocusTheme

/**
 * Экран добавления транзакции.
 *
 * Кнопка "Потратить" / "Пополнить" на Dashboard открывает этот экран
 * с уже выбранным типом транзакции и счётом. Диалог CategoryPickerDialog
 * всплывает сразу при нажатии "Потратить", открывает цифровую клавиатуру
 * и позволяет нажать Enter чтобы сохранить.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    initialAccountId: String = "",
    initialIsExpense: Boolean = true,
    onBack: () -> Unit = {},
) {
    val accounts = viewModel.accounts.collectAsStateWithLifecycle().value
    val categories = viewModel.expenseCategories.collectAsStateWithLifecycle().value
    val form = viewModel.form.collectAsStateWithLifecycle().value
    val event = viewModel.event.collectAsStateWithLifecycle().value

    // Показывать ли диалог выбора категории (для расходов)
    val showCategoryDialog = remember { mutableStateOf(false) }

    LaunchedEffect(accounts) {
        viewModel.initAccount(accounts, initialAccountId)
        if (initialIsExpense != (viewModel.form.value.type == TxType.EXPENSE)) {
            viewModel.setType(if (initialIsExpense) TxType.EXPENSE else TxType.INCOME)
        }
    }

    LaunchedEffect(event) {
        if (event is TransactionEvent.Saved) {
            viewModel.consumeEvent()
            onBack()
        }
    }

    // Доходные категории — хардкод (они не настраиваются пользователем)
    val incomeCategories = listOf(
        stringResource(R.string.income_type_salary),
        stringResource(R.string.income_type_scholarship),
        stringResource(R.string.income_type_service),
        stringResource(R.string.income_type_gift),
        stringResource(R.string.income_type_debt_return),
        stringResource(R.string.income_type_sale),
        stringResource(R.string.income_type_other),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_transaction_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel_action),
                        )
                    }
                },
            )
        },
    ) { padding ->

        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.tx_no_accounts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Тип транзакции
            TypeToggle(
                selected = form.type,
                onSelect = { viewModel.setType(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Выбор счёта
            AccountPickerField(
                accounts = accounts,
                selectedId = form.selectedAccountId,
                isError = form.accountError,
                onSelect = { viewModel.setAccount(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Для расхода — кнопка открывающая CategoryPickerDialog
            if (form.type == TxType.EXPENSE) {
                val selectedCat = categories.firstOrNull { it.id == form.selectedCategoryId }
                Button(
                    onClick = { showCategoryDialog.value = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (selectedCat != null)
                            stringResource(R.string.spend_button_label, selectedCat.name)
                        else
                            stringResource(R.string.action_withdraw),
                    )
                }
            } else {
                // Для дохода — обычный пикер + поле суммы
                AmountField(
                    value = form.amountText,
                    isError = form.amountError,
                    onChange = { viewModel.setAmount(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryPickerField(
                    categories = incomeCategories,
                    selectedCategory = incomeCategories.firstOrNull() ?: "",
                    onSelect = { viewModel.setCategory(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                NoteField(
                    value = form.note,
                    onChange = { viewModel.setNote(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.submit() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.tx_save_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Диалог выбора категории (для расходов) — всплывает по центру
    if (showCategoryDialog.value && form.type == TxType.EXPENSE) {
        CategoryPickerDialog(
            categories = categories,
            selectedId = form.selectedCategoryId,
            initialAmount = form.amountText,
            onDismiss = { showCategoryDialog.value = false },
            onConfirm = { categoryId, amount, note ->
                showCategoryDialog.value = false
                viewModel.submitFromDialog(categoryId, amount, note)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionScreenPreview() {
    FinFocusTheme { TransactionScreen() }
}
