package com.kims.recipe2.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DailyNutrition(
    @ServerTimestamp
    val date: Date? = null,
    var calories: Double = 0.0,
    var carbs: Double = 0.0,
    var protein: Double = 0.0,
    var fat: Double = 0.0,
    var calcium: Double = 0.0,
    var iron: Double = 0.0,
    var sodium: Double = 0.0,
    var vitaminA: Double = 0.0,
    var vitaminC: Double = 0.0
)
