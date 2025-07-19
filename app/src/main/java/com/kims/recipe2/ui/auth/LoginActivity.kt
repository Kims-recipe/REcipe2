package com.kims.recipe2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kims.recipe2.MainActivity
import com.kims.recipe2.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 버튼 클릭 리스너
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // ViewModel의 로그인 상태 관찰
        viewModel.loginStatus.observe(this) { status ->
            when (status) {
                is LoginStatus.Loading -> {
                    binding.loginProgressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is LoginStatus.Success -> {
                    binding.loginProgressBar.visibility = View.GONE
                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is LoginStatus.Failure -> {
                    binding.loginProgressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            // 로그인 후 메인으로 가면, 뒤로가기 시 로그인 화면이 다시 나오지 않도록 스택을 비웁니다.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}