package com.finfocus.app.domain.usecase.debts

import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.repository.FinFocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettleDebtUseCaseTest {
    @Test
    fun `partial payment sets partially paid status`() = runBlocking {
        val debt = Debt("d1", "Петя", 100.0, 0.0, DebtType.I_OWE, System.currentTimeMillis(), null, "", DebtStatus.ACTIVE)
        val repo = DebtFakeRepository(listOf(debt))
        val useCase = SettleDebtUseCase(repo)

        useCase("d1", 40.0)

        assertEquals(DebtStatus.PARTIALLY_PAID, repo.debts.value.first().status)
        assertEquals(40.0, repo.debts.value.first().paidAmount, 0.01)
    }
}

private class DebtFakeRepository(initialDebts: List<Debt>) : FinFocusRepository {
    val debts = MutableStateFlow(initialDebts)
    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(emptyList())
    override fun observeTransactions(): Flow<List<Transaction>> = MutableStateFlow(emptyList())
    override fun observeBudgets(): Flow<List<Budget>> = MutableStateFlow(emptyList())
    override fun observeDebts(): Flow<List<Debt>> = debts
    override fun observeWishlist(): Flow<List<WishItem>> = MutableStateFlow(emptyList())

    override suspend fun saveInsights(insights: List<InsightCard>) = Unit
    override suspend fun addAccount(account: Account) = Unit
    override suspend fun updateAccount(account: Account) = Unit
    override suspend fun deleteAccount(id: String) = Unit
    override suspend fun addTransaction(transaction: Transaction) = Unit
    override suspend fun deleteTransaction(id: String) = Unit
    override suspend fun addDebt(debt: Debt) = Unit
    override suspend fun updateDebt(debt: Debt) { debts.value = debts.value.map { if (it.id == debt.id) debt else it } }
    override suspend fun deleteDebt(id: String) = Unit
    override suspend fun addWishItem(item: WishItem) = Unit
    override suspend fun updateWishItem(item: WishItem) = Unit
    override suspend fun deleteWishItem(id: String) = Unit
    override suspend fun saveBudgets(budgets: List<Budget>) = Unit
}
