package com.kims.recipe2



import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var homeContent: View
    private lateinit var fragmentContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav         = findViewById(R.id.bottom_nav)
        homeContent       = findViewById(R.id.home_content)
        fragmentContainer = findViewById(R.id.fragment_container)

        // 기본 화면 = 홈
        bottomNav.selectedItemId = R.id.bottom_nav

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment     -> { showHome();           true }
                R.id.mealFragment     -> { openFragment(MealFragment());     true }
                R.id.analysisFragment -> { openFragment(AnalysisFragment()); true }
                R.id.profileFragment  -> { openFragment(ProfileFragment());  true }
                else               -> false
            }
        }
    }

    /* ----------------- 헬퍼 ----------------- */

    private fun showHome() {
        // 모든 백스택 제거 → 홈으로 돌아옴
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        homeContent.isVisible       = true
        fragmentContainer.isVisible = false
    }

    private fun openFragment(fragment: Fragment) {
        homeContent.isVisible       = false
        fragmentContainer.isVisible = true

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)            // 뒤로가기 시 홈으로
            .commit()
    }

    override fun onBackPressed() {
        // 프래그먼트 스택이 남아 있으면 팝, 없으면 기본 동작
        if (supportFragmentManager.backStackEntryCount > 0) {
            super.onBackPressed()
            if (supportFragmentManager.backStackEntryCount == 0) {
                bottomNav.selectedItemId = R.id.bottom_nav
            }
        } else {
            super.onBackPressed()
        }
    }
}
