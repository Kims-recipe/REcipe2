package com.kims.recipe2

import java.util.Date

data class IngredientItem(
    val id: String,
    val name: String,
    val storageType: StorageType,
    val number: Int,
    val expiration: Date? = null,
    val imageUrl: String? = null
)
