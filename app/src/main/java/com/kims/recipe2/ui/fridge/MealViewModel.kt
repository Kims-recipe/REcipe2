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

    // 식사 기록을 저장하는 함수
    fun saveMealRecord(
        mealName: String,
        mealType: String,
        selectedIngredients: List<Ingredient>,
        imageUri: String?,
        isHomemade: Boolean,
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
            isPlanned = false,
            isHomemade = true
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
            "imageUri" to imageUri.orEmpty(),
            "isHomemade" to isHomemade // isHomemade 필드 추가
        )

        db.collection("users").document(userId).collection("mealRecords")
            .add(recordMap) // MealRecord 객체 대신 Map을 직접 추가하여 ServerTimestamp 처리
            .addOnSuccessListener {
                Log.d("MealViewModel", "✅ 식사 기록 Firestore 저장 성공!")
                onSuccess() // UI에 성공을 알림
            }
            .addOnFailureListener { e ->
                Log.e("MealViewModel", "❌ 식사 기록 Firestore 저장 실패!", e)
                onFailure(e) // UI에 실패를 알림
            }
    }
    // 외식 기록을 저장하는 함수
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

        // 1. 'foods' 컬렉션에서 음식 이름으로 검색
        db.collection("foods")
            .whereEqualTo("name", mealName)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                val food = if (!documents.isEmpty) {
                    documents.documents[0].toObject(Food::class.java)
                } else {
                    null // 검색 결과가 없으면 null
                }

                // 2. mealRecords에 저장할 데이터 맵 준비
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

                // 3. 트랜잭션 실행: dailyNutrition 업데이트 + mealRecords 생성
                // 3-1. 오늘 날짜로 문서 ID 생성 (예: "2025-08-17")
                val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
                val dailyNutritionRef = db.collection("users").document(userId)
                    .collection("dailyNutrition").document(todayDateString)
                val newMealRecordRef = db.collection("users").document(userId)
                    .collection("mealRecords").document()


                db.runTransaction { transaction ->
                    val snapshot = transaction.get(dailyNutritionRef)

                    if (snapshot.exists()) {
                        // 3-2. 문서가 이미 존재하면, 각 영양소 값을 더해줍니다.
                        // 👇👇👇 바로 이 부분에 타입을 명시해줍니다!
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
                        // 3-3. 문서가 없으면, 새로운 DailyNutrition 객체로 문서를 생성합니다.
                        val newDailyData = DailyNutrition(
                            date = Date(),
                            calories = food?.calories ?: 0.0,
                            carbs = food?.carbs ?: 0.0,
                            protein = food?.protein ?: 0.0,
                            fat = food?.fat ?: 0.0,
                            sodium = food?.sodium ?: 0.0,
                            calcium = food?.calcium ?: 0.0,
                            iron = food?.iron ?: 0.0,
                            vitaminA = food?.vitaminA ?: 0.0,
                            vitaminC = food?.vitaminC ?: 0.0
                        )
                        transaction.set(dailyNutritionRef, newDailyData)
                    }

                    // 3-4. mealRecords에도 새로운 기록을 추가합니다.
                    transaction.set(newMealRecordRef, recordMap)
                    null
                }.addOnSuccessListener {
                    Log.d("MealViewModel", "✅ 외식 기록 및 일일 영양정보 업데이트 성공!")
                    onSuccess()
                }.addOnFailureListener { e ->
                    Log.e("MealViewModel", "❌ 트랜잭션 실패!", e)
                    onFailure(e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MealViewModel", "❌ 'foods' 컬렉션 검색 실패!", e)
                onFailure(e)
            }
    }
}