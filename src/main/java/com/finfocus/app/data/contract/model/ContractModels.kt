package com.finfocus.app.data.contract.model

import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AppSettings
import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.WishItem
import kotlinx.serialization.Serializable

/**
 * Основная единица хранения данных FinFocus.
 *
 * Каждое действие пользователя создаёт/обновляет один контракт.
 * При превышении 45 файлов → рекурсивное слияние до одного.
 * Синхронизируется с ПК через Syncthing как обычный JSON-файл.
 *
 * JSON-схема (поля верхнего уровня):
 *   contractVersion  — "1.1"
 *   contractId       — UUID
 *   createdAt        — Unix ms
 *   updatedAt        — Unix ms  ← ключевое поле для выбора "самого нового"
 *   isArchived       — true если контракт объединён в более новый
 *   accounts         — список счетов
 *   transactions     — список транзакций
 *   budgets          — лимиты бюджета
 *   debts            — долги
 *   wishlist         — мечты/желания
 *   insights         — инсайты прогнозирования
 *   categories       — пользовательские категории расходов
 *   settings         — глобальные настройки (primaryAccountId, savingsAccountId, …)
 */
@Serializable
data class ContractEntity(
    val contractVersion: String = "1.1",
    val contractId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val wishlist: List<WishItem> = emptyList(),
    val insights: List<InsightCard> = emptyList(),
    // Новые поля — совместимы со старыми контрактами (default = emptyList/default)
    val categories: List<ExpenseCategory> = emptyList(),
    val settings: AppSettings = AppSettings(),
)
