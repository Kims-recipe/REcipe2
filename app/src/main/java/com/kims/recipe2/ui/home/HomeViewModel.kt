package com.kims.recipe2.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.DailyNutrition
import com.kims.recipe2.model.Food
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.model.NutritionItem
import com.kims.recipe2.model.UserInfo
import com.kims.recipe2.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _todayDate = MutableLiveData<String>()
    val todayDate: LiveData<String> = _todayDate

    private val _nutritionList = MutableLiveData<List<NutritionItem>>()
    val nutritionList: LiveData<List<NutritionItem>> = _nutritionList

    private val _deficientList = MutableLiveData<List<NutritionItem>>()
    val deficientList: LiveData<List<NutritionItem>> = _deficientList

    private val _foods = MutableLiveData<List<Food>>()
    val foods: LiveData<List<Food>> = _foods

    private val _isFoodsLoading = MutableLiveData<Boolean>()
    val isFoodsLoading: LiveData<Boolean> = _isFoodsLoading

    // 👇 [추가] 우선 소비 재료 LiveData
    private val _priorityIngredients = MutableLiveData<List<Ingredient>>()
    val priorityIngredients: LiveData<List<Ingredient>> = _priorityIngredients

    private var userInfo: UserInfo? = null
    private var todaysNutrition: DailyNutrition? = null

    init {
        loadTodayDate()
        loadFoods()
        fetchInitialData()
        fetchPriorityIngredients() // 👇 [추가] 우선 소비 재료 로딩 함수 호출
    }

    private fun loadTodayDate() {
        val sdf = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
        _todayDate.value = sdf.format(Date())
    }

    // 👇 [추가] 우선 소비 재료를 가져오는 함수
    private fun fetchPriorityIngredients() {
        if (userId == null) return

        db.collection("users").document(userId).collection("ingredients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "우선 소비 재료 로딩 실패", error)
                    return@addSnapshotListener
                }
                val ingredients = snapshot?.toObjects(Ingredient::class.java) ?: emptyList()

                // 유통기한과 남은 양을 기준으로 정렬
                val sortedList = ingredients.sortedWith(
                    compareBy(
                        { DateUtil.calculateDDay(it.expirationDate) ?: Long.MAX_VALUE }, // D-day 오름차순
                        { it.quantity } // 남은 양 오름차순
                    )
                ).take(5) // 상위 5개만 선택

                _priorityIngredients.value = sortedList
            }
    }


    private fun fetchInitialData() {
        if (userId == null) return

        db.collection("users").document(userId)
            .collection("userInfo").document("profile")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "사용자 정보 로딩 실패", error)
                    return@addSnapshotListener
                }
                userInfo = snapshot?.toObject(UserInfo::class.java)
                updateNutritionUI()
            }

        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        db.collection("users").document(userId)
            .collection("dailyNutrition").document(todayDateString)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "오늘 영양정보 로딩 실패", error)
                    return@addSnapshotListener
                }
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

        _nutritionList.value = listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", currentTodaysNutrition.calories.toFloat(), currentUserInfo.goalCalories.toFloat(), "kcal"),
            NutritionItem("탄수화물", "🌾", "#45b7d1", currentTodaysNutrition.carbs.toFloat(), currentUserInfo.goalCarbs.toFloat(), "g"),
            NutritionItem("단백질", "💪", "#4ecdc4", currentTodaysNutrition.protein.toFloat(), currentUserInfo.goalProtein.toFloat(), "g"),
            NutritionItem("지방", "🥑", "#f9ca24", currentTodaysNutrition.fat.toFloat(), currentUserInfo.goalFat.toFloat(), "g")
        )

        val deficientItems = mutableListOf<NutritionItem>()
        _nutritionList.value?.forEach {
            if (it.current < it.goal * 0.5) {
                deficientItems.add(it)
            }
        }
        _deficientList.value = deficientItems
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
}