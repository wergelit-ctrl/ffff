package com.finfocus.app.domain.usecase.prediction

import com.finfocus.app.domain.model.Budget
import com.finfocus.app.domain.model.Debt
import com.finfocus.app.domain.model.DebtStatus
import com.finfocus.app.domain.model.DebtType
import com.finfocus.app.domain.model.InsightType
import com.finfocus.app.domain.model.Transaction
import com.finfocus.app.domain.model.TransactionType
import com.finfocus.app.domain.model.WishItem
import com.finfocus.app.domain.model.Priority
import com.finfocus.app.domain.model.WishStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionEngineTest {
    private val engine = PredictionEngine()

    @Test
    fun `generates forecast insight with enough transactions`() {
        val now = System.currentTimeMillis()
        val tx = (1..10).map {
            Transaction(
                id = it.toString(),
                accountId = "a",
                amount = 10.0,
                category = "Еда",
                type = TransactionType.EXPENSE,
                timestamp = now - it * 86_400_000L,
            )
        }
        val insights = engine.generateInsights(tx, emptyList(), emptyList(), currentBalance = 1000.0, budgets = emptyList())
        assertTrue(insights.any { it.type == InsightType.FORECAST })
    }

    @Test
    fun `generates debt warning for near due debt`() {
        val debt = Debt(
            id = "1",
            personName = "Петя",
            amount = 100.0,
            paidAmount = 0.0,
            type = DebtType.I_OWE,
            createdAt = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 2 * 86_400_000L,
            comment = "",
            status = DebtStatus.ACTIVE,
        )
        val insights = engine.generateInsights(emptyList(), listOf(debt), emptyList(), currentBalance = 100.0, budgets = emptyList())
        assertTrue(insights.any { it.title.contains("Ближайший долг") })
    }
}
