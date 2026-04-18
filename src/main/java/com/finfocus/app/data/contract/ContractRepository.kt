package com.finfocus.app.data.contract

import com.finfocus.app.data.contract.model.ContractEntity
import com.finfocus.app.data.json.JsonDataSource
import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AppSettings
import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DefaultCategories
import com.finfocus.app.domain.model.ExpenseCategory
import com.finfocus.app.domain.model.InsightCard
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.repository.FinFocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_CONTRACTS     = 45
private const val MAX_TX_PER_FILE   = 500
private const val MAX_FILE_BYTES    = 1024 * 1024 // 1 МБ

@Singleton
class ContractRepository @Inject constructor(
    private val jsonDataSource: JsonDataSource,
    private val contractMerger: ContractMerger,
) : FinFocusRepository {

    /** Задача 4.6: true пока initialize() не завершился. */
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    /** Задача 4.5: состояние ошибки инициализации. null = нет ошибки. */
    private val _initError = MutableStateFlow<String?>(null)
    val initError = _initError.asStateFlow()

    private val contracts      = MutableStateFlow<List<ContractEntity>>(emptyList())
    private val mutationMutex  = Mutex()

    /**
     * Отдельный scope для фоновых операций (ротация/слияние),
     * которые запускаются ПОСЛЕ выхода из mutationMutex.
     * Задача 1.2: разрывает цепочку вложенных вызовов.
     */
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Инициализация ─────────────────────────────────────────

    /** Задача 4.5: вызывается из FinFocusApp при ошибке инициализации. */
    fun reportInitError(message: String) {
        _initError.value = message
        _isLoading.value = false
    }

    suspend fun initialize() {
        var loaded = jsonDataSource.loadContracts()

        val hasCategories = loaded.any { it.categories.isNotEmpty() }
        if (!hasCategories) {
            loaded = loaded.toMutableList().also { list ->
                val idx = list.indexOfFirst { !it.isArchived }
                if (idx >= 0) {
                    val updated = list[idx].copy(
                        categories = DefaultCategories.list,
                        updatedAt  = System.currentTimeMillis(),
                    )
                    list[idx] = updated
                    jsonDataSource.saveContract(updated)
                }
            }
        }
        contracts.value = loaded

        // Слияние при превышении порога — в фоне, вне mutex
        scheduleCheckMerge()
        _initError.value = null  // Сброс ошибки при успешной инициализации
        _isLoading.value = false
    }

    // ── Контракты ─────────────────────────────────────────────

    fun observeContracts(): Flow<List<ContractEntity>> = contracts

    /**
     * Задача 1.1: атомарное слияние.
     * 1. Мержим всё в память
     * 2. Записываем merged-файл атомарно (tmp → rename)
     * 3. Удаляем старые ТОЛЬКО после успешной записи
     */
    suspend fun mergeAllContracts() {
        mutationMutex.withLock {
            val current = contracts.value
            if (current.isEmpty()) return@withLock
            val merged = contractMerger.merge(current)
            // Задача 1.1: сначала записываем новый файл
            jsonDataSource.saveMergedContract(merged)
            // Задача 1.1: только после успеха удаляем старые
            jsonDataSource.removeAllContracts()
            // Сохраняем merged под постоянным именем
            jsonDataSource.saveContract(merged)
            contracts.value = listOf(merged)
        }
    }

    /**
     * Задача 1.2: ротация НЕ вызывается внутри mutationMutex.
     * После мутации данных мы выходим из lock, затем в bgScope запускаем ротацию.
     */
    suspend fun createNewContract() {
        // Задача 1.4 ротации: перенос budgets + categories + settings
        val active = mutationMutex.withLock {
            contracts.value.firstOrNull { !it.isArchived }
        } ?: return

        val archived = active.copy(isArchived = true, updatedAt = System.currentTimeMillis())

        val created = jsonDataSource.createNewContract().copy(
            categories = active.categories,
            settings   = active.settings,
            budgets    = active.budgets,  // Задача 4.4: переносим budgets
        )

        mutationMutex.withLock {
            jsonDataSource.saveContract(archived)
            jsonDataSource.saveContract(created)
            contracts.value = contracts.value
                .filterNot { it.contractId == active.contractId } + archived + created
        }

        scheduleCheckMerge()
    }

    suspend fun listContractInfo()              = jsonDataSource.listContractInfo()
    suspend fun importContract(uri: android.net.Uri) =
        jsonDataSource.importContract(uri).also { contracts.value = jsonDataSource.loadContracts() }
    suspend fun exportAll(uri: android.net.Uri)  = jsonDataSource.exportAll(uri)
    fun contractsPath(): String                   = jsonDataSource.contractsPath()

    // ── Observe ───────────────────────────────────────────────

    override fun observeAccounts(): Flow<List<Account>> = contracts.map { list ->
        list.filterNot(ContractEntity::isArchived).flatMap(ContractEntity::accounts)
    }
    override fun observeTransactions(): Flow<List<Transaction>> = contracts.map { list ->
        list.filterNot(ContractEntity::isArchived).flatMap(ContractEntity::transactions)
    }
    override fun observeBudgets(): Flow<List<Budget>> = contracts.map { list ->
        list.filterNot(ContractEntity::isArchived).flatMap(ContractEntity::budgets)
    }
    override fun observeDebts(): Flow<List<Debt>> = contracts.map { list ->
        list.filterNot(ContractEntity::isArchived).flatMap(ContractEntity::debts)
    }
    override fun observeWishlist(): Flow<List<WishItem>> = contracts.map { list ->
        list.filterNot(ContractEntity::isArchived).flatMap(ContractEntity::wishlist)
    }
    override fun observeCategories(): Flow<List<ExpenseCategory>> = contracts.map { list ->
        list.firstOrNull { !it.isArchived }?.categories?.ifEmpty { DefaultCategories.list }
            ?: DefaultCategories.list
    }
    override fun observeSettings(): Flow<AppSettings> = contracts.map { list ->
        list.firstOrNull { !it.isArchived }?.settings ?: AppSettings()
    }

    // ── Мутации ───────────────────────────────────────────────

    override suspend fun saveInsights(insights: List<InsightCard>) =
        updateActive { it.copy(insights = insights) }

    override suspend fun addAccount(account: Account) =
        updateActive { it.copy(accounts = it.accounts + account.withContract(it.contractId)) }

    override suspend fun updateAccount(account: Account) {
        mutationMutex.withLock {
            val current = contracts.value
            val contract = current.firstOrNull { it.contractId == account.contractId && !it.isArchived }
                ?: current.firstOrNull { !it.isArchived && it.accounts.any { a -> a.id == account.id } }
                ?: return
            val updated = contract.copy(
                accounts  = contract.accounts.map { if (it.id == account.id) account.withContract(contract.contractId) else it },
                updatedAt = System.currentTimeMillis(),
            )
            persistUpdatedContractLocked(updated)
        }
    }

    override suspend fun deleteAccount(id: String) =
        updateActive { it.copy(accounts = it.accounts.filterNot { a -> a.id == id }) }

    override suspend fun addTransaction(transaction: Transaction) {
        val needsRotation = mutationMutex.withLock {
            val contract = findEditableContractByAccountLocked(transaction.accountId) ?: ensureActiveLocked()
            val updated  = contract.copy(
                transactions = contract.transactions + transaction,
                updatedAt    = System.currentTimeMillis(),
                isArchived   = false,
            )
            persistUpdatedContractLocked(updated)
            // Задача 1.2: возвращаем флаг ротации — не вызываем createNewContract внутри lock
            updated.transactions.size > MAX_TX_PER_FILE
        }
        if (needsRotation) bgScope.launch { createNewContract() }
    }

    override suspend fun addTransactionAndUpdateBalance(transaction: Transaction) {
        val needsRotation = mutationMutex.withLock {
            val contract = findContractByAccountLocked(transaction.accountId) ?: return
            val account  = contract.accounts.firstOrNull { it.id == transaction.accountId } ?: return
            val delta: Long = when (transaction.type) {
                TransactionType.INCOME, TransactionType.TRANSFER_IN   ->  transaction.amount
                TransactionType.EXPENSE, TransactionType.TRANSFER_OUT -> -transaction.amount
            }
            val updated = contract.copy(
                transactions = contract.transactions + transaction,
                accounts     = contract.accounts.map {
                    if (it.id == account.id) it.copy(balance = it.balance + delta) else it
                },
                updatedAt    = System.currentTimeMillis(),
                isArchived   = false,
            )
            persistUpdatedContractLocked(updated)
            // Задача 1.2: флаг ротации
            updated.transactions.size > MAX_TX_PER_FILE
        }
        if (needsRotation) bgScope.launch { createNewContract() }
    }

    override suspend fun deleteTransaction(id: String) =
        updateAllNonArchived { it.copy(transactions = it.transactions.filterNot { t -> t.id == id }) }

    override suspend fun addDebt(debt: Debt) =
        updateActive { it.copy(debts = it.debts + debt) }
    override suspend fun updateDebt(debt: Debt) =
        updateActive { c -> c.copy(debts = c.debts.map { if (it.id == debt.id) debt else it }) }
    override suspend fun deleteDebt(id: String) =
        updateAllNonArchived { it.copy(debts = it.debts.filterNot { d -> d.id == id }) }

    override suspend fun addWishItem(item: WishItem) =
        updateActive { it.copy(wishlist = it.wishlist + item) }
    override suspend fun updateWishItem(item: WishItem) =
        updateActive { c -> c.copy(wishlist = c.wishlist.map { if (it.id == item.id) item else it }) }
    override suspend fun deleteWishItem(id: String) =
        updateAllNonArchived { it.copy(wishlist = it.wishlist.filterNot { w -> w.id == id }) }

    override suspend fun saveBudgets(budgets: List<Budget>) =
        updateActive { it.copy(budgets = budgets) }
    override suspend fun saveCategories(categories: List<ExpenseCategory>) =
        updateActive { it.copy(categories = categories) }
    override suspend fun saveSettings(settings: AppSettings) =
        updateActive { it.copy(settings = settings) }

    // ── Внутренние хелперы ────────────────────────────────────

    /**
     * Задача 1.2: updateActive возвращает флаг ротации,
     * ротация запускается в bgScope ПОСЛЕ выхода из mutex.
     */
    private suspend fun updateActive(transform: (ContractEntity) -> ContractEntity) {
        val needsRotation = mutationMutex.withLock {
            val active  = ensureActiveLocked()
            val updated = transform(active).copy(updatedAt = System.currentTimeMillis(), isArchived = false)
            val file    = persistUpdatedContractLocked(updated)
            file.length() > MAX_FILE_BYTES || updated.transactions.size > MAX_TX_PER_FILE
        }
        // Задача 1.2: ротация запускается ВНЕ mutex — нет риска deadlock
        if (needsRotation) bgScope.launch { createNewContract() }
    }

    /**
     * Задача 1.3 (updateAllNonArchived): атомарная запись каждого контракта,
     * обновление contracts.value только после всех записей — нет частично-обновлённого состояния.
     */
    private suspend fun updateAllNonArchived(transform: (ContractEntity) -> ContractEntity) {
        mutationMutex.withLock {
            val now      = System.currentTimeMillis()
            val original = contracts.value
            val updated  = original.map { contract ->
                if (!contract.isArchived) transform(contract).copy(updatedAt = now) else contract
            }
            // Сначала сохраняем ВСЕ изменённые файлы
            val changed = updated.filterIndexed { i, c -> c !== original[i] }
            changed.forEach { jsonDataSource.saveContract(it) }
            // Только после успешной записи обновляем state
            contracts.value = updated
        }
    }

    private suspend fun persistUpdatedContractLocked(updated: ContractEntity): java.io.File {
        val file = jsonDataSource.saveContract(updated)
        contracts.value = contracts.value.map { if (it.contractId == updated.contractId) updated else it }
        return file
    }

    private fun findEditableContractByAccountLocked(accountId: String): ContractEntity? {
        val active = contracts.value.firstOrNull { !it.isArchived } ?: return null
        return if (active.accounts.any { it.id == accountId }) active else null
    }

    private fun findContractByAccountLocked(accountId: String): ContractEntity? =
        contracts.value.firstOrNull { !it.isArchived && it.accounts.any { a -> a.id == accountId } }

    private fun ensureActiveLocked(): ContractEntity =
        contracts.value.firstOrNull { !it.isArchived }
            ?: run {
                val new = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { jsonDataSource.createNewContract() }
                contracts.value = contracts.value + new
                new
            }

    /**
     * Задача 1.2: проверяем порог и запускаем слияние в bgScope если нужно.
     * Никогда не вызывается внутри mutationMutex.
     */
    private fun scheduleCheckMerge() {
        bgScope.launch {
            val count = jsonDataSource.countContracts()
            if (count > MAX_CONTRACTS) {
                Timber.d("ContractRepository: $count > $MAX_CONTRACTS, merging in background")
                mergeAllContracts()
                // Рекурсивная проверка
                if (jsonDataSource.countContracts() > MAX_CONTRACTS) scheduleCheckMerge()
            }
        }
    }

    /**
     * Задача 3.6: оба счёта обновляются и обе транзакции записываются атомарно.
     * Если fromAccount и toAccount находятся в одном контракте — один saveContract.
     * Если в разных — два saveContract под одним mutex (гарантия порядка).
     */
    override suspend fun atomicTransfer(
        txOut: com.finfocus.app.domain.model.Transaction,
        txIn:  com.finfocus.app.domain.model.Transaction,
    ) {
        mutationMutex.withLock {
            val contractFrom = findContractByAccountLocked(txOut.accountId) ?: return@withLock
            val contractTo   = findContractByAccountLocked(txIn.accountId)  ?: return@withLock
            val now          = System.currentTimeMillis()

            val deltaOut: Long = -txOut.amount
            val deltaIn: Long  =  txIn.amount

            if (contractFrom.contractId == contractTo.contractId) {
                // Один контракт — одна запись
                val updated = contractFrom.copy(
                    transactions = contractFrom.transactions + txOut + txIn,
                    accounts     = contractFrom.accounts.map { acc ->
                        when (acc.id) {
                            txOut.accountId -> acc.copy(balance = acc.balance + deltaOut)
                            txIn.accountId  -> acc.copy(balance = acc.balance + deltaIn)
                            else            -> acc
                        }
                    },
                    updatedAt = now,
                )
                persistUpdatedContractLocked(updated)
            } else {
                // Два контракта — два сохранения, но под одним mutex
                val updatedFrom = contractFrom.copy(
                    transactions = contractFrom.transactions + txOut,
                    accounts     = contractFrom.accounts.map { acc ->
                        if (acc.id == txOut.accountId) acc.copy(balance = acc.balance + deltaOut) else acc
                    },
                    updatedAt = now,
                )
                val updatedTo = contractTo.copy(
                    transactions = contractTo.transactions + txIn,
                    accounts     = contractTo.accounts.map { acc ->
                        if (acc.id == txIn.accountId) acc.copy(balance = acc.balance + deltaIn) else acc
                    },
                    updatedAt = now,
                )
                persistUpdatedContractLocked(updatedFrom)
                persistUpdatedContractLocked(updatedTo)
            }
        }
    }

    private fun Account.withContract(contractId: String): Account = copy(contractId = contractId)
}
