package com.finfocus.app.domain.usecase.prediction

import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.InsightType
import com.finfocus.app.domain.model.Severity
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.presentation.util.toRubles
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PredictionEngine @Inject constructor() {
    private val zone = ZoneId.systemDefault()

    suspend fun generateInsights(
        transactions: List<Transaction>,
        debts: List<Debt>,
        wishes: List<WishItem>,
        currentBalance: Long,
        budgets: List<Budget>,
    ): List<InsightCard> {
        return withContext(Dispatchers.Default) {
            val currentBalanceD = currentBalance.toRubles()
            val now = System.currentTimeMillis()
            val insights = mutableListOf<InsightCard>()
            val thisMonth = ZonedDateTime.now(zone).withDayOfMonth(1)
            val lastMonthStart = thisMonth.minusMonths(1)

            val recent30 = transactions.filter { it.timestamp >= now - 30L * 24 * 60 * 60 * 1000 }
            if (transactions.size >= 7) {
                val avgDailyExpense = recent30.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } / 30.0
                val forecast14 = currentBalanceD - avgDailyExpense * 14
                insights += insight("Прогноз баланса", "Через 14 дней останется ~${forecast14.toInt()} Br", InsightType.FORECAST)
                if (forecast14 < currentBalanceD * 0.2) {
                    insights += insight("Низкий остаток", "Прогноз ниже 20% текущего баланса", InsightType.WARNING, Severity.HIGH)
                }
            }

            val foodThis = monthSum(transactions, thisMonth, "Еда")
            val foodLast = monthSum(transactions, lastMonthStart, "Еда")
            if (foodLast > 0) {
                val percent = (foodThis.toDouble() - foodLast.toDouble()) / foodLast.toDouble() * 100
                if (percent > 10) insights += insight("Перерасход по еде", "Еда +${percent.toInt()}% к прошлому месяцу", InsightType.WARNING)
            }

            val transportMonths = (0..2).map { monthSum(transactions, thisMonth.minusMonths(it.toLong()), "Транспорт").toDouble() }
            val transportAvg = transportMonths.average()
            if (transportAvg > 0) {
                val std = stdDev(transportMonths)
                if (std < transportAvg * 0.15) insights += insight("Транспорт стабилен", "Расходы на транспорт стабильны", InsightType.PATTERN)
            }

            budgets.firstOrNull { it.spent > it.limit }?.let { exceeded ->
                budgets.firstOrNull { b -> b.limit > b.spent && b.id != exceeded.id }?.let { spare ->
                    val move = minOf(exceeded.spent - exceeded.limit, spare.limit - spare.spent)
                    insights += insight("Перераспределение", "Перенести ${move.toRubles().toInt()} Br из '${spare.category}' в '${exceeded.category}'", InsightType.TIP)
                }
            }

            wishes.filter { it.monthlySaving > 0 && it.saved < it.price }
                .minByOrNull { (it.price - it.saved) / it.monthlySaving }
                ?.let {
                    val months = ceil((it.price.toDouble() - it.saved.toDouble()) / it.monthlySaving.toDouble()).toInt().coerceAtLeast(1)
                    insights += insight("Цель достижима", "${it.name} можно купить через $months мес", InsightType.TIP)
                }

            val monthTransactions = transactions.filter { it.timestamp >= thisMonth.toInstant().toEpochMilli() }
            val income = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            if (income > 0) {
                val rate = (income.toDouble() - expense.toDouble()) / income.toDouble() * 100
                if (rate < 20) insights += insight("Норма сбережений", "Сбережения ${rate.toInt()}% (рекомендуется ≥20%)", InsightType.WARNING)
            }

            val byDay = monthTransactions.filter { it.type == TransactionType.EXPENSE }
                .groupBy { dayOfWeek(it.timestamp) }
                .mapValues { it.value.sumOf { t -> t.amount } }
            byDay.maxByOrNull { it.value }?.let { insights += insight("День трат", "Больше всего трат по ${it.key.asRu()}", InsightType.PATTERN) }

            val byCategory = monthTransactions.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { t -> t.amount } }
            val totalExpense = byCategory.values.sum()
            if (totalExpense > 0) {
                byCategory.maxByOrNull { it.value }?.let {
                    val percent = it.value.toDouble() / totalExpense.toDouble() * 100
                    insights += insight("Основная категория", "${it.key}: ${percent.toInt()}% расходов", InsightType.PATTERN)
                }
            }

            val over3 = budgets.firstOrNull { budget ->
                (0..2).all { monthIndex ->
                    val m = thisMonth.minusMonths(monthIndex.toLong())
                    monthCategoryExpense(transactions, m, budget.category) > budget.limit
                }
            }
            over3?.let { over ->
                val avgOver = (0..2).map { monthIndex ->
                    monthCategoryExpense(transactions, thisMonth.minusMonths(monthIndex.toLong()), over.category) - over.limit
                }.average()
                insights += insight("Систематический перерасход", "${over.category}: в среднем +${(avgOver / 100.0).toInt()} Br", InsightType.WARNING)
            }

            debts.filter { it.dueDate != null && it.status != DebtStatus.CLOSED }
                .minByOrNull { it.dueDate!! }?.let {
                val days = ((it.dueDate!! - now) / (24 * 60 * 60 * 1000)).toInt()
                when {
                    days < 0 -> insights += insight(
                        "Просроченный долг",
                        "${it.personName}: ${(it.amount - it.paidAmount).toRubles().toInt()} Br просрочен на ${-days} дн.",
                        InsightType.WARNING,
                        Severity.HIGH,
                    )
                    days <= 7 -> insights += insight(
                        "Ближайший долг",
                        "${it.personName}: ${(it.amount - it.paidAmount).toRubles().toInt()} Br через $days дн.",
                        InsightType.WARNING,
                    )
                }
            }

            val iOwe = debts.filter { it.type == DebtType.I_OWE }.sumOf { it.amount - it.paidAmount }
            val avgRepay = monthTransactions.filter { it.category == "Возврат долга" }.sumOf { it.amount }.toDouble()
            if (iOwe > 0 && avgRepay > 0) {
                val months = ceil(iOwe.toDouble() / avgRepay).toInt()
                insights += insight("Прогноз выплаты долга", "Закроешь долги примерно за $months мес", InsightType.FORECAST)
            }

            val weekStart = ZonedDateTime.now(zone).with(DayOfWeek.MONDAY).toInstant().toEpochMilli()
            val lastWeekStart = weekStart - 7L * 24 * 60 * 60 * 1000
            val thisWeekIncome = transactions.filter { it.type == TransactionType.INCOME && it.timestamp >= weekStart }.sumOf { it.amount }
            val lastWeekIncome = transactions.filter { it.type == TransactionType.INCOME && it.timestamp in lastWeekStart until weekStart }.sumOf { it.amount }
            if (lastWeekIncome > 0) {
                val growth = (thisWeekIncome.toDouble() - lastWeekIncome.toDouble()) / lastWeekIncome.toDouble() * 100
                if (growth > 5) insights += insight("Рост дохода", "Доход вырос на ${growth.toInt()}%", InsightType.PATTERN)
            }

            val evening = transactions.filter { it.type == TransactionType.EXPENSE && hour(it.timestamp) in 20..23 }
            if (evening.isNotEmpty()) {
                val ratio = evening.count { it.category in listOf("Развлечения", "Кафе", "Одежда") }.toDouble() / evening.size
                if (ratio > 0.3) insights += insight("Импульсные покупки", "Вечерние импульсные траты ${(ratio * 100).toInt()}%", InsightType.PATTERN)
            }

            val avgMonthExpense = (0..2).map { month ->
                val start = thisMonth.minusMonths(month.toLong())
                transactions.filter { it.type == TransactionType.EXPENSE && it.timestamp >= start.toInstant().toEpochMilli() && it.timestamp < start.plusMonths(1).toInstant().toEpochMilli() }
                    .sumOf { it.amount }
            }.average()
            val reserve = avgMonthExpense * 3
            if (currentBalanceD < reserve && reserve > 0) {
                insights += insight("Подушка безопасности", "До резерва не хватает ${(reserve - currentBalanceD).toInt()} Br", InsightType.TIP)
            }

            return@withContext insights.take(15)
        }
    }

    private fun insight(title: String, description: String, type: InsightType, severity: Severity = Severity.MEDIUM) =
        InsightCard(UUID.randomUUID().toString(), type, "AutoAwesome", title, description, severity, if (type == InsightType.TIP) "Открыть" else null, System.currentTimeMillis())

    private fun dayOfWeek(ts: Long): DayOfWeek = Instant.ofEpochMilli(ts).atZone(zone).dayOfWeek
    private fun hour(ts: Long): Int = Instant.ofEpochMilli(ts).atZone(zone).hour
    private fun monthSum(transactions: List<Transaction>, monthStart: ZonedDateTime, category: String): Long {
        val start = monthStart.toInstant().toEpochMilli()
        val end = monthStart.plusMonths(1).toInstant().toEpochMilli()
        return transactions.filter { it.type == TransactionType.EXPENSE && it.category == category && it.timestamp in start until end }.sumOf { it.amount }
    }

    private fun monthCategoryExpense(transactions: List<Transaction>, monthStart: ZonedDateTime, category: String): Long = monthSum(transactions, monthStart, category)

    private fun stdDev(values: List<Double>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean).pow(2) } / values.size)
    }

    private fun DayOfWeek.asRu(): String = when (this) {
        DayOfWeek.MONDAY -> "понедельникам"
        DayOfWeek.TUESDAY -> "вторникам"
        DayOfWeek.WEDNESDAY -> "средам"
        DayOfWeek.THURSDAY -> "четвергам"
        DayOfWeek.FRIDAY -> "пятницам"
        DayOfWeek.SATURDAY -> "субботам"
        DayOfWeek.SUNDAY -> "воскресеньям"
    }
}
