package com.kims.recipe2.model

/**
 * 마이페이지 상단의 통계 카드를 위한 데이터 클래스
 * @param icon 아이콘 (이모지 등)
 * @param label 제목 (예: "이번 주 최다")
 * @param value 값 (예: "김치찌개")
 */
data class MyPageStat(
    val icon: String = "",
    val label: String = "",
    val value: String = ""
)