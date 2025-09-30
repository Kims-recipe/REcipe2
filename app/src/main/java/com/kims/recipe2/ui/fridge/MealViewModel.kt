package com.kims.recipe2.ui.fridge

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.kims.recipe2.model.*
import java.text.SimpleDateFormat
import java.util.*

class MealViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // 👇 [추가] 사용자의 레시피 목록을 담을 LiveData
    private val _userRecipes = MutableLiveData<List<UserRecipe>>()
    val userRecipes: LiveData<List<UserRecipe>> = _userRecipes

    // 👇 [추가] 사용자의 모든 레시피를 불러오는 함수
    fun fetchUserRecipes() {
        if (userId == null) return
        db.collection("users").document(userId).collection("recipes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MealViewModel", "레시피 로딩 실패", error)
                    return@addSnapshotListener
                }
                val recipes = snapshot?.map { doc ->
                    doc.toObject(UserRecipe::class.java).apply { id = doc.id }
                } ?: emptyList()
                _userRecipes.value = recipes
            }
    }

    // 👇 [추가] 새로운 레시피를 저장하는 함수
    fun saveUserRecipe(recipeName: String, ingredients: List<Ingredient>) {
        if (userId == null || recipeName.isBlank() || ingredients.isEmpty()) return

        val ingredientMap = ingredients.map {
            mapOf(
                "name" to it.name,
                "quantity" to it.quantity,
                "unit" to it.unit
                // 레시피에는 간단한 정보만 저장, 영양소는 재료 원본을 따름
            )
        }
        val newRecipe = UserRecipe(name = recipeName, ingredients = ingredientMap)

        db.collection("users").document(userId).collection("recipes")
            .add(newRecipe)
            .addOnSuccessListener { Log.d("MealViewModel", "✅ 새로운 레시피 저장 성공!") }
            .addOnFailureListener { e -> Log.e("MealViewModel", "❌ 새로운 레시피 저장 실패!", e) }
    }


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
            "calories" to totalNutrition.calories,
            "carbs" to totalNutrition.carbs,
            "protein" to totalNutrition.protein,
            "fat" to totalNutrition.fat,
            "calcium" to totalNutrition.calcium,
            "iron" to totalNutrition.iron,
            "sodium" to totalNutrition.sodium,
            "vitaminA" to totalNutrition.vitaminA,
            "vitaminC" to totalNutrition.vitaminC,
            "date" to FieldValue.serverTimestamp(),
            "isPlanned" to false,
            "ingredients" to ingredientsForMealRecord,
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
                    "calories" to (food?.calories ?: 0.0),
                    "carbs" to (food?.carbs ?: 0.0),
                    "protein" to (food?.protein ?: 0.0),
                    "fat" to (food?.fat ?: 0.0),
                    "calcium" to (food?.calcium ?: 0.0),
                    "iron" to (food?.iron ?: 0.0),
                    "sodium" to (food?.sodium ?: 0.0),
                    "vitaminA" to (food?.vitaminA ?: 0.0),
                    "vitaminC" to (food?.vitaminC ?: 0.0),
                    "protein" to (food?.protein ?: 0.0),
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

    fun deleteMealRecord(
        mealRecord: MealRecord,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (userId == null) {
            onFailure(IllegalStateException("User ID is null."))
            return
        }

        // ▼▼▼ [수정된 부분] mealRecord.id를 사용하여 문서 경로를 직접 지정 ▼▼▼
        if (mealRecord.id.isBlank()) {
            onFailure(IllegalStateException("MealRecord ID is empty."))
            return
        }

        // MealRecord의 영양정보를 일일 영양정보에서 차감
        val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val dailyNutritionRef = db.collection("users").document(userId)
            .collection("dailyNutrition").document(todayDateString)
        val mealRecordRef = db.collection("users").document(userId)
            .collection("mealRecords").document(mealRecord.id) // 문서 ID로 직접 참조

        db.runTransaction { transaction ->
            // 일일 영양정보에서 차감
            val dailySnapshot = transaction.get(dailyNutritionRef)
            if (dailySnapshot.exists()) {
                // isHomemade 필드로 집밥/외식 구분하여 차감 로직 실행
                if (mealRecord.isHomemade) {
                    val updates = hashMapOf<String, Any>(
                        "calories" to FieldValue.increment(-mealRecord.calories.toDouble()),
                        "carbs" to FieldValue.increment(-mealRecord.carbs.toDouble()),
                        "protein" to FieldValue.increment(-mealRecord.protein.toDouble()),
                        "fat" to FieldValue.increment(-mealRecord.fat),
                        "sodium" to FieldValue.increment(-mealRecord.sodium),
                        "calcium" to FieldValue.increment(-mealRecord.calcium),
                        "iron" to FieldValue.increment(-mealRecord.iron),
                        "vitaminA" to FieldValue.increment(-mealRecord.vitaminA),
                        "vitaminC" to FieldValue.increment(-mealRecord.vitaminC)
                    )
                    transaction.update(dailyNutritionRef, updates)
                } else {
                    // 외식인 경우, Food DB를 다시 조회해서 모든 영양소 차감 (구현의 복잡성을 고려하여 단순화 가능)
                    // 현재는 칼로리/단백질만 차감
                    val updates = hashMapOf<String, Any>(
                        "calories" to FieldValue.increment(-mealRecord.calories.toDouble()),
                        "protein" to FieldValue.increment(-mealRecord.protein.toDouble()),
                        "fat" to FieldValue.increment(-mealRecord.fat),
                        "sodium" to FieldValue.increment(-mealRecord.sodium),
                        "calcium" to FieldValue.increment(-mealRecord.calcium),
                        "iron" to FieldValue.increment(-mealRecord.iron),
                        "vitaminA" to FieldValue.increment(-mealRecord.vitaminA),
                        "vitaminC" to FieldValue.increment(-mealRecord.vitaminC)
                    )
                    transaction.update(dailyNutritionRef, updates)
                }
            }
            // MealRecord 삭제
            transaction.delete(mealRecordRef)
            null
        }.addOnSuccessListener {
            Log.d("MealViewModel", "✅ 식단 기록 삭제 성공!")
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("MealViewModel", "❌ 식단 기록 삭제 실패!", e)
            onFailure(e)
        }
    }
}