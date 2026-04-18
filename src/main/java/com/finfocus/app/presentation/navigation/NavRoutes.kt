package com.finfocus.app.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute {
    @Serializable data object Dashboard          : NavRoute
    @Serializable data object Accounts           : NavRoute
    @Serializable data object Budget             : NavRoute
    @Serializable data object Plans              : NavRoute
    @Serializable data object Analytics          : NavRoute
    @Serializable data object Debts              : NavRoute
    @Serializable data object Contracts          : NavRoute
    @Serializable data object Settings           : NavRoute
    @Serializable data object CategoriesSettings : NavRoute

    /**
     * Экран "Снять" — категории расходов, сумма, свайп-подтверждение.
     * @param accountId  счёт списания
     */
    @Serializable data class Spend(val accountId: String = "") : NavRoute

    /**
     * Экран "Пополнить" — тип дохода, сумма, свайп-подтверждение.
     * @param accountId  счёт зачисления
     */
    @Serializable data class Deposit(val accountId: String = "") : NavRoute

    /**
     * Экран-чек после успешной операции.
     * @param accountId   счёт
     * @param amount      сумма в копейках (Long, задача 4.2)
     * @param categoryName название категории
     * @param isExpense   true = расход, false = доход
     */
    @Serializable data class Receipt(
        val accountId:    String = "",
        val amount:       Long   = 0L,
        val categoryName: String = "",
        val isExpense:    Boolean = true,
    ) : NavRoute

    /**
     * Экран создания нового долга.
     * @param isIOwe  true = я должен, false = мне должны
     */
    @Serializable data class CreateDebt(val isIOwe: Boolean = false) : NavRoute

    /** Старый маршрут — оставлен для обратной совместимости. */
    @Serializable data class Transaction(
        val accountId:   String  = "",
        val isExpense:   Boolean = true,
    ) : NavRoute
}
