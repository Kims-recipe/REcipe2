package com.kims.recipe2.ui.food

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.kims.recipe2.R
import com.kims.recipe2.ml.FoodDetector
import com.kims.recipe2.ml.FoodMapper
import kotlinx.coroutines.launch

class FoodDetectActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var nutrientButton: Button
    private val detector = FoodDetector()
    private var detectedFoodCode: String? = null  // 라벨링 결과 저장

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_detect)

        imageView = findViewById(R.id.foodImageView)
        resultText = findViewById(R.id.resultText)
        nutrientButton = findViewById(R.id.nutrientButton)

        findViewById<Button>(R.id.cameraButton).setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .compress(1024)
                .start()
        }

        findViewById<Button>(R.id.galleryButton).setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()
                .crop()
                .compress(1024)
                .start()
        }
        nutrientButton.setOnClickListener {
            val code = detectedFoodCode
            if (code != null) {
                callGetNutrients(code)
            } else {
                Toast.makeText(this, "음식이 먼저 인식되어야 합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data!!
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            imageView.setImageBitmap(bitmap)
            resultText.text = "이미지 분석 중..."

            lifecycleScope.launch {
                val results = detector.detect(bitmap)
                if (results.isEmpty()) {
                    resultText.text = "음식을 인식하지 못했습니다"
                    return@launch
                }

                val best = results.maxByOrNull { it.confidence }!!
                val name = best.name
                val confidence = best.confidence
                resultText.text = "인식된 음식: $name ($confidence)"
                val code = FoodMapper.getCode(name)

                if (code != null) {
                    detectedFoodCode = code
                    nutrientButton.visibility = View.VISIBLE  // 버튼 보이기
                } else {
                    detectedFoodCode = null
                    nutrientButton.visibility = View.GONE
                    Toast.makeText(this@FoodDetectActivity, "매핑된 코드 없음", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun callGetNutrients(foodCode: String) {
        val items = listOf(mapOf("foodCode" to foodCode, "grams" to 100))
        Firebase.functions
            .getHttpsCallable("getNutrients")
            .call(mapOf("items" to items))
            .addOnSuccessListener { result ->
                val data = result.data as Map<*, *>
                val summary = """
                    열량: ${data["kcal"]} kcal
                    탄수화물: ${data["carbs"]} g
                    단백질: ${data["protein"]} g
                    지방: ${data["fat"]} g
                """.trimIndent()
                resultText.text = resultText.text.toString() + "\n\n$summary"
            }
            .addOnFailureListener {
                Toast.makeText(this, "영양 정보 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
    }
}