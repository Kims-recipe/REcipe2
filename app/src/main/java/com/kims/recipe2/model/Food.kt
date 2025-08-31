package com.kims.recipe2.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Food(
    val name: String = "",
    val calories: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val sodium: Double = 0.0,
    val vitaminA: Double = 0.0,
    val vitaminC: Double = 0.0,
    // 👇 Firestore의 'expiration_date' 필드를 읽기 위한 필드 추가
    @get:PropertyName("expiration_date") @set:PropertyName("expiration_date")
    var expirationDate: Int = 0
)