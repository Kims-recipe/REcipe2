package com.kims.recipe2

import com.kims.recipe2.ui.fridge.HomemadeMealFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kims.recipe2.databinding.ActivityMealBinding
import com.kims.recipe2.ui.fridge.EatingOutFragment

class MealActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MainActivity로부터 전달받은 식사 유형 가져오기
        val mealType = intent.getStringExtra("MEAL_TYPE")

        // 식사 유형에 따라 적절한 프래그먼트 로드
        if (savedInstanceState == null) { // 액티비티가 처음 생성될 때만 프래그먼트 추가
            val fragment = when (mealType) {
                "외식" -> EatingOutFragment()
                "집밥" -> HomemadeMealFragment() // "집밥"일 경우 com.kims.recipe2.ui.fridge.HomemadeMealFragment 생성
                else -> {
                    // 기본값 또는 오류 처리 (예: 아무 프래그먼트도 로드하지 않거나 기본 프래그먼트 로드)
                    // 여기서는 EatingOutFragment를 기본으로 로드하도록 설정했습니다.
                    EatingOutFragment()
                }
            }

            // 프래그먼트 트랜잭션 시작
            supportFragmentManager.beginTransaction()
                .replace(R.id.meal_fragment_container, fragment) // meal_fragment_container는 MealActivity의 레이아웃에 프래그먼트가 들어갈 컨테이너 ID입니다.
                .commit()
        }
    }
}