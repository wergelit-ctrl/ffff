package com.finfocus.app.domain.usecase.budget

import com.finfocus.app.domain.model.Budget
import javax.inject.Inject

/**
 * Оставлен для обратной совместимости.
 * Логика расчёта бюджета 50/30/20 перенесена в BudgetViewModel.budgetGroups,
 * который использует ExpenseCategory.group для группировки расходов.
 */
class CalculateBudgetUseCase @Inject constructor() {
    operator fun invoke(monthIncome: Long): List<Budget> = listOf(
        Budget("need", "50_30_20", "MONTH", "Нужды",      monthIncome * 50L / 100L, 0L),
        Budget("want", "50_30_20", "MONTH", "Желания",     monthIncome * 30L / 100L, 0L),
        Budget("save", "50_30_20", "MONTH", "Сбережения",  monthIncome * 20L / 100L, 0L),
    )
}
