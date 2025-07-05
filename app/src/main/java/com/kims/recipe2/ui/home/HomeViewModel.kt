package com.kims.recipe2.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.Food
import com.kims.recipe2.model.NutritionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel : ViewModel() {

    // Firestore 인스턴스 초기화
    private val db = FirebaseFirestore.getInstance()

    // 오늘 날짜를 위한 LiveData
    private val _todayDate = MutableLiveData<String>()
    val todayDate: LiveData<String> = _todayDate

    // '영양 섭취 현황' 목록을 위한 LiveData
    private val _nutritionList = MutableLiveData<List<NutritionItem>>()
    val nutritionList: LiveData<List<NutritionItem>> = _nutritionList

    // '부족한 영양소' 목록을 위한 LiveData
    private val _deficientList = MutableLiveData<List<NutritionItem>>()
    val deficientList: LiveData<List<NutritionItem>> = _deficientList

    // 추천 음식(foods) 목록을 위한 LiveData
    private val _foods = MutableLiveData<List<Food>>()
    val foods: LiveData<List<Food>> = _foods

    // 음식 목록 로딩 상태를 관리하는 LiveData
    private val _isFoodsLoading = MutableLiveData<Boolean>()
    val isFoodsLoading: LiveData<Boolean> = _isFoodsLoading

    // ViewModel이 처음 생성될 때 모든 데이터를 로드합니다.
    init {
        loadTodayDate()
        loadNutritionData()
        loadFoods()
    }

    private fun loadTodayDate() {
        val sdf = SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREAN)
        _todayDate.value = sdf.format(Date())
    }

    private fun loadNutritionData() {
        // 이 부분은 향후 사용자의 실제 섭취량 데이터를 기반으로 업데이트할 수 있습니다.
        // 현재는 데모 데이터를 사용합니다.
        _nutritionList.value = listOf(
            NutritionItem("칼로리", "🔥", "#ff6b6b", 1650f, 2000f, "kcal"),
            NutritionItem("단백질", "💪", "#4ecdc4", 45f, 60f, "g"),
            NutritionItem("탄수화물", "🌾", "#45b7d1", 180f, 250f, "g"),
            NutritionItem("지방", "🥑", "#f9ca24", 85f, 70f, "g")
        )

        _deficientList.value = listOf(
            NutritionItem("비타민 C", "🍎", "#ff9ff3", 30f, 100f, "mg"),
            NutritionItem("오메가-3", "🐟", "#54a0ff", 0.8f, 2.0f, "g")
        )
    }

    // Firestore의 'foods' 컬렉션에서 데이터를 가져오는 함수
    private fun loadFoods() {
        _isFoodsLoading.value = true // 로딩 시작
        db.collection("foods")
            .limit(10) // 한 번에 10개만 가져오도록 제한
            .get()
            .addOnSuccessListener { result ->
                // 성공 시, Firestore 문서를 Food 객체 리스트로 자동 변환
                val foodList = result.toObjects(Food::class.java)
                _foods.value = foodList
                _isFoodsLoading.value = false // 로딩 완료
            }
            .addOnFailureListener { exception ->
                // 실패 시, 로그를 남기고 로딩 상태를 종료
                Log.w("HomeViewModel", "Error getting documents: ", exception)
                _isFoodsLoading.value = false // 로딩 완료
            }
    }
}