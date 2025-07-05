package com.kims.recipe2.ui.fridge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kims.recipe2.model.FridgeCategory

class FridgeViewModel : ViewModel() {

    private val _categories = MutableLiveData<List<FridgeCategory>>()
    val categories: LiveData<List<FridgeCategory>> = _categories

    // Firestore에서 가져올 재료 목록 (데모 데이터)
    val fridgeContents = mapOf(
        "freezer" to "냉동 만두, 아이스크림, 냉동 새우, 냉동 블루베리",
        "main" to "우유, 요거트, 치즈, 햄, 김치",
        "vegetable" to "양파, 당근, 브로콜리, 상추, 토마토",
        "door" to "케첩, 마요네즈, 간장, 올리브오일, 버터"
    )

    init {
        loadCategories()
    }

    private fun loadCategories() {
        // 실제 앱에서는 Firestore에서 이 목록을 로드할 수 있습니다.
        _categories.value = listOf(
            FridgeCategory("육류", "소고기, 돼지고기, 닭고기", "🥩", "#ff6b6b"),
            FridgeCategory("채소", "양파, 당근, 브로콜리", "🥕", "#4ecdc4"),
            FridgeCategory("과일", "사과, 바나나, 키위", "🍎", "#45b7d1"),
            FridgeCategory("해산물", "연어, 새우, 오징어", "🐟", "#f9ca24"),
            FridgeCategory("기타", "우유, 달걀, 치즈", "🥛", "#ff9ff3")
        )
    }
}