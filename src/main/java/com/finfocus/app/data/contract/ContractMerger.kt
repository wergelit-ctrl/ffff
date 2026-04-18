package com.finfocus.app.data.contract

import com.finfocus.app.data.contract.model.ContractEntity
import com.finfocus.app.domain.model.AppSettings
import com.finfocus.app.domain.model.DefaultCategories
import java.util.UUID
import javax.inject.Inject

/**
 * Слияние нескольких контрактов в один.
 *
 * Задача 1.1: переносит ВСЕ сущности включая categories и settings.
 * Задача 1.5: использует Sequence для обхода без одновременной загрузки всего в память.
 *
 * Приоритет при конфликте:
 * - categories и settings берутся из самого нового (по updatedAt) неархивированного контракта
 * - остальные сущности дедуплицируются по id
 */
class ContractMerger @Inject constructor() {

    fun merge(contracts: List<ContractEntity>): ContractEntity {
        val now = System.currentTimeMillis()

        // Самый новый неархивированный — источник настроек и категорий
        val newest = contracts
            .filter { !it.isArchived }
            .maxByOrNull { it.updatedAt }
            ?: contracts.maxByOrNull { it.updatedAt }

        // Задача 1.5: Sequence-based дедупликация — не создаём промежуточные списки
        val accounts     = contracts.asSequence().flatMap { it.accounts.asSequence() }.distinctBy { it.id }.toList()
        val transactions = contracts.asSequence().flatMap { it.transactions.asSequence() }.distinctBy { it.id }.toList()
        val budgets      = contracts.asSequence().flatMap { it.budgets.asSequence() }.distinctBy { it.id }.toList()
        val debts        = contracts.asSequence().flatMap { it.debts.asSequence() }.distinctBy { it.id }.toList()
        val wishlist     = contracts.asSequence().flatMap { it.wishlist.asSequence() }.distinctBy { it.id }.toList()
        val insights     = contracts.asSequence().flatMap { it.insights.asSequence() }.distinctBy { it.id }.toList()

        // Задача 1.1: категории — берём из новейшего, fallback на дефолтные
        val categories = newest?.categories?.ifEmpty { DefaultCategories.list } ?: DefaultCategories.list

        // Задача 1.1: настройки — из новейшего, с умным мержем полей
        val settings = mergeSettings(contracts, newest?.settings)

        return ContractEntity(
            contractId   = UUID.randomUUID().toString(),
            createdAt    = now,
            updatedAt    = now,
            isArchived   = false,
            accounts     = accounts,
            transactions = transactions,
            budgets      = budgets,
            debts        = debts,
            wishlist     = wishlist,
            insights     = insights,
            categories   = categories,
            settings     = settings,
        )
    }

    /**
     * Мерж настроек: берём из новейшего, но если поле пустое —
     * ищем в других контрактах (на случай если в разных контрактах разные поля были заполнены).
     */
    private fun mergeSettings(contracts: List<ContractEntity>, base: AppSettings?): AppSettings {
        val allSettings = contracts.asSequence().map { it.settings }

        val primaryId = base?.primaryAccountId?.ifBlank { null }
            ?: allSettings.map { it.primaryAccountId }.firstOrNull { it.isNotBlank() }
            ?: ""

        val savingsId = base?.savingsAccountId?.ifBlank { null }
            ?: allSettings.map { it.savingsAccountId }.firstOrNull { it.isNotBlank() }
            ?: ""

        val income = base?.budgetIncomeAmount?.takeIf { it > 0L }
            ?: allSettings.map { it.budgetIncomeAmount }.firstOrNull { it: Long -> it > 0L }
            ?: 0L

        return AppSettings(
            primaryAccountId    = primaryId,
            savingsAccountId    = savingsId,
            budgetIncomeAmount  = income,
        )
    }
}
