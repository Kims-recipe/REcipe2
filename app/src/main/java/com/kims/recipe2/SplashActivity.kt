package com.kims.recipe2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.kims.recipe2.databinding.ActivitySplashBinding // ViewBinding import
import com.kims.recipe2.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. 레이아웃을 화면에 표시하도록 설정
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. 1.5초 후에 화면 전환 로직을 실행
        Handler(Looper.getMainLooper()).postDelayed({
            // 로그인 상태 확인
            if (FirebaseAuth.getInstance().currentUser == null) {
                // 로그인 상태가 아니면 LoginActivity로 이동
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                // 로그인 상태이면 MainActivity로 이동
                startActivity(Intent(this, MainActivity::class.java))
            }
            // SplashActivity는 확인 후 바로 종료
            finish()
        }, 1500) // 1500ms = 1.5초
    }
}