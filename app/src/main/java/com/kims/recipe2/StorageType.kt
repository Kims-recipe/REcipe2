package com.kims.recipe2

enum class StorageType {
    REFRIGERATOR,
    FREEZER,
    PANTRY;

    fun getDisplayName(): String = when (this) {
        REFRIGERATOR -> "냉장실"
        FREEZER -> "냉동실"
        PANTRY -> "실온보관"
    }
}