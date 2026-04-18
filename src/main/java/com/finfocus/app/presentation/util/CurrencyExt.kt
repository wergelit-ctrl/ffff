package com.finfocus.app.presentation.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Форматирует копейки (Long) в строку «12,50 Br».
 * Задача 4.2: все денежные значения хранятся в копейках.
 */
fun Long.formatByr(): String {
    val byn = this / 100.0
    val symbols = DecimalFormatSymbols(Locale("ru", "BY")).apply {
        decimalSeparator = ','
        groupingSeparator = ' '
    }
    val fmt = DecimalFormat("#,##0.00", symbols)
    return "${fmt.format(byn)} Br"
}

/** Конвертация Double (рубли) → Long (копейки). */
fun Double.toKopecks(): Long = (this * 100).toLong()

/** Конвертация Long (копейки) → Double (рубли). */
fun Long.toRubles(): Double = this / 100.0

/**
 * Backward-compat: Double.formatByr() — конвертирует рубли в копейки перед форматированием.
 * Используется в местах где сумма ещё хранится как Double (переходный период).
 */
fun Double.formatByr(): String = toKopecks().formatByr()
