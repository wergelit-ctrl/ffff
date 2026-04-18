package com.finfocus.app.domain.usecase.accounts

import com.finfocus.app.domain.model.Account
import com.finfocus.app.domain.model.AccountType
import com.finfocus.app.domain.repository.FinFocusRepository
import java.util.UUID
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    suspend operator fun invoke(
        name: String,
        type: AccountType,
        balance: Long = 0L,  // Задача 4.2: копейки
        iconKey: String = "wallet",
        color: Long = 0xFF_4CAF50,
    ) {
        repository.addAccount(
            Account(
                id          = UUID.randomUUID().toString(),
                name        = name,
                type        = type,
                balance     = balance,
                iconKey     = iconKey,
                color       = color,
                createdAt   = System.currentTimeMillis(),
                contractId  = "",
            ),
        )
    }
}
