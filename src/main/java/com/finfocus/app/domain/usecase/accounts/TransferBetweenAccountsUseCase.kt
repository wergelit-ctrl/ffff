package com.finfocus.app.domain.usecase.accounts

import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Перевод между счетами.
 *
 * Задача 3.6: обе транзакции (TRANSFER_OUT и TRANSFER_IN) создаются атомарно —
 * через единый метод репозитория `atomicTransfer`, который записывает их
 * в рамках одного saveContract. Нет риска что одна запись прошла, вторая — нет.
 */
class TransferBetweenAccountsUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    suspend operator fun invoke(fromAccountId: String, toAccountId: String, amount: Long) {
        val now    = System.currentTimeMillis()
        val linkId = UUID.randomUUID().toString() // связывает пару транзакций

        val txOut = Transaction(
            id        = UUID.randomUUID().toString(),
            accountId = fromAccountId,
            amount    = amount,
            category  = "Перевод",
            type      = TransactionType.TRANSFER_OUT,
            timestamp = now,
            note      = "→ transfer:$linkId",
        )
        val txIn = Transaction(
            id        = UUID.randomUUID().toString(),
            accountId = toAccountId,
            amount    = amount,
            category  = "Перевод",
            type      = TransactionType.TRANSFER_IN,
            timestamp = now,
            note      = "← transfer:$linkId",
        )

        // Задача 3.6: атомарный перевод — обе транзакции в одном saveContract
        repository.atomicTransfer(txOut, txIn)
    }
}
