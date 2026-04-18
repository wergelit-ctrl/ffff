package com.finfocus.app.presentation.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.Account
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.AccountIcons
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.ui.theme.FinFocusTheme

/**
 * Главный экран FinFocus.
 *
 * Структура (сверху вниз):
 * 1. Выбор счёта — горизонтальный скролл чипов
 * 2. Название счёта + баланс + мини-график расходов
 * 3. Три кнопки: Потратить (большая) / Пополнить (большая) / Перевести (меньше)
 * 4. Краткая аналитика: доход vs расход за 30 дней
 * 5. Последние транзакции
 * 6. Топ целей / долги
 *
 * "Потратить" открывает CategoryPickerDialog прямо поверх этого экрана —
 * без перехода на TransactionScreen. Диалог сразу показывает цифровую клавиатуру.
 * Название приложения НЕ отображается.
 *
 * @param onDeposit  открыть экран добавления дохода
 * @param onTransfer открыть экран перевода между счетами
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onDeposit: (accountId: String) -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onSpend: (accountId: String) -> Unit = {},
    onTransfer: (accountId: String) -> Unit = {},
) {
    val accounts        = viewModel.accounts.collectAsStateWithLifecycle().value
    val selectedId      = viewModel.selectedAccountId.collectAsStateWithLifecycle().value
    val selectedAccount = viewModel.selectedAccount.collectAsStateWithLifecycle().value
    val displayBalance  = viewModel.displayBalance.collectAsStateWithLifecycle().value
    val expenseChart    = viewModel.expenseChartData.collectAsStateWithLifecycle().value
    val monthlyIncome   = viewModel.monthlyIncome.collectAsStateWithLifecycle().value
    val monthlyExpense  = viewModel.monthlyExpense.collectAsStateWithLifecycle().value
    val insights        = viewModel.insights.collectAsStateWithLifecycle().value
    val wishItems       = viewModel.topWishItems.collectAsStateWithLifecycle().value
    val recentTx        = viewModel.recentTransactions.collectAsStateWithLifecycle().value
    val owedToMe        = viewModel.debtsOwedToMe.collectAsStateWithLifecycle().value
    val iOwe            = viewModel.debtsIOwe.collectAsStateWithLifecycle().value
    val categories      = viewModel.categories.collectAsStateWithLifecycle().value


    // Выбираем первый счёт по умолчанию при загрузке
    LaunchedEffect(accounts) {
        if (selectedId == null && accounts.isNotEmpty()) {
            viewModel.selectAccount(accounts.first().id)
        }
    }

    val activeAccountId = selectedId ?: accounts.firstOrNull()?.id ?: ""

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        message = stringResource(R.string.empty_accounts),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp),
                    )
                }
            } else {

            // ── 1. Выбор счёта ────────────────────────────────────
            item {
                AccountSelectorRow(
                    accounts = accounts,
                    selectedId = selectedId,
                    onSelect = { viewModel.selectAccount(it) },
                )
            }

            // ── 2. Карточка счёта: название + баланс + мини-граф ──
            item {
                AccountBalanceCard(
                    account = selectedAccount,
                    balance = displayBalance,
                    chartData = expenseChart,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── 3. Три кнопки действий ────────────────────────────
            item {
                ActionButtons(
                    onSpend    = { onSpend(activeAccountId) },
                    onDeposit  = { onDeposit(activeAccountId) },
                    onTransfer = { onTransfer(activeAccountId) },
                    modifier   = Modifier.padding(horizontal = 16.dp),
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ── 4. Краткая аналитика: доход / расход ──────────────
            item {
                MiniAnalyticsRow(
                    income  = monthlyIncome,
                    expense = monthlyExpense,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // ── 5. Инсайты ────────────────────────────────────────
            if (insights.isNotEmpty()) {
                item {
                    InsightsCard(
                        insights = insights,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // ── 6. Долги (если есть) ──────────────────────────────
            if (owedToMe > 0.0 || iOwe > 0.0) {
                item {
                    DebtSummaryCard(
                        owedToMe = owedToMe,
                        iOwe     = iOwe,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // ── 7. Топ мечт ───────────────────────────────────────
            if (wishItems.isNotEmpty()) {
                item {
                    DashboardWishlistCard(
                        wishItems = wishItems,
                        modifier  = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // ── 8. Последние транзакции ───────────────────────────
            item {
                RecentTransactionsCard(
                    transactions = recentTx,
                    modifier     = Modifier.padding(horizontal = 16.dp),
                )
            }
            }
        }
    }

}

@Composable
private fun AccountSelectorRow(
    accounts: List<Account>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(accounts) { account ->
            val isSelected = account.id == selectedId
            AccountChip(
                account    = account,
                isSelected = isSelected,
                onClick    = { onSelect(account.id) },
            )
        }
    }
}

@Composable
private fun AccountChip(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon = AccountIcons.get(account.iconKey)
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = account.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Карточка баланса счёта с мини-графиком ────────────────────

@Composable
private fun AccountBalanceCard(
    account: Account?,
    balance: Long,
    chartData: List<Float>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            // Название счёта
            Text(
                text = account?.name ?: stringResource(R.string.total_balance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Баланс
            Text(
                text = balance.formatByr(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            // Мини-график расходов
            if (chartData.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                MiniChartCard(
                    expenseData = chartData,
                    compact     = true,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                )
            }
        }
    }
}

// ── Три кнопки действий ───────────────────────────────────────

@Composable
private fun ActionButtons(
    onSpend: () -> Unit,
    onDeposit: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Потратить и Пополнить — одинакового размера, в одну строку
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton(
                label    = stringResource(R.string.action_withdraw),
                icon     = Icons.Default.ArrowUpward,
                onClick  = onSpend,
                primary  = true,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label    = stringResource(R.string.action_deposit),
                icon     = Icons.Default.ArrowDownward,
                onClick  = onDeposit,
                primary  = true,
                modifier = Modifier.weight(1f),
            )
        }
        // Перевести — чуть меньше (FilledTonalButton)
        FilledTonalButton(
            onClick  = onTransfer,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.action_transfer))
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = if (primary)
            ButtonDefaults.buttonColors()
        else
            ButtonDefaults.filledTonalButtonColors(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

// ── Краткая аналитика ─────────────────────────────────────────

@Composable
private fun MiniAnalyticsRow(
    income: Long,
    expense: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MiniStatCard(
            label    = stringResource(R.string.dashboard_income_label),
            value    = "+${income.formatByr()}",
            color    = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
        )
        MiniStatCard(
            label    = stringResource(R.string.dashboard_expense_label),
            value    = "-${expense.formatByr()}",
            color    = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = color,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    FinFocusTheme { DashboardScreen() }
}
