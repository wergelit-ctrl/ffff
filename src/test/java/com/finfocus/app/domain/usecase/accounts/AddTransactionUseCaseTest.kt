package com.finfocus.app.domain.usecase.accounts

import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AccountType
import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.repository.FinFocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AddTransactionUseCaseTest {

    @Test
    fun `income transaction increases account balance`() = runBlocking {
        val repo = FakeRepository(listOf(account("a1", 100.0)))
        val useCase = AddTransactionUseCase(repo)

        useCase("a1", 50.0, "Зарплата", TransactionType.INCOME, null)

        assertEquals(150.0, repo.accountsFlow.value.first().balance, 0.01)
        assertEquals(1, repo.transactionsFlow.value.size)
    }

    @Test
    fun `expense transaction decreases account balance`() = runBlocking {
        val repo = FakeRepository(listOf(account("a1", 200.0)))
        val useCase = AddTransactionUseCase(repo)

        useCase("a1", 80.0, "Еда", TransactionType.EXPENSE, null)

        assertEquals(120.0, repo.accountsFlow.value.first().balance, 0.01)
        assertEquals(1, repo.transactionsFlow.value.size)
    }

    @Test
    fun `transfer out decreases source account balance`() = runBlocking {
        val repo = FakeRepository(listOf(account("a1", 300.0), account("a2", 50.0)))
        val useCase = AddTransactionUseCase(repo)

        useCase("a1", 100.0, "Перевод", TransactionType.TRANSFER_OUT, null)

        val a1 = repo.accountsFlow.value.first { it.id == "a1" }
        assertEquals(200.0, a1.balance, 0.01)
    }

    @Test
    fun `transfer in increases target account balance`() = runBlocking {
        val repo = FakeRepository(listOf(account("a1", 300.0), account("a2", 50.0)))
        val useCase = AddTransactionUseCase(repo)

        useCase("a2", 100.0, "Перевод", TransactionType.TRANSFER_IN, null)

        val a2 = repo.accountsFlow.value.first { it.id == "a2" }
        assertEquals(150.0, a2.balance, 0.01)
    }

    @Test
    fun `transaction for unknown account does not crash or modify balances`() = runBlocking {
        val repo = FakeRepository(listOf(account("a1", 100.0)))
        val useCase = AddTransactionUseCase(repo)

        // T2: ContractRepository silently returns when account not found — fake must mirror this
        useCase("unknown_id", 50.0, "Еда", TransactionType.EXPENSE, null)

        // Balance unchanged, but transaction was still recorded (matches ContractRepository behaviour)
        val a1 = repo.accountsFlow.value.first()
        assertEquals(100.0, a1.balance, 0.01)
    }

    @Test
    fun `note is stored on the transaction`() = runBlocking {
        val repo = FakeRepository(listOf(account("a1", 100.0)))
        val useCase = AddTransactionUseCase(repo)

        useCase("a1", 10.0, "Кафе", TransactionType.EXPENSE, "Кофе с другом")

        assertEquals("Кофе с другом", repo.transactionsFlow.value.first().note)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun account(id: String, balance: Double) =
        Account(id, "Account $id", AccountType.CARD, balance, "", 0L, System.currentTimeMillis(), "c1")
}

/**
 * T2: FakeRepository now overrides addTransactionAndUpdateBalance to mirror
 * ContractRepository's actual logic. The original fake relied on the interface
 * default, which tested different code from what runs in production.
 */
private class FakeRepository(
    accounts: List<Account> = emptyList(),
) : FinFocusRepository {
    val accountsFlow = MutableStateFlow(accounts)
    val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val budgetsFlow = MutableStateFlow<List<Budget>>(emptyList())
    private val debtsFlow = MutableStateFlow<List<Debt>>(emptyList())
    private val wishlistFlow = MutableStateFlow<List<WishItem>>(emptyList())

    override fun observeAccounts(): Flow<List<Account>> = accountsFlow
    override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow
    override fun observeBudgets(): Flow<List<Budget>> = budgetsFlow
    override fun observeDebts(): Flow<List<Debt>> = debtsFlow
    override fun observeWishlist(): Flow<List<WishItem>> = wishlistFlow

    override suspend fun saveInsights(insights: List<InsightCard>) = Unit
    override suspend fun addAccount(account: Account) { accountsFlow.value = accountsFlow.value + account }
    override suspend fun updateAccount(account: Account) {
        accountsFlow.value = accountsFlow.value.map { if (it.id == account.id) account else it }
    }
    override suspend fun deleteAccount(id: String) { accountsFlow.value = accountsFlow.value.filterNot { it.id == id } }
    override suspend fun addTransaction(transaction: Transaction) {
        transactionsFlow.value = transactionsFlow.value + transaction
    }
    override suspend fun deleteTransaction(id: String) {
        transactionsFlow.value = transactionsFlow.value.filterNot { it.id == id }
    }
    override suspend fun addDebt(debt: Debt) { debtsFlow.value = debtsFlow.value + debt }
    override suspend fun updateDebt(debt: Debt) {
        debtsFlow.value = debtsFlow.value.map { if (it.id == debt.id) debt else it }
    }
    override suspend fun deleteDebt(id: String) { debtsFlow.value = debtsFlow.value.filterNot { it.id == id } }
    override suspend fun addWishItem(item: WishItem) { wishlistFlow.value = wishlistFlow.value + item }
    override suspend fun updateWishItem(item: WishItem) {
        wishlistFlow.value = wishlistFlow.value.map { if (it.id == item.id) item else it }
    }
    override suspend fun deleteWishItem(id: String) {
        wishlistFlow.value = wishlistFlow.value.filterNot { it.id == id }
    }
    override suspend fun saveBudgets(budgets: List<Budget>) { budgetsFlow.value = budgets }

    // T2: ContractRepository-style implementation — only updates balance when account is found,
    // mirrors the production guard: if account not found, transaction is still saved but no
    // balance update occurs (matching ContractRepository.addTransactionAndUpdateBalance).
    override suspend fun addTransactionAndUpdateBalance(transaction: Transaction) {
        transactionsFlow.value = transactionsFlow.value + transaction
        val account = accountsFlow.first().firstOrNull { it.id == transaction.accountId } ?: return
        val delta = when (transaction.type) {
            TransactionType.INCOME, TransactionType.TRANSFER_IN -> transaction.amount
            TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> -transaction.amount
        }
        accountsFlow.value = accountsFlow.value.map { existing ->
            if (existing.id == account.id) existing.copy(balance = existing.balance + delta) else existing
        }
    }
}

