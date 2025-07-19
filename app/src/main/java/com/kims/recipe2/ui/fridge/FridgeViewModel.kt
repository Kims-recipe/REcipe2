package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.kims.recipe2.model.FridgeCategory
import com.kims.recipe2.model.Ingredient // 새로 만들 데이터 모델

class FridgeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // 1. 카테고리 목록 수정
    private val _categories = MutableLiveData<List<FridgeCategory>>()
    val categories: LiveData<List<FridgeCategory>> = _categories

    // 2. Firestore에서 가져온 재료 목록을 담을 LiveData 추가
    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> = _ingredients

    init {
        loadCategories()
        fetchIngredients() // ViewModel 생성 시 재료 목록을 가져옵니다.
    }

    private fun loadCategories() {
        // '기타' 대신 '유제품', '가공식품' 추가
        _categories.value = listOf(
            FridgeCategory("육류", "소고기, 돼지고기...", "🥩", "#ff6b6b"),
            FridgeCategory("채소", "양파, 당근...", "🥕", "#4ecdc4"),
            FridgeCategory("과일", "사과, 바나나...", "🍎", "#45b7d1"),
            FridgeCategory("해산물", "연어, 새우...", "🐟", "#f9ca24"),
            FridgeCategory("유제품", "우유, 치즈...", "🥛", "#a29bfe"),
            FridgeCategory("가공식품", "햄, 소시지...", "🥓", "#fd79a8")
        )
    }

    // 3. Firestore에서 재료 목록을 실시간으로 가져오는 함수
    private fun fetchIngredients() {
        if (userId == null) return

        // addSnapshotListener는 데이터가 변경될 때마다 자동으로 호출되어 UI를 실시간으로 업데이트합니다.
        db.collection("users").document(userId).collection("ingredients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FridgeViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val ingredientList = snapshot.toObjects<Ingredient>()
                    _ingredients.value = ingredientList
                }
            }
    }

    // 4. 새로운 재료를 Firestore에 추가하는 함수
    // 4. 새로운 재료를 Firestore에 추가하는 함수
//    fun addIngredient(ingredient: Ingredient) { // Ingredient 객체를 통째로 받도록 수정
//        // --- 디버깅을 위한 로그 추가 ---
//        Log.d("FridgeViewModel", "addIngredient 호출됨")
//        Log.d("FridgeViewModel", "현재 로그인된 User ID: $userId")
//        Log.d("FridgeViewModel", "추가할 재료 정보: $ingredient")
//        // ----------------------------
//
//        if (userId == null) {
//            Log.e("FridgeViewModel", "User ID가 null입니다. 로그인 상태를 확인하세요.")
//            return
//        }
//
//        db.collection("users").document(userId).collection("ingredients")
//            .add(ingredient) // Ingredient 객체를 직접 추가
//            .addOnSuccessListener { documentReference ->
//                // --- 성공/실패 로그 추가 ---
//                Log.d("FridgeViewModel", "✅ 재료 추가 성공! 문서 ID: ${documentReference.id}")
//            }
//            .addOnFailureListener { e ->
//                Log.e("FridgeViewModel", "❌ 재료 추가 실패!", e)
//            }
//    }
}