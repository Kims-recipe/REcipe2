package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.kims.recipe2.model.Ingredient

class IngredientListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> = _ingredients

    fun fetchFilteredIngredients(filterType: String, filterValue: String) {
        if (userId == null) return

        db.collection("users").document(userId).collection("ingredients")
            .whereEqualTo(filterType, filterValue) // 조건에 맞는 데이터만 필터링
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    _ingredients.value = it.toObjects<Ingredient>().map { ingredient ->
                        ingredient.id = it.documents[it.toObjects<Ingredient>().indexOf(ingredient)].id
                        ingredient
                    }
                }
            }
    }

    fun addIngredient(ingredient: Ingredient) { // Ingredient 객체를 통째로 받도록 수정
        // --- 디버깅을 위한 로그 추가 ---
        Log.d("FridgeViewModel", "addIngredient 호출됨")
        Log.d("FridgeViewModel", "현재 로그인된 User ID: $userId")
        Log.d("FridgeViewModel", "추가할 재료 정보: $ingredient")
        // ----------------------------

        if (userId == null) {
            Log.e("FridgeViewModel", "User ID가 null입니다. 로그인 상태를 확인하세요.")
            return
        }

        db.collection("users").document(userId).collection("ingredients")
            .add(ingredient) // Ingredient 객체를 직접 추가
            .addOnSuccessListener { documentReference ->
                // --- 성공/실패 로그 추가 ---
                Log.d("FridgeViewModel", "✅ 재료 추가 성공! 문서 ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FridgeViewModel", "❌ 재료 추가 실패!", e)
            }
    }
}