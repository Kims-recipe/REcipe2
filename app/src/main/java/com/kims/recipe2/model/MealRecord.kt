package com.kims.recipe2.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class MealRecord(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "아침", "점심", "저녁"
    val calories: Int = 0,
    val protein: Int = 0,
    @ServerTimestamp
    val date: Date? = null,
    val isPlanned: Boolean = false // 예정된 식단인지 여부
)