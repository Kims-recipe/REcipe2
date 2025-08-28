package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.Food
import com.kims.recipe2.model.Ingredient

class IngredientListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> = _ingredients

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
                    Log.d("IngredientListViewModel", "Fetched ${ingredientListWithIds.size} ingredients with IDs.")
                } ?: Log.d("IngredientListViewModel", "Snapshot is null.")
            }
    }

    // 👇 재료 추가 로직 수정
    fun addIngredient(ingredient: Ingredient) {
        if (userId == null) {
            Log.e("IngredientListViewModel", "User ID가 null입니다. 로그인 상태를 확인하세요.")
            return
        }

        // 1. food_ingredients 컬렉션에서 재료 이름으로 영양 정보 검색
        db.collection("food_ingredients")
            .whereEqualTo("name", ingredient.name)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val foodNutrition = if (!documents.isEmpty) {
                    documents.documents[0].toObject(Food::class.java)
                } else {
                    null // 검색 결과가 없으면 null
                }

                // 2. 검색된 영양 정보를 포함하여 최종 재료 객체 생성
                val finalIngredient = ingredient.copy(
                    calories = foodNutrition?.calories ?: 0.0,
                    carbs = foodNutrition?.carbs ?: 0.0,
                    protein = foodNutrition?.protein ?: 0.0,
                    fat = foodNutrition?.fat ?: 0.0,
                    calcium = foodNutrition?.calcium ?: 0.0,
                    iron = foodNutrition?.iron ?: 0.0,
                    sodium = foodNutrition?.sodium ?: 0.0,
                    vitaminA = foodNutrition?.vitaminA ?: 0.0,
                    vitaminC = foodNutrition?.vitaminC ?: 0.0
                )

                // 3. 영양 정보가 포함된 재료를 사용자의 ingredients 컬렉션에 저장
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
                // 만약 검색에 실패하더라도 재료는 추가하고 싶다면, 여기에 영양정보 없이 추가하는 코드를 넣을 수 있습니다.
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
