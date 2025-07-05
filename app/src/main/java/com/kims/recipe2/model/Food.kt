package com.kims.recipe2.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Food(
    // 필드 이름과 동일하게 속성 선언
    val name: String = "",
    val calories: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val sodium: Double = 0.0,
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0
)
