package com.kims.recipe2.ui.mypage

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

// UI 상태를 나타내는 Enum 클래스들
enum class TimePeriod { DAILY, WEEKLY, MONTHLY }
enum class ChartType { TREND, CUMULATIVE }

class MyPageViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // LiveData 선언
    private val _chartDataList = MutableLiveData<List<DailyNutrition>>()
    val chartDataList: LiveData<List<DailyNutrition>> = _chartDataList // '추이' 차트용

    private val _cumulativeChartData = MutableLiveData<Map<String, Double>>()
    val cumulativeChartData: LiveData<Map<String, Double>> = _cumulativeChartData // '누적' 차트용

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 정적 데이터 LiveData (기존과 동일)
    private val _stats = MutableLiveData<List<MyPageStat>>()
    val stats: LiveData<List<MyPageStat>> = _stats
    private val _weeklyProgress = MutableLiveData<List<NutritionItem>>()
    val weeklyProgress: LiveData<List<NutritionItem>> = _weeklyProgress
    private val _achievement = MutableLiveData<Pair<String, String>>()
    val achievement: LiveData<Pair<String, String>> = _achievement

    init {
        loadStaticData()
        // 기본값: 일간(이번 주) 추이 데이터 로드
        loadNutritionDataFor(TimePeriod.DAILY, ChartType.TREND, "칼로리")
    }

    // UI 컨트롤러에서 상태가 변경될 때마다 호출될 메인 함수
    fun loadNutritionDataFor(period: TimePeriod, chartType: ChartType, nutrient: String) {
        if (userId == null) return
        _isLoading.value = true

        // '누적' 차트는 더 넓은 기간의 데이터가 필요하므로 연초부터 오늘까지 데이터를 가져옴
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        val startOfYear = calendar.time
        val endOfYear = Date()

        db.collection("users").document(userId).collection("dailyNutrition")
            .whereGreaterThanOrEqualTo("date", startOfYear)
            .whereLessThanOrEqualTo("date", endOfYear)
            .orderBy("date")
            .get()
            .addOnSuccessListener { documents ->
                val allData = documents.toObjects(DailyNutrition::class.java)

                if (chartType == ChartType.TREND) {
                    _chartDataList.value = filterDataForTrend(allData, period)
                } else { // CUMULATIVE
                    _cumulativeChartData.value = calculateCumulativeData(allData, period, nutrient)
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    // 추이 그래프를 위해 데이터를 기간별로 필터링하는 함수
    private fun filterDataForTrend(data: List<DailyNutrition>, period: TimePeriod): List<DailyNutrition> {
        val calendar = Calendar.getInstance()
        val today = Date()
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.KOREA)

        return data.filter { daily ->
            val dailyDate = daily.date ?: return@filter false
            when (period) {
                TimePeriod.DAILY, TimePeriod.WEEKLY -> {
                    // 이번 주에 속하는지 확인
                    val calToday = Calendar.getInstance().apply { time = today }
                    val calDaily = Calendar.getInstance().apply { time = dailyDate }
                    calToday.get(Calendar.WEEK_OF_YEAR) == calDaily.get(Calendar.WEEK_OF_YEAR) &&
                            calToday.get(Calendar.YEAR) == calDaily.get(Calendar.YEAR)
                }
                TimePeriod.MONTHLY -> {
                    // 이번 달에 속하는지 확인
                    sdf.format(today).substring(0, 6) == sdf.format(dailyDate).substring(0, 6)
                }
            }
        }
    }

    // 누적 그래프를 위해 데이터를 기간별로 그룹화하고 합산하는 함수
    private fun calculateCumulativeData(data: List<DailyNutrition>, period: TimePeriod, nutrient: String): Map<String, Double> {
        val calendar = Calendar.getInstance()
        val groupedData = mutableMapOf<String, Double>()

        for (daily in data) {
            calendar.time = daily.date!!
            // X축 라벨(키) 생성
            val key = when (period) {
                TimePeriod.WEEKLY -> "${calendar.get(Calendar.MONTH) + 1}월 ${calendar.get(Calendar.WEEK_OF_MONTH)}주차"
                TimePeriod.MONTHLY -> "${calendar.get(Calendar.MONTH) + 1}월"
                else -> SimpleDateFormat("M/d", Locale.KOREA).format(daily.date) // 일간 누적은 의미 없지만 기본값 처리
            }
            val value = getNutrientValue(daily, nutrient)
            groupedData[key] = (groupedData[key] ?: 0.0) + value
        }
        return groupedData
    }

    // 문자열로 특정 영양소 값을 가져오는 헬퍼 함수
    fun getNutrientValue(data: DailyNutrition, nutrient: String): Double {
        return when (nutrient) {
            "칼로리" -> data.calories
            "탄수화물" -> data.carbs
            "단백질" -> data.protein
            "지방" -> data.fat
            else -> 0.0
        }
    }

    /* 기간/필터와 상관없이 고정된 데이터를 로드하는 함수입니다.
    * (통계 카드, 주간 목표, 월간 성취 등)
    */
    private fun loadStaticData() {
        // 실제 앱에서는 이 데이터들도 Firestore에서 가져올 수 있습니다.
        // 현재는 데모용 고정 데이터를 사용합니다.
        _stats.value = listOf(
            MyPageStat("🍜", "이번 주 최다", "김치찌개"),
            MyPageStat("💊", "필요 영양소", "비타민 C")
        )
        _weeklyProgress.value = listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", 1750f, 2000f, "kcal"),
            NutritionItem("단백질", "💪", "#4ecdc4", 52f, 60f, "g"),
            NutritionItem("비타민 C", "🍎", "#ff9ff3", 65f, 100f, "mg")
        )
    }
}