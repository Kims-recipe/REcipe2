package com.kims.recipe2.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.kims.recipe2.MainActivity
import com.kims.recipe2.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                viewModel.signUp(email, password, confirmPassword)
            } else {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.signUpStatus.observe(this) { status ->
            when (status) {
                is SignUpStatus.Loading -> {
                    binding.signUpProgressBar.visibility = View.VISIBLE
                    binding.btnSignUp.isEnabled = false
                }
                is SignUpStatus.Success -> {
                    binding.signUpProgressBar.visibility = View.GONE
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is SignUpStatus.Failure -> {
                    binding.signUpProgressBar.visibility = View.GONE
                    binding.btnSignUp.isEnabled = true
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToMain() {
        // 회원가입 성공 시 바로 로그인 상태가 되므로, 메인 화면으로 이동합니다.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}