package com.finfocus.app.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.finfocus.app.R
import com.finfocus.app.presentation.accounts.AccountsScreen
import com.finfocus.app.presentation.analytics.AnalyticsScreen
import com.finfocus.app.presentation.budget.BudgetScreen
import com.finfocus.app.presentation.contracts.ContractScreen
import com.finfocus.app.presentation.dashboard.DashboardScreen
import com.finfocus.app.presentation.debts.CreateDebtScreen
import com.finfocus.app.presentation.debts.DebtScreen
import com.finfocus.app.presentation.deposit.DepositScreen
import com.finfocus.app.presentation.plans.PlansScreen
import com.finfocus.app.presentation.receipt.ReceiptScreen
import com.finfocus.app.presentation.settings.CategoriesSettingsScreen
import com.finfocus.app.presentation.settings.SettingsScreen
import com.finfocus.app.presentation.spend.SpendScreen
import com.finfocus.app.presentation.transaction.TransactionScreen
import kotlinx.coroutines.launch

/**
 * Главный навигационный граф.
 *
 * Задача 2.1: нижняя панель вкладок убрана.
 * Вместо неё — ModalNavigationDrawer (боковое меню, открывается свайпом или из TopAppBar).
 * Задача 2.7: semantics на drawer и элементах меню.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinFocusNavGraph() {
    val navController = rememberNavController()
    val drawerState   = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope         = rememberCoroutineScope()
    val current       = navController.currentBackStackEntryAsState().value?.destination

    fun closeDrawer() = scope.launch { drawerState.close() }
    fun navigate(route: NavRoute) {
        closeDrawer()
        navController.navigate(route) {
            launchSingleTop = true
            restoreState    = true
        }
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        drawerContent = {
            AppDrawerContent(
                current         = current,
                onNavigate      = { navigate(it) },
                onCloseDrawer   = { closeDrawer() },
            )
        },
        modifier = Modifier.semantics { contentDescription = "Боковое меню навигации" },
    ) {
        NavHost(
            navController    = navController,
            startDestination = NavRoute.Dashboard,
        ) {
            composable<NavRoute.Dashboard> {
                DashboardScreen(
                    onSpend = { accountId ->
                        navController.navigate(NavRoute.Spend(accountId))
                    },
                    onDeposit  = { accountId ->
                        navController.navigate(NavRoute.Deposit(accountId))
                    },
                    onTransfer = { _ ->
                        navController.navigate(NavRoute.Accounts) { launchSingleTop = true }
                    },
                    onOpenMenu = { scope.launch { drawerState.open() } },
                )
            }

            composable<NavRoute.Accounts>  { AccountsScreen() }
            composable<NavRoute.Budget>    { BudgetScreen() }
            composable<NavRoute.Plans>     { PlansScreen() }
            composable<NavRoute.Analytics> { AnalyticsScreen() }
            composable<NavRoute.Debts> {
                DebtScreen(
                    onCreateOwedToMe = {
                        navController.navigate(NavRoute.CreateDebt(isIOwe = false))
                    },
                    onCreateIOwe = {
                        navController.navigate(NavRoute.CreateDebt(isIOwe = true))
                    },
                )
            }
            composable<NavRoute.CreateDebt> { backStack ->
                val route = backStack.toRoute<NavRoute.CreateDebt>()
                CreateDebtScreen(
                    isIOwe  = route.isIOwe,
                    onBack  = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable<NavRoute.Contracts> { ContractScreen() }

            composable<NavRoute.Settings> {
                SettingsScreen(
                    onNavigateToContracts  = { navController.navigate(NavRoute.Contracts) { launchSingleTop = true } },
                    onNavigateToCategories = { navController.navigate(NavRoute.CategoriesSettings) { launchSingleTop = true } },
                )
            }
            composable<NavRoute.CategoriesSettings> {
                CategoriesSettingsScreen(onBack = { navController.popBackStack() })
            }

            // ── Новые экраны Спринта 2 ──────────────────────────

            /** Задача 2.3: отдельный экран "Снять" */
            composable<NavRoute.Spend> { backStack ->
                val route = backStack.toRoute<NavRoute.Spend>()
                SpendScreen(
                    accountId = route.accountId,
                    onConfirm = { catName, kopecks ->
                        navController.navigate(
                            NavRoute.Receipt(route.accountId, kopecks, catName, isExpense = true)
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            /** Задача 2.3: отдельный экран "Пополнить" */
            composable<NavRoute.Deposit> { backStack ->
                val route = backStack.toRoute<NavRoute.Deposit>()
                DepositScreen(
                    accountId = route.accountId,
                    onConfirm = { catName, kopecks ->
                        navController.navigate(
                            NavRoute.Receipt(route.accountId, kopecks, catName, isExpense = false)
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            /** Задача 2.6: экран-чек */
            composable<NavRoute.Receipt> { backStack ->
                val route = backStack.toRoute<NavRoute.Receipt>()
                ReceiptScreen(
                    amount       = route.amount,
                    categoryName = route.categoryName,
                    isExpense    = route.isExpense,
                    onDone       = {
                        // Возвращаемся на Dashboard, очищая стек до него
                        navController.navigate(NavRoute.Dashboard) {
                            popUpTo(NavRoute.Dashboard) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }

            /** Старый Transaction — оставлен для совместимости */
            composable<NavRoute.Transaction> { backStack ->
                val route = backStack.toRoute<NavRoute.Transaction>()
                TransactionScreen(
                    initialAccountId = route.accountId,
                    initialIsExpense = route.isExpense,
                    onBack           = { navController.popBackStack() },
                )
            }
        }
    }
}

// ── Содержимое бокового меню ──────────────────────────────────

@Composable
private fun AppDrawerContent(
    current: NavDestination?,
    onNavigate: (NavRoute) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp).fillMaxHeight()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text     = "FinFocus",
                style    = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DrawerItem(
                label       = stringResource(R.string.tab_dashboard),
                icon        = { Icon(Icons.Default.Home, contentDescription = null) },
                selected    = current.match(NavRoute.Dashboard),
                onClick     = { onNavigate(NavRoute.Dashboard) },
            )
            DrawerItem(
                label    = stringResource(R.string.tab_accounts),
                icon     = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                selected = current.match(NavRoute.Accounts),
                onClick  = { onNavigate(NavRoute.Accounts) },
            )
            DrawerItem(
                label    = stringResource(R.string.tab_budget),
                icon     = { Icon(Icons.Default.PieChart, contentDescription = null) },
                selected = current.match(NavRoute.Budget),
                onClick  = { onNavigate(NavRoute.Budget) },
            )
            DrawerItem(
                label    = stringResource(R.string.tab_debts),
                icon     = { Icon(Icons.Default.MoneyOff, contentDescription = null) },
                selected = current.match(NavRoute.Debts),
                onClick  = { onNavigate(NavRoute.Debts) },
            )
            DrawerItem(
                label    = stringResource(R.string.tab_plans),
                icon     = { Icon(Icons.Default.Assignment, contentDescription = null) },
                selected = current.match(NavRoute.Plans),
                onClick  = { onNavigate(NavRoute.Plans) },
            )
            DrawerItem(
                label    = stringResource(R.string.tab_analytics),
                icon     = { Icon(Icons.Default.Analytics, contentDescription = null) },
                selected = current.match(NavRoute.Analytics),
                onClick  = { onNavigate(NavRoute.Analytics) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DrawerItem(
                label    = stringResource(R.string.contracts_title),
                icon     = { Icon(Icons.Default.Folder, contentDescription = null) },
                selected = current.match(NavRoute.Contracts),
                onClick  = { onNavigate(NavRoute.Contracts) },
            )
            DrawerItem(
                label    = stringResource(R.string.settings_title),
                icon     = { Icon(Icons.Default.Settings, contentDescription = null) },
                selected = current.match(NavRoute.Settings),
                onClick  = { onNavigate(NavRoute.Settings) },
            )
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label    = { Text(label) },
        icon     = icon,
        selected = selected,
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics { contentDescription = "$label${if (selected) ", текущий раздел" else ""}" },
    )
}

private fun NavDestination?.match(route: NavRoute): Boolean = when (route) {
    NavRoute.Dashboard          -> this?.hasRoute<NavRoute.Dashboard>()          == true
    NavRoute.Accounts           -> this?.hasRoute<NavRoute.Accounts>()           == true
    NavRoute.Budget             -> this?.hasRoute<NavRoute.Budget>()             == true
    NavRoute.Plans              -> this?.hasRoute<NavRoute.Plans>()              == true
    NavRoute.Analytics          -> this?.hasRoute<NavRoute.Analytics>()          == true
    NavRoute.Debts              -> this?.hasRoute<NavRoute.Debts>()              == true
    NavRoute.Contracts          -> this?.hasRoute<NavRoute.Contracts>()          == true
    NavRoute.Settings           -> this?.hasRoute<NavRoute.Settings>()           == true
    NavRoute.CategoriesSettings -> this?.hasRoute<NavRoute.CategoriesSettings>() == true
    is NavRoute.Spend           -> this?.hasRoute<NavRoute.Spend>()              == true
    is NavRoute.Deposit         -> this?.hasRoute<NavRoute.Deposit>()            == true
    is NavRoute.Receipt         -> this?.hasRoute<NavRoute.Receipt>()            == true
    is NavRoute.Transaction     -> this?.hasRoute<NavRoute.Transaction>()        == true
    is NavRoute.CreateDebt      -> this?.hasRoute<NavRoute.CreateDebt>()          == true
}
