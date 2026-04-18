package com.finfocus.app.presentation.deposit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.finfocus.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.presentation.components.SwipeConfirmButton
import com.finfocus.app.presentation.util.formatByr

/**
 * Экран "Пополнить" (доход).
 *
 * Задача 2.3: отдельный экран, без переключателя.
 * Задача 2.4: сумма вверху по центру, нажатие → поле ввода.
 * Задача 2.5: SwipeConfirmButton внизу.
 * Задача 2.7: semantics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    accountId: String,
    viewModel: DepositViewModel = hiltViewModel(),
    onConfirm: (categoryName: String, amountKopecks: Long) -> Unit = { _, _ -> },
    onBack:    () -> Unit = {},
) {
    val incomeCategories = listOf(
        stringResource(R.string.income_type_salary),
        stringResource(R.string.income_type_scholarship),
        stringResource(R.string.income_type_service),
        stringResource(R.string.income_type_gift),
        stringResource(R.string.income_type_debt_return),
        stringResource(R.string.income_type_sale),
        stringResource(R.string.income_type_other),
    )

    val event = viewModel.event.collectAsStateWithLifecycle().value

    LaunchedEffect(event) {
        if (event is DepositEvent.Saved) {
            viewModel.consumeEvent()
            onConfirm(event.categoryName, event.amountKopecks)
        }
    }

    val selectedCat  = remember { mutableStateOf(incomeCategories.first()) }
    val amountText   = rememberSaveable { mutableStateOf("") }
    val showInput    = rememberSaveable { mutableStateOf(false) }
    val amountError  = remember { mutableStateOf(false) }
    val focusReq     = remember { FocusRequester() }
    val keyboard     = LocalSoftwareKeyboardController.current

    LaunchedEffect(showInput.value) {
        if (showInput.value) { focusReq.requestFocus(); keyboard?.show() }
    }

    fun parsedKopecks(): Long? =
        amountText.value.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() }

    fun onConfirmAction() {
        val kopecks = parsedKopecks()
        if (kopecks == null || kopecks <= 0L) {
            amountError.value = true
            showInput.value   = true
            return
        }
        keyboard?.hide()
        viewModel.saveDeposit(accountId, selectedCat.value, kopecks)
    }

    val cancelDesc = stringResource(R.string.cancel_action)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.action_deposit),
                        modifier = Modifier.semantics { contentDescription = "Экран пополнения счёта" },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = cancelDesc }) {
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

                // ── Сумма вверху по центру (2.4) ─────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .clickable { showInput.value = !showInput.value; amountError.value = false }
                        .padding(vertical = 20.dp)
                        .semantics { contentDescription = "Сумма: ${amountText.value.ifBlank { "не указана" }}. Нажмите для ввода" },
                    contentAlignment = Alignment.Center,
                ) {
                    if (amountText.value.isBlank()) {
                        Text(
                            text  = stringResource(R.string.spend_tap_to_enter_amount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                        )
                    } else {
                        Text(
                            text       = amountText.value.replace(',', '.').toDoubleOrNull()?.formatByr() ?: amountText.value,
                            style      = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color      = if (amountError.value) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }

                AnimatedVisibility(visible = showInput.value, enter = fadeIn() + slideInVertically { -it / 2 }) {
                    OutlinedTextField(
                        value = amountText.value,
                        onValueChange = { v ->
                            val f = v.filter { it.isDigit() || it == '.' || it == ',' }
                            if (f.count { it == '.' || it == ',' } <= 1) {
                                amountText.value = f; amountError.value = false
                            }
                        },
                        label           = { Text(stringResource(R.string.amount_byn)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { showInput.value = false; keyboard?.hide() }),
                        isError         = amountError.value,
                        supportingText  = if (amountError.value) { { Text(stringResource(R.string.error_amount_positive)) } } else null,
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth().focusRequester(focusReq)
                            .semantics { contentDescription = "Поле ввода суммы" },
                    )
                }

                // ── Выбор категории дохода ────────────────────────────
                Text(
                    text  = stringResource(R.string.deposit_choose_category),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.semantics { contentDescription = "Категории дохода" },
                ) {
                    incomeCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat.value == cat,
                            onClick  = { selectedCat.value = cat },
                            label    = { Text(cat) },
                            modifier = Modifier.semantics { contentDescription = "$cat${if (selectedCat.value == cat) ", выбрано" else ""}" },
                        )
                    }
                }
            }

            // ── Свайп-кнопка внизу (2.5) ─────────────────────────────
            SwipeConfirmButton(
                onConfirm    = { onConfirmAction() },
                onCancel     = onBack,
                labelConfirm = stringResource(R.string.deposit_confirm),
                labelCancel  = stringResource(R.string.cancel_action),
                modifier     = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
        }
    }
}
