package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.kims.recipe2.model.FridgeCategory
import com.kims.recipe2.model.Ingredient // 새로 만들 데이터 모델

data class IngredientUIState(
    val displayList: List<Ingredient> = emptyList(),
    val isExpanded: Boolean = false,
    val shouldShowButton: Boolean = false
)

class FridgeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // 1. 카테고리 목록 수정
    private val _categories = MutableLiveData<List<FridgeCategory>>()
    val categories: LiveData<List<FridgeCategory>> = _categories

    // 2. Firestore에서 가져온 재료 목록을 담을 LiveData 추가
    private val _ingredients = MutableLiveData<List<Ingredient>>()
    val ingredients: LiveData<List<Ingredient>> = _ingredients

    // ▼▼▼ [추가] UI 상태 관리 로직 ▼▼▼
    private val _isIngredientsExpanded = MutableLiveData(false)

    private val _ingredientUIState = MediatorLiveData<IngredientUIState>()
    val ingredientUIState: LiveData<IngredientUIState> = _ingredientUIState
    // ▲▲▲ [추가] 여기까지 ▲▲▲

    init {
        loadCategories()
        fetchIngredients() // ViewModel 생성 시 재료 목록을 가져옵니다.
        // MediatorLiveData에 소스 연결
        _ingredientUIState.addSource(_ingredients) { updateIngredientState() }
        _ingredientUIState.addSource(_isIngredientsExpanded) { updateIngredientState() }
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
    // ▼▼▼ [추가] '더보기/접기' 상태를 변경하는 함수 ▼▼▼
    fun toggleIngredientsExpansion() {
        _isIngredientsExpanded.value = !(_isIngredientsExpanded.value ?: false)
    }
    // ▲▲▲ [추가] 여기까지 ▲▲▲

    // ▼▼▼ [추가] UI 상태를 계산하고 발행하는 함수 ▼▼▼
    private fun updateIngredientState() {
        val fullList = _ingredients.value ?: emptyList()
        val isExpanded = _isIngredientsExpanded.value ?: false

        // 재료가 5개 초과일 때만 '더보기/접기' 버튼을 표시
        val shouldShowButton = fullList.size > 5

        // 확장 상태나 버튼이 필요 없는 경우 전체 목록, 그 외엔 5개만 표시
        val displayList = if (isExpanded || !shouldShowButton) {
            fullList
        } else {
            fullList.take(5)
        }

        _ingredientUIState.value = IngredientUIState(
            displayList = displayList,
            isExpanded = isExpanded,
            shouldShowButton = shouldShowButton
        )
    }
    // ▲▲▲ [추가] 여기까지 ▲▲▲

    // 3. Firestore에서 재료 목록을 실시간으로 가져오는 함수
    private fun fetchIngredients() {
        if (userId == null) {
            Log.e("FridgeViewModel", "User ID is null. Cannot fetch ingredients.")
            return
        }

        db.collection("users").document(userId).collection("ingredients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FridgeViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // 각 문서의 ID를 Ingredient 객체에 직접 할당하는 부분이 핵심입니다.
                    val ingredientListWithIds = snapshot.documents.mapNotNull { document ->
                        document.toObject(Ingredient::class.java)?.apply {
                            id = document.id // << 이 한 줄이 핵심
                        }
                    }
                    _ingredients.value = ingredientListWithIds
                    Log.d("FridgeViewModel", "Fetched ${ingredientListWithIds.size} ingredients with IDs.")
                } else {
                    Log.d("FridgeViewModel", "Snapshot is null.")
                }
            }
    }

    fun consumeIngredient(ingredient: Ingredient, consumedQuantity: Int) {
        if (userId == null || ingredient.id.isBlank()) {
            Log.e("FridgeViewModel", "유효한 사용자 ID 또는 재료 ID가 없어 재료를 소비할 수 없습니다.")
            return
        }

        val ingredientRef = db.collection("users").document(userId).collection("ingredients").document(ingredient.id)

        ingredientRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val currentQuantity = documentSnapshot.getLong("quantity") ?: 0L
                    val expectedNewQuantity = currentQuantity - consumedQuantity

                    if (expectedNewQuantity <= 0) {
                        ingredientRef.delete()
                            .addOnSuccessListener { Log.d("FridgeViewModel", "✅ 재료 삭제 성공: ${ingredient.name}") }
                            .addOnFailureListener { e -> Log.e("FridgeViewModel", "❌ 재료 삭제 실패: ${ingredient.name}", e) }
                    } else {
                        ingredientRef.update("quantity", FieldValue.increment(-consumedQuantity.toLong()))
                            .addOnSuccessListener { Log.d("FridgeViewModel", "✅ 재료 수량 차감 성공: ${ingredient.name}, 새 수량: $expectedNewQuantity") }
                            .addOnFailureListener { e -> Log.e("FridgeViewModel", "❌ 재료 수량 차감 실패: ${ingredient.name}", e) }
                    }
                } else {
                    Log.e("FridgeViewModel", "Firestore에 재료 문서가 존재하지 않습니다: ${ingredient.name}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FridgeViewModel", "재료 문서 가져오기 실패: ${ingredient.name}", e)
            }
    }

    fun deleteIngredient(ingredient: Ingredient) {
        if (userId == null || ingredient.id.isBlank()) {
            Log.e("FridgeViewModel", "유효한 사용자 ID 또는 재료 ID가 없어 재료를 삭제할 수 없습니다.")
            return
        }

        db.collection("users").document(userId).collection("ingredients").document(ingredient.id)
            .delete()
            .addOnSuccessListener {
                Log.d("FridgeViewModel", "✅ 재료 삭제 성공: ${ingredient.name}")
            }
            .addOnFailureListener { e ->
                Log.e("FridgeViewModel", "❌ 재료 삭제 실패: ${ingredient.name}", e)
            }
    }
}