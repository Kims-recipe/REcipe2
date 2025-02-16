package com.kims.recipe2

// Firebase 초기화 및 Firestore 인스턴스 가져오기
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class FirestoreManager {
    private val db: FirebaseFirestore = Firebase.firestore
    private val ingredientsCollection = "ingredients"

    // IngredientItem을 Firestore에 저장하는 함수
    fun saveIngredient(
        ingredient: IngredientItem,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val ingredientMap = hashMapOf(
            "id" to ingredient.id,
            "name" to ingredient.name,
            "storageType" to ingredient.storageType.toString(),
            "number" to ingredient.number,
            "expiration" to ingredient.expiration,
            "imageUrl" to ingredient.imageUrl
        )

        db.collection(ingredientsCollection)
            .document(ingredient.id)
            .set(ingredientMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Firestore에서 IngredientItem 가져오는 함수
    fun getIngredient(
        ingredientId: String,
        onSuccess: (IngredientItem) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(ingredientsCollection)
            .document(ingredientId)
            .get()
            .addOnSuccessListener { document ->
                document.data?.let { data ->
                    val ingredient = IngredientItem(
                        id = data["id"] as String,
                        name = data["name"] as String,
                        storageType = StorageType.valueOf(data["storageType"] as String),
                        number = (data["number"] as Long).toInt(),
                        expiration = data["expiration"] as? Date,
                        imageUrl = data["imageUrl"] as? String
                    )
                    onSuccess(ingredient)
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // 모든 IngredientItem 목록 가져오기
    fun getAllIngredients(
        onSuccess: (List<IngredientItem>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(ingredientsCollection)
            .get()
            .addOnSuccessListener { documents ->
                val ingredientsList = documents.mapNotNull { document ->
                    val data = document.data
                    IngredientItem(
                        id = data["id"] as String,
                        name = data["name"] as String,
                        storageType = StorageType.valueOf(data["storageType"] as String),
                        number = (data["number"] as Long).toInt(),
                        expiration = data["expiration"] as? Date,
                        imageUrl = data["imageUrl"] as? String
                    )
                }
                onSuccess(ingredientsList)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // IngredientItem 업데이트
    fun updateIngredient(
        ingredient: IngredientItem,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        saveIngredient(ingredient, onSuccess, onFailure)
    }

    // IngredientItem 삭제
    fun deleteIngredient(
        ingredientId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(ingredientsCollection)
            .document(ingredientId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // 실시간 데이터 감지
    fun observeIngredients(onChange: (List<IngredientItem>) -> Unit) {
        db.collection(ingredientsCollection)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FirestoreManager", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val ingredients = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        IngredientItem(
                            id = data["id"] as String,
                            name = data["name"] as String,
                            storageType = StorageType.valueOf(data["storageType"] as String),
                            number = (data["number"] as Long).toInt(),
                            expiration = data["expiration"] as? Date,
                            imageUrl = data["imageUrl"] as? String
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: listOf()

                onChange(ingredients)
            }
    }
}