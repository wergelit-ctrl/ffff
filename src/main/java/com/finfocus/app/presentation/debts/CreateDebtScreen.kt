package com.finfocus.app.presentation.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finfocus.app.R
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.presentation.util.toKopecks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экран создания нового долга.
 *
 * Задача 3.5: вынесен на отдельный экран с полноценным UI.
 * @param isIOwe  true = я должен, false = мне должны
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDebtScreen(
    isIOwe:    Boolean,
    viewModel: DebtViewModel = hiltViewModel(),
    onBack:    () -> Unit = {},
    onSaved:   () -> Unit = {},
) {
    val person         = remember { mutableStateOf("") }
    val amount         = remember { mutableStateOf("") }
    val comment        = remember { mutableStateOf("") }
    val dueDate        = remember { mutableStateOf<Long?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }
    val personError    = remember { mutableStateOf(false) }
    val amountError    = remember { mutableStateOf(false) }
    val fmt            = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val cancelDesc     = stringResource(R.string.cancel_action)

    val title = if (isIOwe)
        stringResource(R.string.debt_create_i_owe_title)
    else
        stringResource(R.string.debt_create_owed_to_me_title)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Подсказка что означает тип долга
            Text(
                text  = if (isIOwe) stringResource(R.string.debt_hint_i_owe)
                        else stringResource(R.string.debt_hint_owed_to_me),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = person.value,
                onValueChange = { person.value = it; personError.value = false },
                label = { Text(stringResource(R.string.person_name)) },
                isError = personError.value,
                supportingText = if (personError.value) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else null,
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true,
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
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value         = comment.value,
                onValueChange = { comment.value = it },
                label         = { Text(stringResource(R.string.comment_optional)) },
                modifier      = Modifier.fillMaxWidth(),
                maxLines      = 3,
            )

            // Срок (необязательный)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = { showDatePicker.value = true }) {
                    Text(stringResource(R.string.debt_set_due_date))
                }
                dueDate.value?.let { ts ->
                    Text(
                        text  = fmt.format(Date(ts)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    TextButton(onClick = { dueDate.value = null }) {
                        Text("×", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    var err = false
                    if (person.value.isBlank()) { personError.value = true; err = true }
                    val parsed = amount.value.replace(',', '.').toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) { amountError.value = true; err = true }
                    if (err) return@Button
                    viewModel.addDebt(
                        personName = person.value.trim(),
                        amount     = (parsed!! * 100).toLong(),
                        type       = if (isIOwe) DebtType.I_OWE else DebtType.OWED_TO_ME,
                        dueDate    = dueDate.value,
                        comment    = comment.value.trim(),
                    )
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_action))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker.value) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate.value)
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
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
