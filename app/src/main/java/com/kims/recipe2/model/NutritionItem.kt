package com.kims.recipe2.model

data class NutritionItem(
    val name: String,
    val icon: String,
    val backgroundColorHex: String,
    val current: Float,
    val goal: Float,
    val unit: String
)