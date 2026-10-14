package com.deudom.deudom.util

object ScoreCalculator {
    const val BASE_SCORE = 50
    const val MAX_SCORE = 100
    const val MIN_SCORE = 0

    const val POINTS_ON_TIME = 5
    const val POINTS_LATE = -10
    const val POINTS_EARLY = 8

    fun calculateNewScore(currentScore: Int, isOnTime: Boolean, isEarly: Boolean = false): Int {
        val points = when {
            isEarly -> POINTS_EARLY
            isOnTime -> POINTS_ON_TIME
            else -> POINTS_LATE
        }
        
        return (currentScore + points).coerceIn(MIN_SCORE, MAX_SCORE)
    }

    fun getScoreLabel(score: Int): String {
        return when (score) {
            in 90..100 -> "Legendario"
            in 75..89 -> "Excelente"
            in 60..74 -> "Bueno"
            in 40..59 -> "Regular"
            else -> "Riesgoso"
        }
    }
}
