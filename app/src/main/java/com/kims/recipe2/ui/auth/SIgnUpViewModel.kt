package com.kims.recipe2.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.model.UserInfo

class SignUpViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _signUpStatus = MutableLiveData<SignUpStatus>()
    val signUpStatus: LiveData<SignUpStatus> = _signUpStatus

    fun signUp(
        email: String,
        password: String,
        confirmPassword: String,
        height: Int,
        weight: Int,
        gender: String
    ) {
        // 입력값 유효성 검사
        if (password != confirmPassword) {
            _signUpStatus.value = SignUpStatus.Failure("비밀번호가 일치하지 않습니다.")
            return
        }
        if (password.length < 6) {
            _signUpStatus.value = SignUpStatus.Failure("비밀번호는 6자 이상이어야 합니다.")
            return
        }
        if (height <= 0 || weight <= 0) {
            _signUpStatus.value = SignUpStatus.Failure("키와 체중을 올바르게 입력해주세요.")
            return
        }

        _signUpStatus.value = SignUpStatus.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId == null) {
                        _signUpStatus.value = SignUpStatus.Failure("사용자 정보를 가져올 수 없습니다.")
                        return@addOnCompleteListener
                    }
                    // 목표 영양소 계산
                    val userInfo = calculateGoals(height, weight, gender)

                    // Firestore에 사용자 정보 저장
                    db.collection("users").document(userId)
                        .set(mapOf("email" to email)) // users 문서에 기본 정보 저장
                        .addOnSuccessListener {
                            // users/{userId}/userInfo/profile 경로에 상세 정보 저장
                            db.collection("users").document(userId)
                                .collection("userInfo").document("profile")
                                .set(userInfo)
                                .addOnSuccessListener { _signUpStatus.value = SignUpStatus.Success }
                                .addOnFailureListener { e -> _signUpStatus.value = SignUpStatus.Failure(e.message ?: "사용자 정보 저장에 실패했습니다.") }
                        }
                        .addOnFailureListener { e -> _signUpStatus.value = SignUpStatus.Failure(e.message ?: "사용자 정보 저장에 실패했습니다.") }
                } else {
                    _signUpStatus.value = SignUpStatus.Failure(task.exception?.message ?: "회원가입에 실패했습니다.")
                }
            }
    }

    private fun calculateGoals(height: Int, weight: Int, gender: String): UserInfo {
        // 해리스-베네딕트 공식 (활동량 보통으로 가정 = * 1.55)
        val bmr = if (gender == "남성") {
            (88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * 25)) * 1.55 // 25세로 가정
        } else {
            (447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * 25)) * 1.55 // 25세로 가정
        }
        // 탄수화물 50%, 단백질 30%, 지방 20%
        val carbs = (bmr * 0.5) / 4
        val protein = (bmr * 0.3) / 4
        val fat = (bmr * 0.2) / 9

        return UserInfo(
            height = height,
            weight = weight,
            gender = gender,
            goalCalories = bmr,
            goalCarbs = carbs,
            goalProtein = protein,
            goalFat = fat
        )
    }
}

sealed class SignUpStatus {
    object Loading : SignUpStatus()
    object Success : SignUpStatus()
    data class Failure(val message: String) : SignUpStatus()
}