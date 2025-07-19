package com.kims.recipe2.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _signUpStatus = MutableLiveData<SignUpStatus>()
    val signUpStatus: LiveData<SignUpStatus> = _signUpStatus

    fun signUp(email: String, password: String, confirmPassword: String) {
        // 입력값 유효성 검사
        if (password != confirmPassword) {
            _signUpStatus.value = SignUpStatus.Failure("비밀번호가 일치하지 않습니다.")
            return
        }
        if (password.length < 6) {
            _signUpStatus.value = SignUpStatus.Failure("비밀번호는 6자 이상이어야 합니다.")
            return
        }

        _signUpStatus.value = SignUpStatus.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Auth 생성 성공 시, Firestore에 사용자 정보 저장을 시도할 수 있습니다. (선택사항)
                    // 현재는 바로 성공 상태를 알립니다.
                    _signUpStatus.value = SignUpStatus.Success
                } else {
                    // 회원가입 실패
                    _signUpStatus.value = SignUpStatus.Failure(task.exception?.message ?: "회원가입에 실패했습니다.")
                }
            }
    }
}

sealed class SignUpStatus {
    object Loading : SignUpStatus()
    object Success : SignUpStatus()
    data class Failure(val message: String) : SignUpStatus()
}