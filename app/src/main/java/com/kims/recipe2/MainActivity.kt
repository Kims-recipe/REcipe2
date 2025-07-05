package com.kims.recipe2


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.kims.recipe2.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // BottomNavigationView와 NavController 연결
        binding.bottomNavigationView.setupWithNavController(navController)

        // FAB 클릭 리스너
        binding.fab.setOnClickListener {
            // TODO: 식사 추가 다이얼로그 또는 화면 표시
            Snackbar.make(it, "새로운 식사를 기록합니다.", Snackbar.LENGTH_SHORT).show()
        }

        // FAB을 위한 더미 메뉴 클릭 방지
        binding.bottomNavigationView.menu.findItem(R.id.placeholder).isEnabled = false
    }
}
