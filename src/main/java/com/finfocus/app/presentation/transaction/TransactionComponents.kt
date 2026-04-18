package com.finfocus.app.presentation.transaction

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.finfocus.app.R

/**
 * Сегментированная кнопка Расход / Доход.
 *
 * @param selected  текущий тип
 * @param onSelect  колбэк при переключении
 * @param modifier  внешний модификатор
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeToggle(
    selected: TxType,
    onSelect: (TxType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = selected == TxType.EXPENSE,
            onClick = { onSelect(TxType.EXPENSE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(stringResource(R.string.tx_type_expense))
        }
        SegmentedButton(
            selected = selected == TxType.INCOME,
            onClick = { onSelect(TxType.INCOME) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(R.string.tx_type_income))
        }
    }
}

/**
 * Поле необязательной заметки к транзакции.
 *
 * @param value    текущий текст
 * @param onChange колбэк при вводе
 * @param modifier внешний модификатор
 */
@Composable
fun NoteField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.tx_note_label)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        maxLines = 3,
        modifier = modifier,
    )
}
