package com.finfocus.app.domain.usecase.debts

import com.finfocus.app.presentation.util.toRubles

import com.finfocus.app.data.notifications.DebtNotificationService
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.domain.repository.FinFocusRepository
import java.util.UUID
import javax.inject.Inject

class CreateDebtUseCase @Inject constructor(
    private val repository: FinFocusRepository,
    private val debtNotificationService: DebtNotificationService,
) {
    suspend operator fun invoke(personName: String, amount: Long, type: DebtType, dueDate: Long?, comment: String) {
        val debt = Debt(
            id = UUID.randomUUID().toString(),
            personName = personName,
            amount = amount,
            paidAmount = 0L,
            type = type,
            createdAt = System.currentTimeMillis(),
            dueDate = dueDate,
            comment = comment,
            status = DebtStatus.ACTIVE,
        )
        repository.addDebt(debt)
        if (dueDate != null) {
            debtNotificationService.scheduleDebtReminder(debt.id, debt.personName, debt.amount.toRubles(), dueDate)
        }
    }
}
