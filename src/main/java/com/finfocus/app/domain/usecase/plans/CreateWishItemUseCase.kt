package com.finfocus.app.domain.usecase.plans

import com.finfocus.app.domain.model.Priority
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.model.WishStatus
import com.finfocus.app.domain.repository.FinFocusRepository
import java.util.UUID
import javax.inject.Inject

class CreateWishItemUseCase @Inject constructor(
    private val repository: FinFocusRepository,
) {
    suspend operator fun invoke(
        name: String,
        price: Long,
        priority: Priority,
        category: String,
        comment: String,
        monthlySaving: Long,
        planCategory: String = "",      // Задача 4.1
        isUncertain: Boolean = false,   // Задача 4.1
    ) {
        repository.addWishItem(
            WishItem(
                id            = UUID.randomUUID().toString(),
                name          = name,
                price         = price,
                priority      = priority,
                category      = category,
                comment       = comment,
                monthlySaving = monthlySaving,
                saved         = 0L,
                status        = if (isUncertain) WishStatus.UNCERTAIN else WishStatus.PLANNED,
                planCategory  = planCategory,
            ),
        )
    }
}
