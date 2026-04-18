package com.finfocus.app.domain.usecase.accounts

import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.repository.FinFocusRepository
import java.util.UUID
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    suspend operator fun invoke(
        accountId: String,
        amount: Long,  // Задача 4.2: копейки
        category: String,
        type: TransactionType,
        note: String?,
    ) {
        val tx = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            amount = amount,  // Long kopecks
            category = category,
            type = type,
            timestamp = System.currentTimeMillis(),
            note = note,
        )
        repository.addTransactionAndUpdateBalance(tx)
    }
}
