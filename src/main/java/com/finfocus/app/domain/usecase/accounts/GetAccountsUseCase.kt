package com.finfocus.app.domain.usecase.accounts

import com.finfocus.app.domain.repository.FinFocusRepository
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    operator fun invoke() = repository.observeAccounts()
}
