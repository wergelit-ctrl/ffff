package com.finfocus.app.domain.model

import kotlinx.serialization.Serializable

// ── Enums ─────────────────────────────────────────────────────

@Serializable enum class AccountType { CARD, WALLET, BANK_ACCOUNT, STASH, OTHER }
@Serializable enum class TransactionType { INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT }
@Serializable enum class DebtType { OWED_TO_ME, I_OWE }
@Serializable enum class DebtStatus { ACTIVE, PARTIALLY_PAID, CLOSED }
@Serializable enum class Priority { HIGH, MEDIUM, LOW }
@Serializable
enum class WishStatus {
    PLANNED,   // Активная мечта / план
    SAVING,    // Накапливаю
    BOUGHT,    // Куплено / выполнено
    UNCERTAIN, // «Под вопросом» — ещё не решил
}
@Serializable enum class InsightType { FORECAST, WARNING, TIP, PATTERN }
@Serializable enum class Severity { LOW, MEDIUM, HIGH }

/**
 * Группа бюджета 50/30/20.
 * NEED = Нужды (50%), WANT = Желания (30%), SAVE = Сбережения (20%)
 */
@Serializable enum class BudgetGroup { NEED, WANT, SAVE, UNSET }

enum class BudgetPeriod { WEEK, MONTH, YEAR }
enum class AnalyticsPeriod { WEEK, MONTH, THREE_MONTHS, YEAR, CUSTOM }

// ── Вспомогательные ───────────────────────────────────────────

data class BarEntry(val label: String, val income: Long, val expense: Long)
data class ContractInfo(val fileName: String, val size: Long, val txCount: Int, val date: Long, val isArchived: Boolean)

// ── Категория расходов ────────────────────────────────────────

/**
 * Пользовательская категория расходов.
 * iconKey — ключ из CategoryIcons (напр. "restaurant").
 * group   — принадлежность к нуждам/желаниям/сбережениям для 50/30/20.
 */
@Serializable
data class ExpenseCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String = "",
    val group: BudgetGroup = BudgetGroup.UNSET,
    val isDefault: Boolean = false,
)

/** Предустановленные категории — создаются при первом запуске. */
object DefaultCategories {
    val list: List<ExpenseCategory> = listOf(
        ExpenseCategory("cat_food",          "Еда",              "restaurant",             group = BudgetGroup.NEED, isDefault = true),
        ExpenseCategory("cat_housing",       "Жильё",            "home",                   group = BudgetGroup.NEED),
        ExpenseCategory("cat_transport",     "Транспорт",        "directions_bus",         group = BudgetGroup.NEED),
        ExpenseCategory("cat_health",        "Здоровье",         "local_hospital",         group = BudgetGroup.NEED),
        ExpenseCategory("cat_education",     "Образование",      "school",                 group = BudgetGroup.NEED),
        ExpenseCategory("cat_clothes",       "Одежда",           "checkroom",              group = BudgetGroup.WANT),
        ExpenseCategory("cat_entertainment", "Развлечения",      "movie",                  group = BudgetGroup.WANT),
        ExpenseCategory("cat_communication", "Связь",            "phone_iphone",           group = BudgetGroup.NEED),
        ExpenseCategory("cat_care",          "Уход за собой",    "spa",                    group = BudgetGroup.WANT),
        ExpenseCategory("cat_gifts",         "Подарки",          "card_giftcard",          group = BudgetGroup.WANT),
        ExpenseCategory("cat_animals",       "Питомцы",          "pets",                   group = BudgetGroup.WANT),
        ExpenseCategory("cat_cafe",          "Кафе",             "local_cafe",             group = BudgetGroup.WANT),
        ExpenseCategory("cat_sport",         "Спорт",            "fitness_center",         group = BudgetGroup.WANT),
        ExpenseCategory("cat_travel",        "Путешествия",      "flight",                 group = BudgetGroup.WANT),
        ExpenseCategory("cat_beauty",        "Красота",          "face",                   group = BudgetGroup.WANT),
        ExpenseCategory("cat_tech",          "Техника",          "devices",                group = BudgetGroup.WANT),
        ExpenseCategory("cat_subscriptions", "Подписки",         "subscriptions",          group = BudgetGroup.WANT),
        ExpenseCategory("cat_medicine",      "Лекарства",        "medication",             group = BudgetGroup.NEED),
        ExpenseCategory("cat_grocery",       "Продукты",         "local_grocery_store",    group = BudgetGroup.NEED),
        ExpenseCategory("cat_taxi",          "Такси",            "local_taxi",             group = BudgetGroup.WANT),
        ExpenseCategory("cat_debt_payment",  "Долги",            "account_balance_wallet", group = BudgetGroup.NEED, isDefault = true),
        ExpenseCategory("cat_savings",       "Сбережения",       "savings",                group = BudgetGroup.SAVE, isDefault = true),
        ExpenseCategory("cat_wish",          "Мечты",            "star",                   group = BudgetGroup.SAVE),
        ExpenseCategory("cat_other",         "Другое",           "more_horiz",             group = BudgetGroup.UNSET, isDefault = true),
    )
}

// ── Настройки приложения (хранятся в контракте) ──────────────

/**
 * Глобальные настройки — сохраняются в JSON-контракте для синхронизации через Syncthing.
 */
@Serializable
data class AppSettings(
    val primaryAccountId: String = "",
    val savingsAccountId: String = "",
    val budgetIncomeAmount: Long = 0L,  // Задача 4.2: копейки
)

// ── Account ───────────────────────────────────────────────────

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Long,  // Задача 4.2: копейки (1 Br = 100)
    val iconKey: String = "wallet",
    val color: Long,
    val createdAt: Long,
    val contractId: String,
)

// ── Transaction ───────────────────────────────────────────────

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val amount: Long,  // Задача 4.2: копейки
    val category: String,
    val type: TransactionType,
    val timestamp: Long,
    val note: String? = null,
)

// ── Budget ────────────────────────────────────────────────────

@Serializable
data class Budget(
    val id: String,
    val method: String,
    val period: String,
    val category: String,
    val limit: Long,  // Задача 4.2: копейки
    val spent: Long,
)

// ── Debt ──────────────────────────────────────────────────────

@Serializable
data class Debt(
    val id: String,
    val personName: String,
    val amount: Long,       // Задача 4.2: копейки
    val paidAmount: Long,
    val type: DebtType,
    val createdAt: Long,
    val dueDate: Long? = null,
    val comment: String = "",
    val status: DebtStatus,
)

// ── WishItem ──────────────────────────────────────────────────

@Serializable
data class WishItem(
    val id: String,
    val name: String,
    val price: Long,          // Задача 4.2: копейки
    val priority: Priority,
    val category: String,
    val comment: String,
    val monthlySaving: Long,  // Задача 4.2: копейки
    val saved: Long,          // Задача 4.2: копейки
    val status: WishStatus,
    /** Задача 4.1: пользовательская категория плана (напр. «Техника», «Путешествия»). */
    val planCategory: String = "",
)

// ── InsightCard ───────────────────────────────────────────────

@Serializable
data class InsightCard(
    val id: String,
    val type: InsightType,
    val icon: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val actionLabel: String? = null,
    val generatedAt: Long,
)

// ── UI ────────────────────────────────────────────────────────

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
