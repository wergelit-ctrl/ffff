package com.finfocus.app.presentation.debts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.ui.theme.FinFocusTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel:        DebtViewModel = hiltViewModel(),
    onCreateOwedToMe: () -> Unit = {},
    onCreateIOwe:     () -> Unit = {},
) {
    val tabs      = listOf(
        stringResource(R.string.debt_tab_owed_to_me),
        stringResource(R.string.debt_tab_i_owe),
    )
    val selected   = rememberSaveable { mutableIntStateOf(0) }
    val owed       = viewModel.debtsOwedToMe.collectAsStateWithLifecycle().value
    val iOweList   = viewModel.debtsIOwe.collectAsStateWithLifecycle().value
    val totalOwed  = viewModel.totalOwedToMe.collectAsStateWithLifecycle().value
    val totalIOwe  = viewModel.totalIOwe.collectAsStateWithLifecycle().value
    val accounts   = viewModel.accounts.collectAsStateWithLifecycle().value
    val settings   = viewModel.appSettings.collectAsStateWithLifecycle().value
    val list       = if (selected.intValue == 0) owed else iOweList
    val haptic     = LocalHapticFeedback.current

    val settleTarget    = remember { mutableStateOf<Debt?>(null) }

    Scaffold(
        // Задача 3.5: заголовок «Долги» убран
        floatingActionButton = {
            // Две кнопки: "Мне должны" и "Я должен"
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { onCreateOwedToMe() },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.debt_tab_owed_to_me))
                }
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { onCreateIOwe() },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.debt_tab_i_owe))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Итог выбранной вкладки ────────────────────────
            val currentTotal = if (selected.intValue == 0) totalOwed else totalIOwe
            if (currentTotal > 0L) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (selected.intValue == 0)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = stringResource(R.string.debt_total_label),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text       = currentTotal.formatByr(),
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = if (selected.intValue == 0)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            TabRow(selectedTabIndex = selected.intValue) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selected.intValue == i,
                        onClick  = { selected.intValue = i },
                        text     = { Text(title) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (list.isEmpty()) {
                EmptyState(stringResource(R.string.empty_debts))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(list, key = { it.id }) { debt ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        settleTarget.value = debt
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deleteDebt(debt.id)
                                        true
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val dir   = dismissState.dismissDirection
                                val color by animateColorAsState(
                                    when (dir) {
                                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                                        else -> Color.Transparent
                                    },
                                    label = "swipe-bg",
                                )
                                val align = if (dir == SwipeToDismissBoxValue.StartToEnd)
                                    Alignment.CenterStart else Alignment.CenterEnd
                                val icon  = if (dir == SwipeToDismissBoxValue.StartToEnd)
                                    Icons.Default.Payments else Icons.Default.Delete

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = align,
                                ) {
                                    if (dir != SwipeToDismissBoxValue.Settled)
                                        Icon(icon, null, tint = Color.White,
                                            modifier = Modifier.size(24.dp))
                                }
                            },
                        ) {
                            DebtCard(
                                debt      = debt,
                                onSettle  = { settleTarget.value = debt },
                                onDelete  = { viewModel.deleteDebt(debt.id) },
                                isIOwe    = selected.intValue == 1,
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }


    // ── Шит частичной оплаты ──────────────────────────────────
    settleTarget.value?.let { debt ->
        PartialSettleSheet(
            debt      = debt,
            accounts  = accounts,
            primaryId = settings.primaryAccountId,
            onDismiss = { settleTarget.value = null },
            onSettle  = { amount, accountId ->
                viewModel.settle(debt.id, amount, accountId)
                settleTarget.value = null
            },
        )
    }
}

// ── DebtCard ──────────────────────────────────────────────────

@Composable
private fun DebtCard(
    debt: Debt,
    onSettle: () -> Unit,
    onDelete: () -> Unit,
    isIOwe: Boolean,
) {
    val remaining = debt.amount - debt.paidAmount
    val progress  = if (debt.amount > 0) (debt.paidAmount / debt.amount).toFloat() else 0f
    val isClosed  = debt.status == DebtStatus.CLOSED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = if (isClosed)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Имя + сумма
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = debt.personName,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (debt.comment.isNotBlank()) {
                        Text(
                            text  = debt.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = remaining.formatByr(),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = when {
                            isClosed  -> MaterialTheme.colorScheme.outline
                            isIOwe    -> MaterialTheme.colorScheme.error
                            else      -> MaterialTheme.colorScheme.tertiary
                        },
                    )
                    if (debt.paidAmount > 0 && !isClosed) {
                        Text(
                            text  = stringResource(R.string.debt_paid_of,
                                debt.paidAmount.formatByr(), debt.amount.formatByr()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Прогресс-бар если есть частичная оплата
            if (debt.paidAmount > 0) {
                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier.fillMaxWidth(),
                    color      = if (isClosed) MaterialTheme.colorScheme.outline
                                 else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                )
            }

            // Срок (если задан)
            debt.dueDate?.let { ts ->
                val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val overdue = ts < System.currentTimeMillis() && !isClosed
                Text(
                    text  = stringResource(R.string.debt_due_date_label, fmt.format(Date(ts))),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                )
            }

            // Кнопки если не закрыт
            if (!isClosed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSettle) {
                        Icon(Icons.Default.Payments, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.debt_pay_partial))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text  = stringResource(R.string.debt_closed_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

// ── AddDebtSheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDebtSheet(
    onDismiss: () -> Unit,
    onAdd: (person: String, amount: Long, isIOwe: Boolean, comment: String, dueDate: Long?) -> Unit,
) {
    val person          = remember { mutableStateOf("") }
    val amount          = remember { mutableStateOf("") }
    val comment         = remember { mutableStateOf("") }
    val isIOwe          = remember { mutableStateOf(true) }
    val dueDate         = remember { mutableStateOf<Long?>(null) }
    val showDatePicker  = remember { mutableStateOf(false) }
    val personError     = remember { mutableStateOf(false) }
    val amountError     = remember { mutableStateOf(false) }
    val fmt             = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.debt_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = person.value,
                onValueChange = { person.value = it; personError.value = false },
                label = { Text(stringResource(R.string.person_name)) },
                isError = personError.value,
                supportingText = if (personError.value) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
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
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                value = comment.value, onValueChange = { comment.value = it },
                label = { Text(stringResource(R.string.comment_optional)) },
                modifier = Modifier.fillMaxWidth(), maxLines = 2,
            )

            // Тип долга
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(true to stringResource(R.string.debt_tab_i_owe),
                       false to stringResource(R.string.debt_tab_owed_to_me))
                    .forEach { (value, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = isIOwe.value == value,
                            onClick  = { isIOwe.value = value },
                            label    = { Text(label) },
                        )
                    }
            }

            // Срок — необязательный
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { showDatePicker.value = true }) {
                    Text(stringResource(R.string.debt_set_due_date))
                }
                dueDate.value?.let {
                    Text(fmt.format(Date(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { dueDate.value = null }) {
                        Text("×", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Button(
                onClick = {
                    var err = false
                    if (person.value.isBlank()) { personError.value = true; err = true }
                    val parsed = amount.value.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) { amountError.value = true; err = true }
                    if (err) return@Button
                    onAdd(person.value.trim(), (parsed!! * 100).toLong(), isIOwe.value,
                        comment.value.trim(), dueDate.value)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add_action)) }
        }
    }

    if (showDatePicker.value) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate.value)
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton    = {
                TextButton(onClick = {
                    dueDate.value = state.selectedDateMillis
                    showDatePicker.value = false
                }) { Text(stringResource(R.string.apply_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        ) { DatePicker(state = state) }
    }
}

// ── PartialSettleSheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartialSettleSheet(
    debt: Debt,
    accounts: List<Account>,
    primaryId: String,
    onDismiss: () -> Unit,
    onSettle: (amount: Long, accountId: String?) -> Unit,
) {
    val amount      = remember { mutableStateOf("") }
    val selectedAcct = remember {
        mutableStateOf(primaryId.ifBlank { accounts.firstOrNull()?.id })
    }
    val error = remember { mutableStateOf(false) }
    val remaining = debt.amount - debt.paidAmount

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = stringResource(R.string.debt_pay_partial),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text  = stringResource(R.string.debt_remaining, debt.personName, remaining.formatByr()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Выбор счёта списания
            if (accounts.isNotEmpty()) {
                Text(
                    text  = stringResource(R.string.debt_pay_from_account),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                accounts.forEach { acct ->
                    androidx.compose.material3.ListItem(
                        headlineContent   = { Text(acct.name) },
                        supportingContent = { Text(acct.balance.formatByr()) },
                        trailingContent   = if (selectedAcct.value == acct.id) {
                            { Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        modifier = Modifier.clickable {
                            selectedAcct.value = acct.id
                        },
                    )
                }
                HorizontalDivider()
            }

            OutlinedTextField(
                value = amount.value,
                onValueChange = { amount.value = it; error.value = false },
                label = { Text(stringResource(R.string.partial_settle_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error.value,
                supportingText = if (error.value) {
                    { Text(stringResource(R.string.error_amount_positive)) }
                } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )

            // Кнопка "Оплатить полностью"
            TextButton(
                onClick = { amount.value = (remaining.toDouble() / 100.0).toString() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.debt_pay_full, remaining.formatByr()))
            }

            Button(
                onClick = {
                    val parsed = amount.value.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) { error.value = true; return@Button }
                    onSettle((parsed * 100).toLong().coerceAtMost(remaining), selectedAcct.value)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.partial_settle_action)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DebtPreview() { FinFocusTheme { DebtScreen() } }
