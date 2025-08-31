package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.Food
import com.kims.recipe2.model.Ingredient
import java.util.Calendar
import java.util.Date

class IngredientListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> = _ingredients

    private val _searchResults = MutableLiveData<List<String>>()
    val searchResults: LiveData<List<String>> = _searchResults

    // 👇 [추가] 계산된 유통기한을 Activity에 전달하기 위한 LiveData
    private val _calculatedExpirationDate = MutableLiveData<Date>()
    val calculatedExpirationDate: LiveData<Date> = _calculatedExpirationDate

    private var allFoodIngredients: List<String>? = null

    // 👇 [추가] 재료 이름으로 유통기한을 계산하는 함수
    fun calculateExpirationDateFor(ingredientName: String) {
        db.collection("food_ingredients")
            .whereEqualTo("name", ingredientName)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val food = documents.firstOrNull()?.toObject(Food::class.java)
                val shelfLife = food?.expirationDate ?: 0 // 소비기한(일)

                // 오늘 날짜 + 소비기한으로 유통기한 계산
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, shelfLife)
                _calculatedExpirationDate.value = calendar.time
            }
            .addOnFailureListener {
                // 실패 시 오늘 날짜를 기본값으로 설정하거나 다른 처리를 할 수 있음
                _calculatedExpirationDate.value = Date()
            }
    }


    fun fetchAllFoodIngredients() {
        if (allFoodIngredients != null) return

        db.collection("food_ingredients")
            .get()
            .addOnSuccessListener { documents ->
                allFoodIngredients = documents.mapNotNull { it.getString("name") }
                Log.d("AutocompleteCache", "Successfully cached ${allFoodIngredients?.size} ingredients.")
            }
            .addOnFailureListener { e ->
                Log.e("AutocompleteCache", "Error caching ingredients", e)
            }
    }

    fun searchFoodIngredients(query: String) {
        if (query.isBlank() || allFoodIngredients == null) {
            _searchResults.value = emptyList()
            return
        }
        _searchResults.value = allFoodIngredients!!.filter { it.contains(query, ignoreCase = true) }.take(10)
    }

    fun fetchFilteredIngredients(filterType: String, filterValue: String) {
        if (userId == null) {
            Log.e("IngredientListViewModel", "User ID is null. Cannot fetch ingredients.")
            return
        }

        db.collection("users").document(userId).collection("ingredients")
            .whereEqualTo(filterType, filterValue)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("IngredientListViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val ingredientListWithIds = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Ingredient::class.java)?.apply {
                            id = document.id
                        }
                    }
                    _ingredients.value = ingredientListWithIds
                }
            }
    }

    fun addIngredient(ingredient: Ingredient) {
        if (userId == null) {
            Log.e("IngredientListViewModel", "User ID가 null입니다. 로그인 상태를 확인하세요.")
            return
        }

        db.collection("food_ingredients")
            .whereEqualTo("name", ingredient.name)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val foodNutrition = if (!documents.isEmpty) {
                    documents.documents[0].toObject(Food::class.java)
                } else {
                    null
                }

                // 2. 검색된 영양 정보를 포함하여 최종 재료 객체 생성
                val ratio = (ingredient.amount / 100.0) // 100g 당 영양소 기준
                val finalIngredient = ingredient.copy(
                    calories = (foodNutrition?.calories ?: 0.0) * ratio,
                    carbs = (foodNutrition?.carbs ?: 0.0) * ratio,
                    protein = (foodNutrition?.protein ?: 0.0) * ratio,
                    fat = (foodNutrition?.fat ?: 0.0) * ratio,
                    calcium = (foodNutrition?.calcium ?: 0.0) * ratio,
                    iron = (foodNutrition?.iron ?: 0.0) * ratio,
                    sodium = (foodNutrition?.sodium ?: 0.0) * ratio,
                    vitaminA = (foodNutrition?.vitaminA ?: 0.0) * ratio,
                    vitaminC = (foodNutrition?.vitaminC ?: 0.0) * ratio
                )

                db.collection("users").document(userId).collection("ingredients")
                    .add(finalIngredient)
                    .addOnSuccessListener { documentReference ->
                        Log.d("IngredientListViewModel", "✅ 재료 추가 성공! (영양정보 포함) 문서 ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("IngredientListViewModel", "❌ 재료 추가 실패!", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("IngredientListViewModel", "❌ 재료 영양정보 검색 실패!", e)
            }
    }
    fun deleteIngredient(ingredient: Ingredient) {
        if (userId == null || ingredient.id.isBlank()) {
            Log.e("IngredientListViewModel", "유효한 사용자 ID 또는 재료 ID가 없어 재료를 삭제할 수 없습니다.")
            return
        }

        db.collection("users").document(userId).collection("ingredients").document(ingredient.id)
            .delete()
            .addOnSuccessListener {
                Log.d("IngredientListViewModel", "✅ 재료 삭제 성공: ${ingredient.name}")
            }
            .addOnFailureListener { e ->
                Log.e("IngredientListViewModel", "❌ 재료 삭제 실패: ${ingredient.name}", e)
            }
    }
}
