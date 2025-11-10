package com.kims.recipe2.model

/**
 * 사용자의 신체 정보와 목표 영양소를 저장하는 데이터 클래스
 */
data class UserInfo(
    val height: Int = 0,
    val weight: Int = 0,
    val gender: String = "남성",
    // 목표 영양소
    val goalCalories: Double = 2000.0,
    val goalCarbs: Double = 250.0,
    val goalProtein: Double = 150.0,
    val goalFat: Double = 67.0
)