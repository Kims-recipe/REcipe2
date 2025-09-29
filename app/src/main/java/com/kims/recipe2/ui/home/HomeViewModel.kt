package com.kims.recipe2.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.DailyNutrition
import com.kims.recipe2.model.Food
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.model.MealRecord
import com.kims.recipe2.model.NutritionItem
import com.kims.recipe2.model.UserInfo
import com.kims.recipe2.util.DateUtil
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// 1. UI 상태를 한번에 관리할 데이터 클래스 추가
data class NutritionUIState(
    val displayList: List<NutritionItem> = emptyList(),
    val isExpanded: Boolean = false,
    val shouldShowButtons: Boolean = false
)

class HomeViewModel : ViewModel() {
    //파베 호출
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    //live 데이터 호출
    private val _todayDate = MutableLiveData<String>()
    val todayDate: LiveData<String> = _todayDate

    private val _foods = MutableLiveData<List<Food>>()
    val foods: LiveData<List<Food>> = _foods

    private val _isFoodsLoading = MutableLiveData<Boolean>()
    val isFoodsLoading: LiveData<Boolean> = _isFoodsLoading

    private val _priorityIngredients = MutableLiveData<List<Ingredient>>()
    val priorityIngredients: LiveData<List<Ingredient>> = _priorityIngredients

    private val _todayMealRecords = MutableLiveData<List<MealRecord>>()
    val todayMealRecords: LiveData<List<MealRecord>> = _todayMealRecords


    // ▼▼▼ 2. 기존 영양소 관련 LiveData들을 아래 코드로 대체 ▼▼▼
    private val _isNutritionExpanded = MutableLiveData(false)
    private val _allNutritionItems = MutableLiveData<List<NutritionItem>>()

    private val _nutritionUIState = MediatorLiveData<NutritionUIState>()
    val nutritionUIState: LiveData<NutritionUIState> = _nutritionUIState
    // ▲▲▲ 여기까지 대체 ▲▲▲


    private var userInfo: UserInfo? = null
    private var todaysNutrition: DailyNutrition? = null

    init {
        loadTodayDate()
        loadFoods()
        fetchInitialData()
        fetchPriorityIngredients()
        fetchTodayMealRecords()

        // 3. MediatorLiveData에 소스 LiveData들을 연결
        _nutritionUIState.addSource(_allNutritionItems) { updateNutritionState() }
        _nutritionUIState.addSource(_isNutritionExpanded) { updateNutritionState() }
    }

    private fun loadTodayDate() {
        val sdf = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
        _todayDate.value = sdf.format(Date())
    }

    private fun fetchTodayMealRecords() {
        if (userId == null) return
        val today = java.time.LocalDate.now()
        val startOfDay = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endOfDay = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant())

        db.collection("users").document(userId).collection("mealRecords")
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "오늘 식단 기록 로딩 실패", error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(MealRecord::class.java) ?: emptyList()
                _todayMealRecords.value = records.sortedBy { it.date }
            }
    }

    private fun fetchPriorityIngredients() {
        if (userId == null) return
        db.collection("users").document(userId).collection("ingredients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "우선 소비 재료 로딩 실패", error)
                    return@addSnapshotListener
                }
                val ingredients = snapshot?.toObjects(Ingredient::class.java) ?: emptyList()
                val sortedList = ingredients.sortedWith(
                    compareBy(
                        { it.expirationDate == null },
                        { DateUtil.calculateDDay(it.expirationDate) },
                        { it.quantity }
                    )
                ).take(5)
                _priorityIngredients.value = sortedList
            }
    }

    private fun fetchInitialData() {
        if (userId == null) return
        db.collection("users").document(userId)
            .collection("userInfo").document("profile")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                userInfo = snapshot?.toObject(UserInfo::class.java)
                updateNutritionUI()
            }
        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        db.collection("users").document(userId)
            .collection("dailyNutrition").document(todayDateString)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                todaysNutrition = if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(DailyNutrition::class.java)
                } else {
                    DailyNutrition()
                }
                updateNutritionUI()
            }
    }

    private fun updateNutritionUI() {
        val currentUserInfo = userInfo ?: return
        val currentTodaysNutrition = todaysNutrition ?: DailyNutrition()

        val allNutrients = listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", currentTodaysNutrition.calories.toFloat(), currentUserInfo.goalCalories.toFloat(), "kcal"),
            NutritionItem("탄수화물", "🌾", "#45b7d1", currentTodaysNutrition.carbs.toFloat(), currentUserInfo.goalCarbs.toFloat(), "g"),
            NutritionItem("단백질", "💪", "#4ecdc4", currentTodaysNutrition.protein.toFloat(), currentUserInfo.goalProtein.toFloat(), "g"),
            NutritionItem("지방", "🥑", "#f9ca24", currentTodaysNutrition.fat.toFloat(), currentUserInfo.goalFat.toFloat(), "g"),
            NutritionItem("나트륨", "🧂", "#A5D6A7", currentTodaysNutrition.sodium.toFloat(), 2000f, "mg"),
            NutritionItem("칼슘", "🦴", "#B0BEC5", currentTodaysNutrition.calcium.toFloat(), 1000f, "mg"),
            NutritionItem("철분", "🩸", "#EF9A9A", currentTodaysNutrition.iron.toFloat(), 18f, "mg"),
            NutritionItem("비타민A", "🥕", "#FFAB91", currentTodaysNutrition.vitaminA.toFloat(), 900f, "μg"),
            NutritionItem("비타민C", "🍊", "#FFCC80", currentTodaysNutrition.vitaminC.toFloat(), 100f, "mg")
        )
        _allNutritionItems.value = allNutrients.map {
            it.copy(isDeficient = it.current < it.goal * 0.5)
        }
    }

    private fun loadFoods() {
        _isFoodsLoading.value = true
        db.collection("foods")
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                _foods.value = result.toObjects(Food::class.java)
                _isFoodsLoading.value = false
            }
            .addOnFailureListener { exception ->
                Log.w("HomeViewModel", "Error getting documents: ", exception)
                _isFoodsLoading.value = false
            }
    }

    // 4. '더보기/접기' 상태를 변경하는 함수 (내부 상태값만 변경)
    fun toggleNutritionExpansion() {
        _isNutritionExpanded.value = !(_isNutritionExpanded.value ?: false)
    }

    // 5. 모든 UI 상태를 계산하고 _nutritionUIState에 발행하는 함수
    private fun updateNutritionState() {
        val fullList = _allNutritionItems.value ?: return
        val isExpanded = _isNutritionExpanded.value ?: false

        val shouldShowButtons = fullList.size > 5
        val displayList = if (isExpanded || !shouldShowButtons) {
            fullList
        } else {
            fullList.take(5)
        }

        _nutritionUIState.value = NutritionUIState(
            displayList = displayList,
            isExpanded = isExpanded,
            shouldShowButtons = shouldShowButtons
        )
    }
}