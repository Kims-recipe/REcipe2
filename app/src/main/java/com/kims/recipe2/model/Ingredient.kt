package com.kims.recipe2.model

import com.google.firebase.firestore.Exclude

data class Ingredient(
    @get:Exclude var id: String = "",
    val name: String = "",
    val category: String = "",
    val location: String = "",   // 냉장고 위치 (예: "냉동실")
    val quantity: Int = 1,       // 개수
    val amount: Double = 0.0,    // 양 (g, ml 등)
    val unit: String = "개"      // 단위 (개, g, ml 등)
)