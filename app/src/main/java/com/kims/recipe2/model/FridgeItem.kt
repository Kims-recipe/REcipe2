package com.kims.recipe2.model

data class FridgeSection(
    val id: String,
    val name: String,
    val icon: String
)

data class FridgeCategory(
    val name: String,
    val examples: String,
    val icon: String,
    val backgroundColorHex: String
)
