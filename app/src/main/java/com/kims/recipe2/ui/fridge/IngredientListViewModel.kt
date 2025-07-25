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
                        // 각 문서의 ID를 Ingredient 객체에 직접 할당
                        document.toObject(Ingredient::class.java)?.apply {
                            id = document.id // << Firestore 문서 ID를 Ingredient 객체의 id 필드에 할당
                        }
                    }
                    _ingredients.value = ingredientListWithIds
                    Log.d("IngredientListViewModel", "Fetched ${ingredientListWithIds.size} ingredients with IDs.")
                } ?: Log.d("IngredientListViewModel", "Snapshot is null.")
            }
    }

    fun addIngredient(ingredient: Ingredient) { // Ingredient 객체를 통째로 받도록 수정
        // --- 디버깅을 위한 로그 추가 ---
        Log.d("FridgeViewModel", "addIngredient 호출됨")
        Log.d("FridgeViewModel", "현재 로그인된 User ID: $userId")
        Log.d("FridgeViewModel", "추가할 재료 정보: $ingredient")
        // ----------------------------

        if (userId == null) {
            Log.e("IngredientListViewModel", "User ID가 null입니다. 로그인 상태를 확인하세요.")
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