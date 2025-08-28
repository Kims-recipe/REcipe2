package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.DailyNutrition
import com.kims.recipe2.model.Food
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.model.MealRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MealViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // 👇 집밥 기록 로직 수정
    fun saveMealRecord(
        mealName: String,
        mealType: String,
        selectedIngredients: List<Ingredient>,
        imageUri: String?,
        isHomemade: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (userId == null) {
            onFailure(IllegalStateException("User ID is null."))
            return
        }
        if (mealName.isEmpty() || selectedIngredients.isEmpty()) {
            onFailure(IllegalArgumentException("Meal name or ingredients cannot be empty."))
            return
        }

        // 1. 전달받은 재료 리스트에서 직접 영양소 총합 계산 (DB 접근 불필요)
        val totalNutrition = DailyNutrition(
            calories = selectedIngredients.sumOf { it.calories },
            carbs = selectedIngredients.sumOf { it.carbs },
            protein = selectedIngredients.sumOf { it.protein },
            fat = selectedIngredients.sumOf { it.fat },
            sodium = selectedIngredients.sumOf { it.sodium },
            calcium = selectedIngredients.sumOf { it.calcium },
            iron = selectedIngredients.sumOf { it.iron },
            vitaminA = selectedIngredients.sumOf { it.vitaminA },
            vitaminC = selectedIngredients.sumOf { it.vitaminC }
        )

        // 2. mealRecords에 저장할 데이터 맵 준비
        val ingredientsForMealRecord = selectedIngredients.map {
            mapOf(
                "id" to it.id, "name" to it.name, "category" to it.category,
                "quantity" to it.quantity, "unit" to it.unit, "amount" to it.amount
            )
        }
        val recordMap = hashMapOf(
            "id" to UUID.randomUUID().toString(),
            "name" to mealName,
            "type" to mealType,
            "calories" to totalNutrition.calories.toInt(),
            "protein" to totalNutrition.protein.toInt(),
            "date" to FieldValue.serverTimestamp(),
            "isPlanned" to false,
            "ingredients" to ingredientsForMealRecord,
            "imageUri" to imageUri.orEmpty(),
            "isHomemade" to isHomemade
        )

        // 3. 트랜잭션 실행
        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val dailyNutritionRef = db.collection("users").document(userId)
            .collection("dailyNutrition").document(todayDateString)
        val newMealRecordRef = db.collection("users").document(userId)
            .collection("mealRecords").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(dailyNutritionRef)
            if (snapshot.exists()) {
                val updates = hashMapOf<String, Any>(
                    "calories" to FieldValue.increment(totalNutrition.calories),
                    "carbs" to FieldValue.increment(totalNutrition.carbs),
                    "protein" to FieldValue.increment(totalNutrition.protein),
                    "fat" to FieldValue.increment(totalNutrition.fat),
                    "sodium" to FieldValue.increment(totalNutrition.sodium),
                    "calcium" to FieldValue.increment(totalNutrition.calcium),
                    "iron" to FieldValue.increment(totalNutrition.iron),
                    "vitaminA" to FieldValue.increment(totalNutrition.vitaminA),
                    "vitaminC" to FieldValue.increment(totalNutrition.vitaminC)
                )
                transaction.update(dailyNutritionRef, updates)
            } else {
                transaction.set(dailyNutritionRef, totalNutrition.copy(date = Date()))
            }
            transaction.set(newMealRecordRef, recordMap)
            null
        }.addOnSuccessListener {
            Log.d("MealViewModel", "✅ 집밥 기록 및 영양정보 업데이트 성공!")
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("MealViewModel", "❌ 집밥 기록 트랜잭션 실패!", e)
            onFailure(e)
        }
    }

    // (saveEatingOutRecord 함수는 변경 없이 그대로 유지)
    fun saveEatingOutRecord(
        mealName: String,
        mealType: String,
        imageUri: String?,
        isHomemade: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onFailure(IllegalStateException("User ID is null."))
            return
        }

        db.collection("foods")
            .whereEqualTo("name", mealName)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val food = if (!documents.isEmpty) {
                    documents.documents[0].toObject(Food::class.java)
                } else {
                    null
                }

                val recordMap = hashMapOf(
                    "id" to UUID.randomUUID().toString(),
                    "name" to mealName,
                    "type" to mealType,
                    "calories" to (food?.calories?.toInt() ?: 0),
                    "protein" to (food?.protein?.toInt() ?: 0),
                    "date" to FieldValue.serverTimestamp(),
                    "isPlanned" to false,
                    "imageUri" to imageUri.orEmpty(),
                    "isHomemade" to isHomemade
                )

                val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
                val dailyNutritionRef = db.collection("users").document(userId)
                    .collection("dailyNutrition").document(todayDateString)
                val newMealRecordRef = db.collection("users").document(userId)
                    .collection("mealRecords").document()

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(dailyNutritionRef)
                    if (snapshot.exists()) {
                        val updates = hashMapOf<String, Any>(
                            "calories" to FieldValue.increment(food?.calories ?: 0.0),
                            "carbs" to FieldValue.increment(food?.carbs ?: 0.0),
                            "protein" to FieldValue.increment(food?.protein ?: 0.0),
                            "fat" to FieldValue.increment(food?.fat ?: 0.0),
                            "sodium" to FieldValue.increment(food?.sodium ?: 0.0),
                            "calcium" to FieldValue.increment(food?.calcium ?: 0.0),
                            "iron" to FieldValue.increment(food?.iron ?: 0.0),
                            "vitaminA" to FieldValue.increment(food?.vitaminA ?: 0.0),
                            "vitaminC" to FieldValue.increment(food?.vitaminC ?: 0.0)
                        )
                        transaction.update(dailyNutritionRef, updates)
                    } else {
                        val newDailyData = DailyNutrition(
                            date = Date(),
                            calories = food?.calories ?: 0.0, carbs = food?.carbs ?: 0.0,
                            protein = food?.protein ?: 0.0, fat = food?.fat ?: 0.0,
                            sodium = food?.sodium ?: 0.0, calcium = food?.calcium ?: 0.0,
                            iron = food?.iron ?: 0.0, vitaminA = food?.vitaminA ?: 0.0,
                            vitaminC = food?.vitaminC ?: 0.0
                        )
                        transaction.set(dailyNutritionRef, newDailyData)
                    }
                    transaction.set(newMealRecordRef, recordMap)
                    null
                }.addOnSuccessListener {
                    Log.d("MealViewModel", "✅ 외식 기록 및 일일 영양정보 업데이트 성공!")
                    onSuccess()
                }.addOnFailureListener { e ->
                    Log.e("MealViewModel", "❌ 외식 기록 트랜잭션 실패!", e)
                    onFailure(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MealViewModel", "❌ 'foods' 컬렉션 검색 실패!", e)
                onFailure(e)
            }
    }
    // 👇 식단 기록 삭제 함수 추가
    fun deleteMealRecord(mealRecord: MealRecord, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            onFailure(IllegalStateException("User ID is null."))
            return
        }

        if (mealRecord.id.isEmpty()) {
            onFailure(IllegalArgumentException("Meal record ID is empty."))
            return
        }

        db.collection("users").document(userId).collection("mealRecords").document(mealRecord.id)
            .delete()
            .addOnSuccessListener {
                Log.d("MealViewModel", "✅ 식단 기록 삭제 성공: ${mealRecord.name}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("MealViewModel", "❌ 식단 기록 삭제 실패: ${mealRecord.name}", e)
                onFailure(e)
            }
    }
}