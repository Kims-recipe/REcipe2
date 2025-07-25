package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.model.MealRecord
import java.util.UUID

// FridgeViewModel을 주입받기 위해 생성자에 추가 (Hilt/Koin 같은 DI 라이브러리 사용 시 더 쉬움)
// ViewModelProvider.Factory를 사용해야 할 수도 있지만, 여기서는 간단하게 인스턴스화를 가정
class MealViewModel(private val fridgeViewModel: FridgeViewModel) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // 식사 기록을 저장하는 함수
    fun saveMealRecord(
        mealName: String,
        mealType: String,
        selectedIngredients: List<Ingredient>,
        imageUri: String?,
        onSuccess: () -> Unit, // 성공 시 호출될 콜백
        onFailure: (Exception) -> Unit // 실패 시 호출될 콜백
    ) {
        if (userId == null) {
            val e = IllegalStateException("User ID is null. Cannot save meal record.")
            Log.e("MealViewModel", e.message, e)
            onFailure(e)
            return
        }
        if (mealName.isEmpty() || selectedIngredients.isEmpty()) {
            val e = IllegalArgumentException("Meal name or ingredients cannot be empty.")
            Log.e("MealViewModel", e.message, e)
            onFailure(e)
            return
        }

        // TODO: 실제 칼로리/단백질 계산 로직 필요. 현재는 amount * 1.0으로 가정.
        val totalCalories = selectedIngredients.sumOf { it.amount * 1.0 }.toInt()
        val totalProtein = selectedIngredients.sumOf { it.amount * 1.0 }.toInt()

        val mealRecord = MealRecord(
            id = UUID.randomUUID().toString(), // MealRecord 데이터 클래스에 id 필드가 있다면 사용
            name = mealName,
            type = mealType,
            calories = totalCalories,
            protein = totalProtein,
            date = null, // @ServerTimestamp가 자동으로 채워줄 것
            isPlanned = false
        )

        // MealRecord에 포함될 재료 정보 (필요한 데이터만 매핑)
        val ingredientsForMealRecord = selectedIngredients.map {
            mapOf(
                "id" to it.id, // 재료 ID도 함께 기록하면 나중에 식단 상세 보기 시 유용
                "name" to it.name,
                "category" to it.category,
                "quantity" to it.quantity,
                "unit" to it.unit,
                "amount" to it.amount // amount도 함께 기록
            )
        }

        val recordMap = hashMapOf(
            "id" to mealRecord.id,
            "name" to mealRecord.name,
            "type" to mealRecord.type,
            "calories" to mealRecord.calories,
            "protein" to mealRecord.protein,
            "date" to FieldValue.serverTimestamp(), // 서버 타임스탬프
            "isPlanned" to mealRecord.isPlanned,
            "ingredients" to ingredientsForMealRecord, // 매핑된 재료 리스트
            "imageUri" to imageUri.orEmpty()
        )

        db.collection("users").document(userId).collection("mealRecords")
            .add(recordMap) // MealRecord 객체 대신 Map을 직접 추가하여 ServerTimestamp 처리
            .addOnSuccessListener {
                Log.d("MealViewModel", "✅ 식사 기록 Firestore 저장 성공!")
                // 식사 기록 성공 후, 재료 소모 로직 호출 (FridgeViewModel 위임)
                selectedIngredients.forEach { ingredient ->
                    fridgeViewModel.consumeIngredient(ingredient, ingredient.quantity)
                }
                onSuccess() // UI에 성공을 알림
            }
            .addOnFailureListener { e ->
                Log.e("MealViewModel", "❌ 식사 기록 Firestore 저장 실패!", e)
                onFailure(e) // UI에 실패를 알림
            }
    }
}