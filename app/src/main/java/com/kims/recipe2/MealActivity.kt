package com.kims.recipe2

import com.kims.recipe2.ui.fridge.HomemadeMealFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kims.recipe2.databinding.ActivityMealBinding
import com.kims.recipe2.ui.fridge.EatingOutFragment
import com.kims.recipe2.ui.fridge.FoodShotFragment
import androidx.fragment.app.commit // commitKtx 사용을 위한 import

class MealActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mealType = intent.getStringExtra("MEAL_TYPE")

        if (savedInstanceState == null) {
            val fragment = when (mealType) {
                "외식" -> EatingOutFragment()
                "집밥" -> HomemadeMealFragment()
                "푸드샷" -> FoodShotFragment() // FoodShotDummyFragment 로드
                else -> EatingOutFragment()
            }

            supportFragmentManager.commit {
                replace(R.id.meal_fragment_container, fragment)
            }
        }

        // 💡 [추가] FoodShotDummyFragment의 결과(식사 이름, 사진 URI)를 받아서 EatingOutFragment를 실행
        supportFragmentManager.setFragmentResultListener(
            "foodShotResult", // FoodShotDummyFragment에서 설정할 키
            this
        ) { requestKey, bundle ->
            val mealName = bundle.getString("mealName")
            val imageUri = bundle.getString("imageUri")
            val mealType = bundle.getString("mealType") // (아침, 점심, 저녁)

            // EatingOutFragment를 생성하고 데이터를 Bundle로 전달
            val eatingOutFragment = EatingOutFragment().apply {
                arguments = Bundle().apply {
                    putString("mealName", mealName)
                    putString("imageUri", imageUri)
                    putString("mealType", mealType)
                    putBoolean("fromFoodShot", true) // FoodShot에서 왔음을 알림
                }
            }

            // EatingOutFragment로 교체
            supportFragmentManager.commit {
                replace(R.id.meal_fragment_container, eatingOutFragment)
            }
        }
    }
}