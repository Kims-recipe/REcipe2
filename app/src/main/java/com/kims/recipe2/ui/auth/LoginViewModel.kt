package com.kims.recipe2.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // 로그인 상태를 UI에 알려주기 위한 LiveData
    private val _loginStatus = MutableLiveData<LoginStatus>()
    val loginStatus: LiveData<LoginStatus> = _loginStatus

    fun login(email: String, password: String) {
        // 로딩 상태 시작
        _loginStatus.value = LoginStatus.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 로그인 성공
                    _loginStatus.value = LoginStatus.Success
                } else {
                    // 로그인 실패
                    _loginStatus.value = LoginStatus.Failure(task.exception?.message ?: "로그인에 실패했습니다.")
                }
            }
    }
}

// 로그인 상태를 나타내는 클래스
sealed class LoginStatus {
    object Loading : LoginStatus()
    object Success : LoginStatus()
    data class Failure(val message: String) : LoginStatus()
}