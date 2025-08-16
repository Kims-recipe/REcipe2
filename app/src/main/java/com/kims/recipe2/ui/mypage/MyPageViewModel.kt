package com.kims.recipe2.ui.mypage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kims.recipe2.model.DailyNutrition
import com.kims.recipe2.model.MyPageStat
import com.kims.recipe2.model.NutritionItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimePeriod { DAILY, WEEKLY, MONTHLY }

class MyPageViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // LiveData 선언
    private val _chartData = MutableLiveData<Map<String, Float>>()
    val chartData: LiveData<Map<String, Float>> = _chartData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _stats = MutableLiveData<List<MyPageStat>>()
    val stats: LiveData<List<MyPageStat>> = _stats
    private val _weeklyProgress = MutableLiveData<List<NutritionItem>>()
    val weeklyProgress: LiveData<List<NutritionItem>> = _weeklyProgress
    private val _achievement = MutableLiveData<Pair<String, String>>()
    val achievement: LiveData<Pair<String, String>> = _achievement

    init {
        loadStaticData()
        loadNutritionDataFor(TimePeriod.DAILY, "칼로리") // 기본값
    }

    private fun loadStaticData() {
        _stats.value = listOf(
            MyPageStat("🍜", "이번 주 최다", "김치찌개"),
            MyPageStat("💊", "필요 영양소", "비타민 C")
        )
        _weeklyProgress.value = listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", 1750f, 2000f, "kcal"),
            NutritionItem("단백질", "💪", "#4ecdc4", 52f, 60f, "g"),
            NutritionItem("비타민 C", "🍎", "#ff9ff3", 65f, 100f, "mg")
        )
        _achievement.value = "건강한 한 달!" to "목표 칼로리 달성 23일"
    }

    fun loadNutritionDataFor(period: TimePeriod, nutrient: String) {
        if (userId == null) {
            _chartData.value = emptyMap()
            return
        }
        _isLoading.value = true

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -6)
        val startDate = calendar.time

        db.collection("users").document(userId).collection("dailyNutrition")
            .whereGreaterThanOrEqualTo("date", startDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val allData = documents.toObjects(DailyNutrition::class.java)
                processDataForChart(allData, period, nutrient)
                _isLoading.value = false
            }.addOnFailureListener {
                _chartData.value = emptyMap()
                _isLoading.value = false
            }
    }

    private fun processDataForChart(data: List<DailyNutrition>, period: TimePeriod, nutrient: String) {
        val processedData = when (period) {
            TimePeriod.DAILY -> {
                val dailyData = data.take(7).reversed()
                dailyData.associate {
                    val dateLabel = SimpleDateFormat("M/d", Locale.KOREA).format(it.date!!)
                    dateLabel to getNutrientValue(it, nutrient).toFloat()
                }
            }
            TimePeriod.WEEKLY -> {
                val weeklyAverages = mutableMapOf<String, Pair<Double, Int>>()
                for (daily in data) {
                    val weekKey = getWeekLabel(daily.date!!)
                    if (weekKey.isNotEmpty() && (weeklyAverages.size < 6 || weeklyAverages.containsKey(weekKey))) {
                        val current = weeklyAverages.getOrDefault(weekKey, Pair(0.0, 0))
                        weeklyAverages[weekKey] = Pair(current.first + getNutrientValue(daily, nutrient), current.second + 1)
                    }
                }
                weeklyAverages.mapValues { (_, value) ->
                    if (value.second == 0) 0f else (value.first / value.second).toFloat()
                }
            }
            TimePeriod.MONTHLY -> {
                val monthlyAverages = mutableMapOf<String, Pair<Double, Int>>()
                for (daily in data) {
                    val monthKey = getMonthLabel(daily.date!!)
                    if (monthKey.isNotEmpty() && (monthlyAverages.size < 6 || monthlyAverages.containsKey(monthKey))) {
                        val current = monthlyAverages.getOrDefault(monthKey, Pair(0.0, 0))
                        monthlyAverages[monthKey] = Pair(current.first + getNutrientValue(daily, nutrient), current.second + 1)
                    }
                }
                monthlyAverages.mapValues { (_, value) ->
                    if (value.second == 0) 0f else (value.first / value.second).toFloat()
                }
            }
        }
        _chartData.value = processedData
    }

    fun getNutrientValue(data: DailyNutrition, nutrient: String): Double {
        return when (nutrient) {
            "칼로리" -> data.calories
            "탄수화물" -> data.carbs
            "단백질" -> data.protein
            "지방" -> data.fat
            else -> 0.0
        }
    }

    private fun getWeekLabel(date: Date): String {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }

        // 연도가 다를 경우를 대비한 주차 계산
        var weekDiff = 0
        if (today.get(Calendar.YEAR) != target.get(Calendar.YEAR)){
            val daysBetween = ((today.timeInMillis - target.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            weekDiff = daysBetween / 7
        } else {
            weekDiff = today.get(Calendar.WEEK_OF_YEAR) - target.get(Calendar.WEEK_OF_YEAR)
        }

        return when (weekDiff) {
            0 -> "이번주"
            in 1..5 -> "${weekDiff}주전"
            else -> ""
        }
    }

    private fun getMonthLabel(date: Date): String {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        val monthDiff = (today.get(Calendar.YEAR) - target.get(Calendar.YEAR)) * 12 +
                (today.get(Calendar.MONTH) - target.get(Calendar.MONTH))

        return when (monthDiff) {
            0 -> "이번달"
            in 1..5 -> "${monthDiff}달전"
            else -> ""
        }
    }
}