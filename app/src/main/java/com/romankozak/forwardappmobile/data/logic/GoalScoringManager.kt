package com.romankozak.forwardappmobile.data.logic

import com.romankozak.forwardappmobile.data.database.models.Goal

/**
 * Інкапсулює логіку обчислення оцінок для цілей.
 * Використовує різні шкали для різних параметрів та нормалізує їх до діапазону [0, 1].
 */
object GoalScoringManager {

    // --- Визначення індивідуальних шкал для кожного параметра ---
    private val effortScale = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    private val importanceScale = (1..12).map { it.toFloat() } // Лінійна 1-12
    private val impactScale = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    private val costScale = (0..5).map { it.toFloat() } // Лінійна 0-5
    private val riskScale = effortScale // Ризик використовує ту ж шкалу, що й зусилля

    /**
     * Нормалізує значення до діапазону [0, 1] на основі наданої шкали.
     * @param value Поточне значення параметра.
     * @param scale Список значень, що визначає шкалу (напр., [0, 1, 2, 3, 5]).
     * @return Значення, нормалізоване до діапазону [0, 1].
     */
    private fun normalize(value: Float, scale: List<Float>): Float {
        val min = scale.firstOrNull() ?: 0f
        val max = scale.lastOrNull() ?: 1f
        if (max <= min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    /**
     * Обчислює "сиру" та "популярну" оцінки для цілі та повертає оновлений об'єкт.
     */
    fun calculateScores(goal: Goal): Goal {
        // 1. Нормалізація кожного параметра з використанням його власної шкали
        val normImportance = normalize(goal.valueImportance, importanceScale)
        val normImpact = normalize(goal.valueImpact, impactScale)
        val normEffort = normalize(goal.effort, effortScale)
        val normCost = normalize(goal.cost, costScale)
        val normRisk = normalize(goal.risk, riskScale)

        // 2. Розрахунок Нормалізованої Користі
        val normBenefit = normImportance * normImpact

        // 3. Розрахунок Нормалізованих Загальних Витрат
        val totalWeight = goal.weightEffort + goal.weightCost + goal.weightRisk
        val normTotalCost = if (totalWeight > 0f) {
            (goal.weightEffort * normEffort + goal.weightCost * normCost + goal.weightRisk * normRisk) / totalWeight
        } else {
            0f
        }

        // 4. ЕТАП 1: Обчислення "сирого" результату
        val calculatedRawScore = normBenefit - normTotalCost

        // 5. ЕТАП 2: Обчислення "популярної" оцінки
        val calculatedDisplayScore = (((calculatedRawScore + 1) / 2) * 100).toInt().coerceIn(0, 100)

        // 6. Повернення оновленого об'єкта Goal
        return goal.copy(
            rawScore = calculatedRawScore,
            displayScore = calculatedDisplayScore
        )
    }
}