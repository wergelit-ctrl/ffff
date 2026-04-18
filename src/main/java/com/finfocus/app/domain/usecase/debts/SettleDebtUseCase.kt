package com.finfocus.app.domain.usecase.debts

import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.repository.FinFocusRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SettleDebtUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    suspend operator fun invoke(debtId: String, paidAmount: Long) {
        val debt = repository.observeDebts().first().firstOrNull { it.id == debtId } ?: return
        // L5: clamp totalPaid first, then derive remaining — avoids status mismatch
        // when paidAmount overshoots (e.g. totalPaid=110, amount=100 → remaining=-10 → CLOSED but paidAmount=100)
        val totalPaid = (debt.paidAmount + paidAmount).coerceAtMost(debt.amount)
        val remaining = debt.amount - totalPaid
        val status = if (remaining <= 0.0) DebtStatus.CLOSED else DebtStatus.PARTIALLY_PAID
        repository.updateDebt(debt.copy(paidAmount = totalPaid, status = status))
    }
}
