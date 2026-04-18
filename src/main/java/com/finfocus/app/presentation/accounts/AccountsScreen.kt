package com.finfocus.app.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AccountType
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.AccountIcons
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.presentation.util.toKopecks
import com.finfocus.app.ui.theme.FinFocusTheme

/**
 * Экран счетов.
 *
 * Задача 3.1:
 * - Убран заголовок «Мои счета» (LargeTopAppBar → нет топбара, экономим место)
 * - Тип счёта (основной / сбережения) выбирается прямо с карточки через контекстное меню
 * - Бейджи ★ и 💰 показывают роль счёта
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(viewModel: AccountsViewModel = hiltViewModel()) {
    val accounts = viewModel.accounts.collectAsStateWithLifecycle().value
    val settings = viewModel.appSettings.collectAsStateWithLifecycle().value
    val selected = viewModel.selectedAccount.collectAsStateWithLifecycle().value

    val showAdd      = rememberSaveable { mutableStateOf(false) }
    val showIncome   = rememberSaveable { mutableStateOf(false) }
    val showExpense  = rememberSaveable { mutableStateOf(false) }
    val showTransfer = rememberSaveable { mutableStateOf(false) }
    val pendingAcct  = remember { mutableStateOf<Account?>(null) }

    val incomeCategory  = stringResource(R.string.income_type_salary)
    val expenseCategory = stringResource(R.string.category_food)
    val newAccountDesc  = stringResource(R.string.new_account)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick   = { showAdd.value = true },
                modifier  = Modifier.semantics { contentDescription = newAccountDesc },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        if (accounts.isEmpty()) {
            EmptyState(stringResource(R.string.empty_accounts))
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Задача 3.1: краткая сводка вместо заголовка
                item {
                    AccountsSummaryHeader(
                        accounts        = accounts,
                        primaryId       = settings.primaryAccountId,
                        savingsId       = settings.savingsAccountId,
                    )
                }

                items(accounts) { account ->
                    AccountCard(
                        account   = account,
                        isPrimary = account.id == settings.primaryAccountId,
                        isSavings = account.id == settings.savingsAccountId,
                        onClick   = { viewModel.select(account) },
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Контекстное меню
        if (selected != null) {
            ModalBottomSheet(onDismissRequest = { viewModel.select(null) }) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text       = selected.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    HorizontalDivider()

                    AccountMenuItem(stringResource(R.string.action_deposit)) {
                        pendingAcct.value = selected; viewModel.select(null); showIncome.value = true
                    }
                    AccountMenuItem(stringResource(R.string.action_withdraw)) {
                        pendingAcct.value = selected; viewModel.select(null); showExpense.value = true
                    }
                    AccountMenuItem(stringResource(R.string.action_transfer)) {
                        pendingAcct.value = selected; viewModel.select(null); showTransfer.value = true
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Задача 3.1: выбор роли прямо из меню
                    val isPrimary = selected.id == settings.primaryAccountId
                    val isSavings = selected.id == settings.savingsAccountId

                    AccountMenuItem(
                        label = if (isPrimary)
                            stringResource(R.string.accounts_already_primary)
                        else
                            stringResource(R.string.accounts_set_primary),
                        icon  = Icons.Default.Star,
                        tint  = if (isPrimary) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = !isPrimary,
                    ) {
                        viewModel.setPrimaryAccount(selected.id); viewModel.select(null)
                    }
                    AccountMenuItem(
                        label = if (isSavings)
                            stringResource(R.string.accounts_already_savings)
                        else
                            stringResource(R.string.accounts_set_savings),
                        icon  = Icons.Default.Savings,
                        tint  = if (isSavings) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = !isSavings,
                    ) {
                        viewModel.setSavingsAccount(selected.id); viewModel.select(null)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    AccountMenuItem(
                        label = stringResource(R.string.action_delete),
                        tint  = MaterialTheme.colorScheme.error,
                        icon  = Icons.Default.Delete,
                    ) {
                        viewModel.deleteAccount(selected.id); viewModel.select(null)
                    }
                }
            }
        }
    }

    if (showAdd.value) {
        AddAccountSheet(
            onDismiss = { showAdd.value = false },
            onCreate  = { name, bal, iconKey, type ->
                viewModel.addAccount(name, type, bal, iconKey)
            },
        )
    }
    if (showIncome.value) {
        val acct = pendingAcct.value ?: return
        AmountSheet(
            title     = stringResource(R.string.action_deposit),
            onDismiss = { showIncome.value = false; pendingAcct.value = null },
            onSubmit  = { amount, note -> viewModel.deposit(acct.id, amount, incomeCategory, note) },
        )
    }
    if (showExpense.value) {
        val acct = pendingAcct.value ?: return
        AmountSheet(
            title     = stringResource(R.string.action_withdraw),
            onDismiss = { showExpense.value = false; pendingAcct.value = null },
            onSubmit  = { amount, note -> viewModel.withdraw(acct.id, amount, expenseCategory, note) },
        )
    }
    if (showTransfer.value) {
        val acct = pendingAcct.value ?: return
        TransferSheet(
            current   = acct,
            accounts  = accounts,
            onDismiss = { showTransfer.value = false; pendingAcct.value = null },
            onSubmit  = { toId, amount -> viewModel.transfer(acct.id, toId, amount) },
        )
    }
}

// ── Краткая сводка вместо заголовка ──────────────────────────

@Composable
private fun AccountsSummaryHeader(
    accounts: List<Account>,
    primaryId: String,
    savingsId: String,
) {
    val totalBalance = accounts.sumOf { it.balance }
    val primaryAcct  = accounts.firstOrNull { it.id == primaryId }
    val savingsAcct  = accounts.firstOrNull { it.id == savingsId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text  = stringResource(R.string.total_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text       = totalBalance.formatByr(),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (primaryAcct != null || savingsAcct != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (primaryAcct != null) {
                        AccountRoleChip(
                            icon  = Icons.Default.Star,
                            label = primaryAcct.name,
                            value = primaryAcct.balance.formatByr(),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (savingsAcct != null) {
                        AccountRoleChip(
                            icon  = Icons.Default.Savings,
                            label = savingsAcct.name,
                            value = savingsAcct.balance.formatByr(),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRoleChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

// ── AccountCard ───────────────────────────────────────────────

@Composable
private fun AccountCard(
    account: Account,
    isPrimary: Boolean,
    isSavings: Boolean,
    onClick: () -> Unit,
) {
    val icon         = AccountIcons.get(account.iconKey)
    val accentColor  = Color(account.color).copy(alpha = 0.85f)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .semantics { contentDescription = "${account.name}: ${account.balance.formatByr()}" },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier         = Modifier.size(48.dp).clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = accentColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (isPrimary) Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary)
                    if (isSavings) Icon(Icons.Default.Savings, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                }
                Text(stringResource(account.type.labelRes()), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text       = account.balance.formatByr(),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = if (account.balance >= 0) MaterialTheme.colorScheme.onSurface
                             else MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Вспомогательные composable ────────────────────────────────

@Composable
private fun AccountMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label, color = if (enabled) tint else tint.copy(alpha = 0.4f)) },
        leadingContent  = icon?.let { { Icon(it, null, tint = if (enabled) tint else tint.copy(alpha = 0.4f)) } },
        modifier        = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, balance: Long, iconKey: String, type: AccountType) -> Unit,
) {
    val name      = remember { mutableStateOf("") }
    val balance   = remember { mutableStateOf("") }
    val iconKey   = remember { mutableStateOf("wallet") }
    val nameError = remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.new_account), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name.value, onValueChange = { name.value = it; nameError.value = false },
                label = { Text(stringResource(R.string.account_name)) },
                isError = nameError.value,
                supportingText = if (nameError.value) { { Text(stringResource(R.string.error_name_required)) } } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                value = balance.value, onValueChange = { balance.value = it },
                label = { Text(stringResource(R.string.initial_balance)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Text(stringResource(R.string.accounts_icon_label), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountIcons.keys) { key ->
                    val isSel = iconKey.value == key
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(if (isSel) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { iconKey.value = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AccountIcons.get(key), null, modifier = Modifier.size(22.dp),
                            tint = if (isSel) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Button(
                onClick = {
                    if (name.value.isBlank()) { nameError.value = true; return@Button }
                    onCreate(name.value.trim(),
                        balance.value.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L,
                        iconKey.value, AccountType.WALLET)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.create_action)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountSheet(title: String, onDismiss: () -> Unit, onSubmit: (Long, String?) -> Unit) {
    val amount = remember { mutableStateOf("") }
    val note   = remember { mutableStateOf("") }
    val error  = remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = amount.value, onValueChange = { amount.value = it; error.value = false },
                label = { Text(stringResource(R.string.amount_byn)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error.value,
                supportingText = if (error.value) { { Text(stringResource(R.string.error_amount_positive)) } } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(value = note.value, onValueChange = { note.value = it },
                label = { Text(stringResource(R.string.comment_optional)) },
                modifier = Modifier.fillMaxWidth(), maxLines = 2)
            Button(
                onClick = {
                    val p = amount.value.replace(',', '.').toDoubleOrNull()
                    if (p == null || p <= 0.0) { error.value = true; return@Button }
                    onSubmit(p.toKopecks(), note.value.ifBlank { null }); onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.apply_action)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferSheet(
    current: Account, accounts: List<Account>,
    onDismiss: () -> Unit, onSubmit: (String, Long) -> Unit,
) {
    val amount      = remember { mutableStateOf("") }
    val toAccount   = remember { mutableStateOf<String?>(null) }
    val amountError = remember { mutableStateOf(false) }
    val targetError = remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.action_transfer), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = amount.value, onValueChange = { amount.value = it; amountError.value = false },
                label = { Text(stringResource(R.string.amount_byn)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError.value,
                supportingText = if (amountError.value) { { Text(stringResource(R.string.error_amount_positive)) } } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            if (targetError.value) Text(stringResource(R.string.error_select_target),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            accounts.filter { it.id != current.id }.forEach { acct ->
                ListItem(
                    headlineContent   = { Text(acct.name) },
                    supportingContent = { Text(acct.balance.formatByr()) },
                    leadingContent    = { Icon(AccountIcons.get(acct.iconKey), null, modifier = Modifier.size(20.dp)) },
                    trailingContent   = if (toAccount.value == acct.id) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    modifier = Modifier.clickable { toAccount.value = acct.id; targetError.value = false },
                )
            }
            Button(
                onClick = {
                    val p = amount.value.replace(',', '.').toDoubleOrNull()
                    if (p == null || p <= 0.0) { amountError.value = true; return@Button }
                    val t = toAccount.value; if (t == null) { targetError.value = true; return@Button }
                    onSubmit(t, p.toKopecks()); onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_transfer)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountsPreview() { FinFocusTheme { AccountsScreen() } }

private fun AccountType.labelRes(): Int = when (this) {
    AccountType.CARD         -> R.string.account_type_card
    AccountType.WALLET       -> R.string.account_type_wallet
    AccountType.BANK_ACCOUNT -> R.string.account_type_bank
    AccountType.STASH        -> R.string.account_type_stash
    AccountType.OTHER        -> R.string.account_type_other
}
