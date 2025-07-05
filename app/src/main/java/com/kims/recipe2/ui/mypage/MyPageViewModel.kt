package com.kims.recipe2.ui.mypage

import androidx.lifecycle.ViewModel
import com.kims.recipe2.model.NutritionItem

class MyPageViewModel : ViewModel() {

    // 주간 목표 달성률 데이터를 반환하는 함수
    fun getWeeklyProgress(): List<NutritionItem> {
        return listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", 1750f, 2000f, "kcal"),
            NutritionItem("단백질", "💪", "#4ecdc4", 52f, 60f, "g"),
            NutritionItem("비타민 C", "🍎", "#ff9ff3", 65f, 100f, "mg")
        )
    }
}