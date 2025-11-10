package com.kims.recipe2.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MealRecord(
    var id: String = "",
    val name: String = "",
    val type: String = "", // "아침", "점심", "저녁"
    var calories: Double = 0.0,
    var carbs: Double = 0.0,
    var protein: Double = 0.0,
    var fat: Double = 0.0,
    var calcium: Double = 0.0,
    var iron: Double = 0.0,
    var sodium: Double = 0.0,
    var vitaminA: Double = 0.0,
    var vitaminC: Double = 0.0,
    @ServerTimestamp
    val date: Date? = null,
    val isPlanned: Boolean = false, // 예정된 식단인지 여부
    val isHomemade: Boolean = true, // 집밥인지 외식인지 구분하는 필드 추가
    val imageUri: String? = null // 식사 사진 URI 필드 추가
)