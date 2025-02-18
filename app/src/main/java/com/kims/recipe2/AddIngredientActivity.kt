package com.kims.recipe2

import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.UUID

class AddIngredientActivity : AppCompatActivity() {

    private val firestoreManager = FirestoreManager()
    private lateinit var storageType: StorageType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_ingredient)

        // Intent에서 현재 선택된 저장 타입 받기
        storageType = intent.getSerializableExtra("storageType") as? StorageType ?: StorageType.REFRIGERATOR

        setupToolbar()
        setupAddButton()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title = "${storageType.getDisplayName()}에 재료 추가"
    }

    private fun setupAddButton() {
        val btnAddIngredient = findViewById<MaterialButton>(R.id.btnAddIngredient)
        val etName = findViewById<TextInputEditText>(R.id.etIngredientName)
        val etAmount = findViewById<TextInputEditText>(R.id.etIngredientAmount)
        val datePicker = findViewById<DatePicker>(R.id.dpExpiration)

        btnAddIngredient.setOnClickListener {
            val ingredientName = etName.text.toString()

            if (ingredientName.isBlank()) {
                Toast.makeText(this, "재료 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = etAmount.text.toString().toIntOrNull() ?: 0

            // 유통기한 날짜 설정
            val calendar = Calendar.getInstance()
            calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
            val expirationDate = calendar.time

            // 새 재료 객체 생성
            val newIngredient = IngredientItem(
                id = UUID.randomUUID().toString(),
                name = ingredientName,
                storageType = storageType,
                number = amount,
                expiration = expirationDate,
                imageUrl = null  // 이미지는 나중에 추가 가능
            )

            // Firestore에 저장
            saveIngredientToFirestore(newIngredient)
        }
    }

    private fun saveIngredientToFirestore(ingredient: IngredientItem) {
        firestoreManager.saveIngredient(
            ingredient = ingredient,
            onSuccess = {
                Toast.makeText(this, "${ingredient.name} 추가 완료", Toast.LENGTH_SHORT).show()
                finish() // 저장 후 액티비티 종료
            },
            onFailure = { exception ->
                Toast.makeText(this, "추가 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}