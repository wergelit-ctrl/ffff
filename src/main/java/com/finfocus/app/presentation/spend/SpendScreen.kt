package com.finfocus.app.presentation.spend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.presentation.components.SwipeConfirmButton
import com.finfocus.app.presentation.util.CategoryIcons
import com.finfocus.app.presentation.util.formatByr

/**
 * Экран "Снять" (расход).
 *
 * Задача 2.3: отдельный экран без переключателя доход/расход.
 * Задача 2.4: сумма вверху по центру, нажатие → открывает поле ввода.
 * Задача 2.5: SwipeConfirmButton внизу — вправо подтвердить, влево отменить.
 * Задача 2.7: semantics/contentDescription.
 *
 * @param accountId  id счёта списания
 * @param onConfirm  колбэк (categoryId, amountKopecks) → навигация на ReceiptScreen
 * @param onBack     кнопка назад
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendScreen(
    accountId:  String,
    viewModel:  SpendViewModel = hiltViewModel(),
    onConfirm:  (categoryName: String, amountKopecks: Long) -> Unit = { _, _ -> },
    onBack:     () -> Unit = {},
) {
    val categories    = viewModel.categories.collectAsStateWithLifecycle().value
    val event         = viewModel.event.collectAsStateWithLifecycle().value

    // Навигация на чек после сохранения транзакции
    LaunchedEffect(event) {
        if (event is SpendEvent.Saved) {
            viewModel.consumeEvent()
            onConfirm(event.categoryName, event.amountKopecks)
        }
    }
    val selectedCatId = remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    val amountText    = rememberSaveable { mutableStateOf("") }
    val showInput     = rememberSaveable { mutableStateOf(false) }
    val amountError   = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val cancelDesc     = stringResource(R.string.cancel_action)
    val keyboard       = LocalSoftwareKeyboardController.current

    // При первой загрузке выбираем первую категорию
    LaunchedEffect(categories) {
        if (selectedCatId.value.isEmpty() && categories.isNotEmpty()) {
            selectedCatId.value = categories.first().id
        }
    }

    // Автофокус при открытии поля ввода
    LaunchedEffect(showInput.value) {
        if (showInput.value) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // Парсим сумму: поддерживаем запятую
    fun parsedAmount(): Long? {
        val text = amountText.value.replace(',', '.').trim()
        return text.toDoubleOrNull()?.let { (it * 100).toLong() }
    }

    fun onConfirmAction() {
        val kopecks = parsedAmount()
        if (kopecks == null || kopecks <= 0L) {
            amountError.value = true
            showInput.value   = true
            return
        }
        val catName = categories.firstOrNull { it.id == selectedCatId.value }?.name ?: "Другое"
        keyboard?.hide()
        viewModel.saveExpense(accountId, catName, kopecks)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.action_withdraw),
                        modifier = Modifier.semantics { contentDescription = "Экран списания средств" },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = cancelDesc },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ── Задача 2.4: сумма вверху по центру ──────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            showInput.value = !showInput.value
                            amountError.value = false
                        }
                        .padding(vertical = 20.dp)
                        .semantics { contentDescription = "Сумма: ${amountText.value.ifBlank { "не указана" }}. Нажмите для ввода" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (amountText.value.isBlank()) {
                        Text(
                            text  = stringResource(R.string.spend_tap_to_enter_amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        )
                    } else {
                        Text(
                            text       = amountText.value.replace(',', '.').toDoubleOrNull()
                                ?.let { it.formatByr() } ?: amountText.value,
                            style      = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color      = if (amountError.value)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Поле ввода — появляется при нажатии на сумму
                AnimatedVisibility(
                    visible = showInput.value,
                    enter   = fadeIn() + slideInVertically { -it / 2 },
                ) {
                    OutlinedTextField(
                        value    = amountText.value,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() || it == '.' || it == ',' }
                            if (filtered.count { it == '.' || it == ',' } <= 1) {
                                amountText.value = filtered
                                amountError.value = false
                            }
                        },
                        label   = { Text(stringResource(R.string.amount_byn)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction    = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { showInput.value = false; keyboard?.hide() }
                        ),
                        isError = amountError.value,
                        supportingText = if (amountError.value) {
                            { Text(stringResource(R.string.error_amount_positive)) }
                        } else null,
                        singleLine = true,
                        modifier   = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .semantics { contentDescription = "Поле ввода суммы" },
                    )
                }

                // ── Сетка категорий с иконками ───────────────────────
                Text(
                    text  = stringResource(R.string.spend_choose_category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyVerticalGrid(
                    columns        = GridCells.Fixed(4),
                    modifier       = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .semantics { contentDescription = "Список категорий расходов" },
                    contentPadding = PaddingValues(2.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories, key = { it.id }) { cat ->
                        SpendCategoryCell(
                            category   = cat,
                            isSelected = cat.id == selectedCatId.value,
                            onClick    = { selectedCatId.value = cat.id },
                        )
                    }
                }
            }

            // ── Задача 2.5: свайп-кнопка внизу ──────────────────────
            Column {
                SwipeConfirmButton(
                    onConfirm    = { onConfirmAction() },
                    onCancel     = onBack,
                    labelConfirm = stringResource(R.string.spend_confirm),
                    labelCancel  = stringResource(R.string.cancel_action),
                    modifier     = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun SpendCategoryCell(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val icon = CategoryIcons.get(category.iconKey)
    val bg   = if (isSelected) MaterialTheme.colorScheme.primaryContainer
               else MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .semantics { contentDescription = "${category.name}${if (isSelected) ", выбрано" else ""}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(24.dp),
            tint               = if (isSelected) MaterialTheme.colorScheme.primary
                                 else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text      = category.name,
            style     = MaterialTheme.typography.labelSmall,
            color     = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            fontSize  = 10.sp,
        )
    }
}
