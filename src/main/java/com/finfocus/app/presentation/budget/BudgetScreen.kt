package com.finfocus.app.presentation.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.BudgetGroup
import com.finfocus.app.presentation.budget.BudgetGroupData
import com.finfocus.app.domain.model.BudgetPeriod
import com.finfocus.app.presentation.components.SwipeConfirmButton
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.presentation.util.toRubles
import com.finfocus.app.ui.theme.FinFocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val period = viewModel.selectedPeriod.collectAsStateWithLifecycle().value
    val groups = viewModel.budgetGroups.collectAsStateWithLifecycle().value
    val settings = viewModel.appSettings.collectAsStateWithLifecycle().value
    val savingsAcct = viewModel.savingsAccount.collectAsStateWithLifecycle().value
    val primaryAcct = viewModel.primaryAccount.collectAsStateWithLifecycle().value

    val showIncomeSheet = rememberSaveable { mutableStateOf(false) }
    val showSavingsSheet = rememberSaveable { mutableStateOf(false) }

    val hasSavingsSetup = savingsAcct != null && primaryAcct != null
    val income = settings.budgetIncomeAmount

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.budget_title)) },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showIncomeSheet.value = true },
                icon = { Icon(Icons.Default.Savings, contentDescription = null) },
                text = { Text(stringResource(R.string.budget_set_income)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scroll.nestedScrollConnection)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Переключатель периода ─────────────────────────────
            item {
                val tabs = listOf(BudgetPeriod.WEEK, BudgetPeriod.MONTH, BudgetPeriod.YEAR)
                TabRow(selectedTabIndex = tabs.indexOf(period)) {
                    tabs.forEach { p ->
                        Tab(
                            selected = p == period,
                            onClick = { viewModel.setPeriod(p) },
                            text = { Text(stringResource(p.labelRes())) },
                        )
                    }
                }
            }

            // ── Заголовок дохода ──────────────────────────────────
            item {
                InlineIncomeCard(
                    income = income,
                    onSave = { viewModel.setIncome(it) },
                )
            }

            // ── Три группы 50/30/20 ───────────────────────────────
            items(groups) { group ->
                BudgetGroupCard(group = group)
            }

            // ── Кнопка "Отправить в сбережения" ──────────────────
            if (hasSavingsSetup) {
                item {
                    Button(
                        onClick = { showSavingsSheet.value = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.budget_send_to_savings, savingsAcct!!.name))
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.budget_savings_not_configured),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }

    // ── Шит: Указать доход ────────────────────────────────────
    if (showIncomeSheet.value) {
        SetIncomeSheet(
            currentIncome = income,
            onDismiss = { showIncomeSheet.value = false },
            onSet = { amount ->
                viewModel.setIncome(amount)
                showIncomeSheet.value = false
            },
        )
    }

    // ── Задача 3.2: Отправить 20% в сбережения ───────────────
    if (showSavingsSheet.value) {
        val autoAmount = viewModel.calculateSavingsAmount()
        if (autoAmount != null && primaryAcct != null && savingsAcct != null) {
            SavingsSwipeSheet(
                fromName = primaryAcct.name,
                toName = savingsAcct.name,
                amount = autoAmount,
                onDismiss = { showSavingsSheet.value = false },
                onConfirm = {
                    viewModel.sendToSavings(autoAmount)
                    showSavingsSheet.value = false
                },
            )
        } else {
            showSavingsSheet.value = false
        }
    }
}

// ── Карточка группы 50/30/20 ──────────────────────────────────
@Composable
private fun BudgetGroupCard(group: BudgetGroupData) {
    val progress = if (group.limit > 0L)
        (group.spent.toFloat() / group.limit.toFloat()).coerceIn(0f, 1f)
    else 0f

    val overBudget = group.spent > group.limit && group.limit > 0L

    val containerColor = when (group.group) {
        BudgetGroup.NEED -> MaterialTheme.colorScheme.primaryContainer
        BudgetGroup.WANT -> MaterialTheme.colorScheme.tertiaryContainer
        BudgetGroup.SAVE -> MaterialTheme.colorScheme.secondaryContainer
        BudgetGroup.UNSET -> MaterialTheme.colorScheme.surfaceVariant
    }

    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Заголовок группы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = group.categories.isNotEmpty()) {
                        expanded.value = !expanded.value
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${group.label} · ${group.percent}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${group.spent.formatByr()} / ${group.limit.formatByr()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overBudget)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                if (group.categories.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded.value)
                            Icons.Default.ExpandLess
                        else
                            Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Прогресс-бар
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (overBudget)
                    MaterialTheme.colorScheme.error
                else when (group.group) {
                    BudgetGroup.NEED -> MaterialTheme.colorScheme.primary
                    BudgetGroup.WANT -> MaterialTheme.colorScheme.tertiary
                    BudgetGroup.SAVE -> MaterialTheme.colorScheme.secondary
                    BudgetGroup.UNSET -> MaterialTheme.colorScheme.outline
                },
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )

            // Раскрывающийся список категорий
            AnimatedVisibility(
                visible = expanded.value,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    group.categories.forEach { (catName, catSpent) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            )
                            Text(
                                text = catSpent.formatByr(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── SetIncomeSheet ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetIncomeSheet(
    currentIncome: Long,
    onDismiss: () -> Unit,
    onSet: (Long) -> Unit,
) {
    val income = remember { mutableStateOf(if (currentIncome > 0L) (currentIncome.toDouble() / 100.0).toString() else "") }
    val error = remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.budget_set_income),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.budget_income_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = income.value,
                onValueChange = { income.value = it; error.value = false },
                label = { Text(stringResource(R.string.budget_income_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error.value,
                supportingText = if (error.value) {
                    { Text(stringResource(R.string.error_amount_positive)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = {
                    val parsed = income.value.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        error.value = true
                        return@Button
                    }
                    onSet((parsed * 100).toLong()) // Преобразуем рубли в копейки
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.apply_action)) }
        }
    }
}

// ── InlineIncomeCard (Задача 3.3) ────────────────────────────
@Composable
private fun InlineIncomeCard(income: Long, onSave: (Long) -> Unit) {
    val editing = remember { mutableStateOf(false) }
    val inputText = remember { mutableStateOf(if (income > 0L) (income / 100.0).toString() else "") }
    val error = remember { mutableStateOf(false) }
    val focusReq = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing.value) {
        if (editing.value) {
            focusReq.requestFocus()
            keyboard?.show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { if (!editing.value) editing.value = true },
        colors = CardDefaults.cardColors(
            containerColor = if (income > 0 || editing.value)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.budget_income_monthly),
                style = MaterialTheme.typography.labelMedium,
                color = if (income > 0 || editing.value)
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (editing.value) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = inputText.value,
                        onValueChange = { inputText.value = it; error.value = false },
                        label = { Text(stringResource(R.string.budget_income_label)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val v = inputText.value.replace(',', '.').toDoubleOrNull()
                                if (v != null && v > 0) {
                                    onSave((v * 100).toLong())
                                    editing.value = false
                                    keyboard?.hide()
                                } else error.value = true
                            },
                        ),
                        isError = error.value,
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(focusReq),
                    )
                    IconButton(onClick = {
                        val v = inputText.value.replace(',', '.').toDoubleOrNull()
                        if (v != null && v > 0) {
                            onSave((v * 100).toLong())
                            editing.value = false
                            keyboard?.hide()
                        } else error.value = true
                    }) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        editing.value = false
                        keyboard?.hide()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
                if (error.value) {
                    Text(
                        stringResource(R.string.error_amount_positive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                if (income > 0) {
                    Text(
                        text = income.formatByr(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.budget_tap_to_edit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.budget_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

// ── SavingsSwipeSheet (Задача 3.2) ───────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsSwipeSheet(
    fromName: String,
    toName: String,
    amount: Long,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }

            Text(
                text = stringResource(R.string.budget_send_to_savings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(R.string.budget_send_to_savings_body, fromName, toName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Text(
                text = amount.formatByr(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )

            Text(
                text = stringResource(R.string.budget_savings_20_percent_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SwipeConfirmButton(
                onConfirm = onConfirm,
                onCancel = onDismiss,
                labelConfirm = stringResource(R.string.budget_send_action),
                labelCancel = stringResource(R.string.cancel_action),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetPreview() {
    FinFocusTheme { BudgetScreen() }
}

private fun BudgetPeriod.labelRes(): Int = when (this) {
    BudgetPeriod.WEEK -> R.string.period_week
    BudgetPeriod.MONTH -> R.string.period_month
    BudgetPeriod.YEAR -> R.string.period_year
}
