package com.finfocus.app.domain.repository

import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AppSettings
import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface FinFocusRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeBudgets(): Flow<List<Budget>>
    fun observeDebts(): Flow<List<Debt>>
    fun observeWishlist(): Flow<List<WishItem>>
    fun observeCategories(): Flow<List<ExpenseCategory>>
    fun observeSettings(): Flow<AppSettings>

    suspend fun saveInsights(insights: List<InsightCard>)
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: String)
    suspend fun addTransaction(transaction: Transaction)
    suspend fun addTransactionAndUpdateBalance(transaction: Transaction) {
        addTransaction(transaction)
        val account = observeAccounts().first().firstOrNull { it.id == transaction.accountId } ?: return
        val delta = when (transaction.type) {
            TransactionType.INCOME, TransactionType.TRANSFER_IN -> transaction.amount
            TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> -transaction.amount
        }
        updateAccount(account.copy(balance = account.balance + delta))
    }
    suspend fun deleteTransaction(id: String)
    suspend fun addDebt(debt: Debt)
    suspend fun updateDebt(debt: Debt)
    suspend fun deleteDebt(id: String)
    suspend fun addWishItem(item: WishItem)
    suspend fun updateWishItem(item: WishItem)
    suspend fun deleteWishItem(id: String)
    suspend fun saveBudgets(budgets: List<Budget>)
    suspend fun saveCategories(categories: List<ExpenseCategory>)
    suspend fun saveSettings(settings: AppSettings)

    /**
     * Задача 3.6: атомарный перевод между двумя счетами.
     * Обе транзакции (TRANSFER_OUT + TRANSFER_IN) и оба обновления баланса
     * записываются в рамках одного saveContract — нет рассинхронизации при сбое.
     */
    suspend fun atomicTransfer(txOut: Transaction, txIn: Transaction)
}
