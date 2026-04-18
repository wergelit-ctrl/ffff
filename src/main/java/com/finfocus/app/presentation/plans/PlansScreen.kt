package com.finfocus.app.presentation.plans

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.Priority
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.model.WishStatus
import com.finfocus.app.presentation.components.EmptyState
import com.finfocus.app.presentation.util.formatByr
import com.finfocus.app.ui.theme.FinFocusTheme

/**
 * Экран планов / мечт.
 *
 * Задача 4.1:
 * - Убран заголовок «Список планов»
 * - Добавлены категории планов (planCategory)
 * - Раздел «Под вопросом» (UNCERTAIN) — отдельная секция
 * - Раздел «Активные» (PLANNED + SAVING) — основная секция
 * - Задача 5.3: состояния dialogs сохраняются через rememberSaveable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(viewModel: PlansViewModel = hiltViewModel()) {
    val items    = viewModel.wishItems.collectAsStateWithLifecycle().value
    val accounts = viewModel.accounts.collectAsStateWithLifecycle().value
    val settings = viewModel.appSettings.collectAsStateWithLifecycle().value

    // Задача 5.3: rememberSaveable сохраняет состояние при повороте
    val showAdd         = rememberSaveable { mutableStateOf(false) }
    val addTarget       = remember { mutableStateOf<WishItem?>(null) }
    val withdrawTarget  = remember { mutableStateOf<WishItem?>(null) }
    val withdrawConfirm = remember { mutableStateOf<WishItem?>(null) }

    // Секции
    val activeItems    = items.filter { it.status == WishStatus.PLANNED || it.status == WishStatus.SAVING }
    val uncertainItems = items.filter { it.status == WishStatus.UNCERTAIN }
    val boughtItems    = items.filter { it.status == WishStatus.BOUGHT }

    // Категории для фильтра
    val allCategories  = (activeItems + uncertainItems).map { it.planCategory }
        .filter { it.isNotBlank() }.distinct().sorted()
    val selectedCat    = rememberSaveable { mutableStateOf("") } // "" = все

    val filteredActive = if (selectedCat.value.isBlank()) activeItems
        else activeItems.filter { it.planCategory == selectedCat.value }
    val filteredUncertain = if (selectedCat.value.isBlank()) uncertainItems
        else uncertainItems.filter { it.planCategory == selectedCat.value }

    Scaffold(
        // Задача 4.1: заголовок убран
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd.value = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.plans_add_title))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Фильтр по категориям ──────────────────────────
            if (allCategories.isNotEmpty()) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        FilterChip(
                            selected = selectedCat.value.isBlank(),
                            onClick  = { selectedCat.value = "" },
                            label    = { Text(stringResource(R.string.filter_all)) },
                        )
                        allCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCat.value == cat,
                                onClick  = { selectedCat.value = cat },
                                label    = { Text(cat) },
                            )
                        }
                    }
                }
            }

            // ── Секция «Активные» ─────────────────────────────
            if (filteredActive.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.plans_section_active),
                        count = filteredActive.size,
                    )
                }
                items(filteredActive, key = { it.id }) { item ->
                    WishItemCard(
                        item         = item,
                        monthsToGoal = viewModel.monthsToGoal(item),
                        onAddSaving  = { addTarget.value = item },
                        onWithdraw   = { withdrawConfirm.value = item },
                        onMarkBought = { viewModel.markBought(item.id) },
                        onDelete     = { viewModel.deleteItem(item.id) },
                    )
                }
            }

            // ── Секция «Под вопросом» ─────────────────────────
            if (filteredUncertain.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = stringResource(R.string.plans_section_uncertain),
                        count = filteredUncertain.size,
                        icon  = { Icon(Icons.Default.QuestionMark, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline) },
                    )
                }
                items(filteredUncertain, key = { it.id }) { item ->
                    WishItemCard(
                        item         = item,
                        monthsToGoal = viewModel.monthsToGoal(item),
                        onAddSaving  = { addTarget.value = item },
                        onWithdraw   = { withdrawConfirm.value = item },
                        onMarkBought = { viewModel.markBought(item.id) },
                        onDelete     = { viewModel.deleteItem(item.id) },
                        isUncertain  = true,
                    )
                }
            }

            // ── Купленные — компактно ─────────────────────────
            if (boughtItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title    = stringResource(R.string.status_bought),
                        count    = boughtItems.size,
                        subdued  = true,
                    )
                }
                items(boughtItems, key = { it.id }) { item ->
                    WishItemCard(
                        item         = item,
                        monthsToGoal = 0,
                        onAddSaving  = {},
                        onWithdraw   = {},
                        onMarkBought = {},
                        onDelete     = { viewModel.deleteItem(item.id) },
                    )
                }
            }

            if (filteredActive.isEmpty() && filteredUncertain.isEmpty() && boughtItems.isEmpty()) {
                item { EmptyState(stringResource(R.string.empty_wishlist)) }
            }
        }
    }

    // ── Диалог предупреждения перед снятием ───────────────────
    withdrawConfirm.value?.let { item ->
        AlertDialog(
            onDismissRequest = { withdrawConfirm.value = null },
            title = { Text(stringResource(R.string.plans_withdraw_warning_title)) },
            text  = { Text(stringResource(R.string.plans_withdraw_warning_body,
                item.name, item.saved.formatByr())) },
            confirmButton = {
                TextButton(onClick = {
                    withdrawTarget.value = item
                    withdrawConfirm.value = null
                }) { Text(stringResource(R.string.plans_withdraw_confirm),
                    color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { withdrawConfirm.value = null }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }

    if (showAdd.value) {
        AddWishSheet(
            onDismiss = { showAdd.value = false },
            onAdd     = { name, price, priority, category, comment, monthly, planCat, isUncertain ->
                viewModel.addItem(name, price, priority, category, comment, monthly, planCat, isUncertain)
            },
        )
    }

    addTarget.value?.let { item ->
        SavingSheet(
            title     = stringResource(R.string.plans_add_saving_for, item.name),
            accounts  = accounts,
            primaryId = settings.primaryAccountId,
            onDismiss = { addTarget.value = null },
            onSubmit  = { amount, accountId ->
                viewModel.addSaving(item.id, amount, accountId)
                addTarget.value = null
            },
        )
    }

    withdrawTarget.value?.let { item ->
        SavingSheet(
            title     = stringResource(R.string.plans_withdraw_for, item.name),
            accounts  = accounts,
            primaryId = settings.primaryAccountId,
            maxAmount = item.saved,  // Long kopecks
            onDismiss = { withdrawTarget.value = null },
            onSubmit  = { amount, accountId ->
                viewModel.withdrawSaving(item.id, amount, accountId)
                withdrawTarget.value = null
            },
        )
    }
}

// ── Секция-заголовок ──────────────────────────────────────────

@Composable
private fun SectionHeader(
    title:   String,
    count:   Int,
    subdued: Boolean = false,
    icon:    (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        icon?.invoke()
        Text(
            text  = "$title ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = if (subdued) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (subdued) FontWeight.Normal else FontWeight.SemiBold,
        )
    }
}

// ── WishItemCard ──────────────────────────────────────────────

@Composable
fun WishItemCard(
    item: WishItem,
    monthsToGoal: Int,
    onAddSaving: () -> Unit,
    onWithdraw: () -> Unit,
    onMarkBought: () -> Unit,
    onDelete: () -> Unit,
    isUncertain: Boolean = false,
) {
    val progress = if (item.price > 0)
        (item.saved / item.price).toFloat().coerceIn(0f, 1f) else 0f
    val isBought = item.status == WishStatus.BOUGHT

    val containerColor = when {
        isBought    -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        isUncertain -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else        -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    val subtitle = listOf(item.planCategory, item.category)
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                PriorityStars(priority = item.priority)
            }

            if (!isBought && item.price > 0) {
                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier.fillMaxWidth(),
                    color      = when {
                        isUncertain  -> MaterialTheme.colorScheme.outline
                        progress >= 1f -> MaterialTheme.colorScheme.primary
                        progress >= .5f -> MaterialTheme.colorScheme.tertiary
                        else          -> MaterialTheme.colorScheme.secondary
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isBought) {
                    Text(stringResource(R.string.plans_status_bought_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium)
                } else if (item.price > 0) {
                    Column {
                        Text("${item.saved.formatByr()} / ${item.price.formatByr()}",
                            style = MaterialTheme.typography.bodyMedium)
                        if (monthsToGoal > 0)
                            Text(stringResource(R.string.plans_months_to_goal, monthsToGoal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            if (item.comment.isNotBlank())
                Text(item.comment, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!isBought) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
                    if (item.saved > 0 && !isUncertain) {
                        IconButton(onClick = onWithdraw, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error) } }
                    if (!isUncertain) {
                        IconButton(onClick = onAddSaving, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.tertiary) }
                        IconButton(onClick = onMarkBought, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityStars(priority: Priority) {
    val count = when (priority) { Priority.HIGH -> 3; Priority.MEDIUM -> 2; Priority.LOW -> 1 }
    Row {
        repeat(3) { i ->
            Icon(
                imageVector = if (i < count) Icons.Default.Star else Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = when (priority) {
                    Priority.HIGH   -> MaterialTheme.colorScheme.error
                    Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    Priority.LOW    -> MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

// ── AddWishSheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWishSheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, price: Long, priority: Priority, category: String,
            comment: String, monthlySaving: Long, planCategory: String, isUncertain: Boolean) -> Unit,
) {
    val name        = rememberSaveable { mutableStateOf("") }
    val price       = rememberSaveable { mutableStateOf("") }
    val monthly     = rememberSaveable { mutableStateOf("") }
    val category    = rememberSaveable { mutableStateOf("") }
    val planCat     = rememberSaveable { mutableStateOf("") }
    val comment     = rememberSaveable { mutableStateOf("") }
    val priority    = rememberSaveable { mutableStateOf(Priority.MEDIUM) }
    val isUncertain = rememberSaveable { mutableStateOf(false) }
    val nameError   = rememberSaveable { mutableStateOf(false) }
    val priceError  = rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.plans_add_title), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = name.value, onValueChange = { name.value = it; nameError.value = false },
                label = { Text(stringResource(R.string.plans_name_label)) },
                isError = nameError.value,
                supportingText = if (nameError.value) { { Text(stringResource(R.string.error_name_required)) } } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )

            // Категория плана (пользовательская, например "Техника")
            OutlinedTextField(
                value = planCat.value, onValueChange = { planCat.value = it },
                label = { Text(stringResource(R.string.plans_plan_category_label)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price.value, onValueChange = { price.value = it; priceError.value = false },
                    label = { Text(stringResource(R.string.plans_price_label)) },
                    isError = priceError.value,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = monthly.value, onValueChange = { monthly.value = it },
                    label = { Text(stringResource(R.string.plans_monthly_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), singleLine = true,
                )
            }

            OutlinedTextField(
                value = comment.value, onValueChange = { comment.value = it },
                label = { Text(stringResource(R.string.comment_optional)) },
                modifier = Modifier.fillMaxWidth(), maxLines = 2,
            )

            // «Под вопросом» — переключатель
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Switch(
                    checked = isUncertain.value,
                    onCheckedChange = { isUncertain.value = it },
                )
                Text(stringResource(R.string.plans_mark_uncertain),
                    style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider()

            Text(stringResource(R.string.plans_priority_label), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    val label = when (p) {
                        Priority.HIGH   -> stringResource(R.string.priority_high)
                        Priority.MEDIUM -> stringResource(R.string.priority_medium)
                        Priority.LOW    -> stringResource(R.string.priority_low)
                    }
                    FilterChip(selected = priority.value == p,
                        onClick = { priority.value = p }, label = { Text(label) })
                }
            }

            Button(
                onClick = {
                    var err = false
                    if (name.value.isBlank()) { nameError.value = true; err = true }
                    val parsedPrice = price.value.replace(',', '.').toDoubleOrNull()
                    if (!isUncertain.value && (parsedPrice == null || parsedPrice <= 0.0)) {
                        priceError.value = true; err = true
                    }
                    if (err) return@Button
                    onAdd(
                        name.value.trim(),
                        ((parsedPrice ?: 0.0) * 100).toLong(),
                        priority.value,
                        category.value.trim(),
                        comment.value.trim(),
                        ((monthly.value.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toLong(),
                        planCat.value.trim(),
                        isUncertain.value,
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.add_action)) }
        }
    }
}

// ── SavingSheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingSheet(
    title: String,
    accounts: List<com.finfocus.app.domain.model.Account>,
    primaryId: String,
    maxAmount: Long = Long.MAX_VALUE,
    onDismiss: () -> Unit,
    onSubmit: (amount: Long, accountId: String?) -> Unit,
) {
    val amount       = rememberSaveable { mutableStateOf("") }
    val selectedAcct = remember { mutableStateOf(primaryId.ifBlank { accounts.firstOrNull()?.id }) }
    val error        = rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (accounts.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acct ->
                        FilterChip(selected = selectedAcct.value == acct.id,
                            onClick = { selectedAcct.value = acct.id },
                            label   = { Text("${acct.name} · ${acct.balance.formatByr()}") })
                    }
                }
            }
            OutlinedTextField(
                value = amount.value, onValueChange = { amount.value = it; error.value = false },
                label = { Text(stringResource(R.string.amount_byn)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error.value,
                supportingText = if (error.value) { { Text(stringResource(R.string.error_amount_positive)) } } else null,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Button(
                onClick = {
                    val p = amount.value.replace(',', '.').toDoubleOrNull()
                    if (p == null || p <= 0.0) { error.value = true; return@Button }
                    onSubmit(((p * 100).toLong()).coerceAtMost(maxAmount), selectedAcct.value)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.apply_action)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlansPreview() { FinFocusTheme { PlansScreen() } }
