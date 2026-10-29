package com.creativeblisslabs.wearproductivity.wear.workout

object WorkoutScienceEngine {
    // Brzycki Formula for Estimated 1 Rep Max (1RM) in Strength Training
    fun calculateEstimated1RM(weightKg: Double, reps: Int): Double {
        if (reps <= 0 || weightKg <= 0.0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (36.0 / (37.0 - reps.toDouble().coerceAtMost(36.0)))
    }

    // SWOLF Score for Swimming Efficiency (Lower score = higher stroke efficiency)
    fun calculateSwolf(strokeCount: Int, lapTimeSeconds: Int): Int {
        return strokeCount + lapTimeSeconds
    }

    // Running Pace calculation (min per km)
    fun calculatePace(distanceKm: Double, timeSeconds: Int): String {
        if (distanceKm <= 0.0 || timeSeconds <= 0) return "0:00 /km"
        val totalMinutes = (timeSeconds / 60.0) / distanceKm
        val mins = totalMinutes.toInt()
        val secs = ((totalMinutes - mins) * 60).toInt()
        return "$mins:${secs.toString().padStart(2, '0')} /km"
    }

    // Calorie expenditure estimation based on Metabolic Equivalent of Task (MET)
    fun estimateCalories(met: Double, weightKg: Double = 70.0, durationMinutes: Int): Double {
        return (met * 3.5 * weightKg / 200.0) * durationMinutes
    }
}
