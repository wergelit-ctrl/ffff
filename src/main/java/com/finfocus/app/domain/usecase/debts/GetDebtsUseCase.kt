package com.finfocus.app.domain.usecase.debts

import com.finfocus.app.domain.repository.FinFocusRepository
import javax.inject.Inject

class GetDebtsUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    operator fun invoke() = repository.observeDebts()
}
