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

    // 👇 '부족한 영양소' LiveData는 이제 필요 없으므로 삭제합니다.
    // private val _deficientList = MutableLiveData<List<NutritionItem>>()
    // val deficientList: LiveData<List<NutritionItem>> = _deficientList

    private val _foods = MutableLiveData<List<Food>>()
    val foods: LiveData<List<Food>> = _foods

    private val _isFoodsLoading = MutableLiveData<Boolean>()
    val isFoodsLoading: LiveData<Boolean> = _isFoodsLoading

    private val _priorityIngredients = MutableLiveData<List<Ingredient>>()
    val priorityIngredients: LiveData<List<Ingredient>> = _priorityIngredients

    private var userInfo: UserInfo? = null
    private var todaysNutrition: DailyNutrition? = null

    init {
        loadTodayDate()
        loadFoods()
        fetchInitialData()
        fetchPriorityIngredients()
    }

    private fun loadTodayDate() {
        val sdf = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
        _todayDate.value = sdf.format(Date())
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

    // 👇 [수정] 하나의 리스트를 만들면서 부족 여부를 함께 판단
    private fun updateNutritionUI() {
        val currentUserInfo = userInfo ?: return
        val currentTodaysNutrition = todaysNutrition ?: DailyNutrition()

        val allNutrients = listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", currentTodaysNutrition.calories.toFloat(), currentUserInfo.goalCalories.toFloat(), "kcal"),
            NutritionItem("탄수화물", "🌾", "#45b7d1", currentTodaysNutrition.carbs.toFloat(), currentUserInfo.goalCarbs.toFloat(), "g"),
            NutritionItem("단백질", "💪", "#4ecdc4", currentTodaysNutrition.protein.toFloat(), currentUserInfo.goalProtein.toFloat(), "g"),
            NutritionItem("지방", "🥑", "#f9ca24", currentTodaysNutrition.fat.toFloat(), currentUserInfo.goalFat.toFloat(), "g"),
            // 추가적인 영양소들 (예시)
            NutritionItem("나트륨", "🧂", "#A5D6A7", currentTodaysNutrition.sodium.toFloat(), 2000f, "mg"),
            NutritionItem("칼슘", "🦴", "#B0BEC5", currentTodaysNutrition.calcium.toFloat(), 1000f, "mg"),
            NutritionItem("철분", "🩸", "#EF9A9A", currentTodaysNutrition.iron.toFloat(), 18f, "mg"),
            NutritionItem("비타민A", "🥕", "#FFAB91", currentTodaysNutrition.vitaminA.toFloat(), 900f, "μg"), // 비타민 A 추가
            NutritionItem("비타민C", "🍊", "#FFCC80", currentTodaysNutrition.vitaminC.toFloat(), 100f, "mg")
        )

        // 각 영양소가 부족한지(목표량의 50% 미만) 판단하여 isDeficient 플래그 설정
        _nutritionList.value = allNutrients.map {
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
}