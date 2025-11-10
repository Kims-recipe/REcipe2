package com.kims.recipe2.model

import com.google.firebase.firestore.Exclude

/**
 * 사용자가 직접 만든 레시피 정보를 담는 데이터 클래스
 */
data class UserRecipe(
    @get:Exclude var id: String = "", // Firestore 문서 ID
    val name: String = "", // 레시피 이름 (예: "나만의 김치찌개")
    val ingredients: List<Map<String, Any>> = emptyList() // 레시피에 포함된 재료 목록
)