package com.kims.recipe2.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kims.recipe2.model.MealRecord
import java.time.LocalDate

class CalendarViewModel : ViewModel() {

    // Firestore에서 가져올 식단 데이터 (데모)
    private val mealData = MutableLiveData<Map<LocalDate, List<MealRecord>>>()

    private val _selectedDateMeals = MutableLiveData<List<MealRecord>>()
    val selectedDateMeals: LiveData<List<MealRecord>> = _selectedDateMeals

    init {
        loadMealData()
    }

    private fun loadMealData() {
        // 실제로는 Firestore에서 비동기적으로 로드
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        mealData.value = mapOf(
            today to listOf(
                MealRecord(id="1", name="아침: 계란후라이 토스트", type = "아침", calories = 350, protein = 18),
                MealRecord(id="2", name="점심: 김치찌개", type = "점심", calories = 450, protein = 22)
            ),
            yesterday to listOf(
                MealRecord(id="3", name="저녁: 닭가슴살 샐러드", type = "저녁", calories = 400, protein = 30)
            ),
            tomorrow to listOf(
                MealRecord(id="4", name="저녁: 연어 샐러드 (예정)", type = "저녁", calories = 380, isPlanned = true)
            )
        )
    }

    fun getMealsForDate(date: LocalDate) {
        _selectedDateMeals.value = mealData.value?.get(date) ?: emptyList()
    }

    fun getDatesWithMeals(): LiveData<Set<LocalDate>> {
        val datesWithMeals = MutableLiveData<Set<LocalDate>>()
        datesWithMeals.value = mealData.value?.keys ?: emptySet()
        return datesWithMeals
    }
}