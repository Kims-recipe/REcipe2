package com.kims.recipe2.ui.fridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.kims.recipe2.R
import com.kims.recipe2.databinding.FragmentFridgeBinding
import android.content.Intent
import androidx.core.view.isVisible
import com.kims.recipe2.ui.fridge.IngredientListActivity // 새로 만들 액티비티

class FridgeFragment : Fragment() {

    private var _binding: FragmentFridgeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FridgeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFridgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSectionClickListeners()
        setupClickListeners()

        val categoryAdapter = FridgeCategoryAdapter { category ->
//            showAddIngredientDialog(category.name)
            navigateToIngredientList("category", category.name)
        }

        binding.rvFridgeCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
        viewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.submitList(it)
        }

        val ingredientAdapter = IngredientAdapter(
            onItemClick = { ingredient ->
                // 재료 아이템 클릭 시 동작 (예: 상세 정보 보기)
                // 현재 코드에는 onItemClick 로직이 없으므로 비워둡니다.
            },
            onDeleteClick = { ingredient ->
                // 재료 삭제 버튼 클릭 시 ViewModel의 함수 호출
                viewModel.deleteIngredient(ingredient)
            }
        )
        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientAdapter
        }
        // ▼▼▼ [수정] viewModel.ingredients 대신 viewModel.ingredientUIState 관찰 ▼▼▼
        viewModel.ingredientUIState.observe(viewLifecycleOwner) { state ->
            // 어댑터에 표시할 목록 제출
            ingredientAdapter.submitList(state.displayList)

            // 버튼 표시 여부 설정
            binding.btnExpandIngredients.isVisible = !state.isExpanded && state.shouldShowButton
            binding.btnCollapseIngredients.isVisible = state.isExpanded && state.shouldShowButton

            // 재료가 없을 때 텍스트 표시
            val isFullListEmpty = viewModel.ingredients.value.isNullOrEmpty()
            binding.tvNoIngredients.isVisible = isFullListEmpty
            binding.rvIngredients.isVisible = !isFullListEmpty
        }
        // ▲▲▲ [수정] 여기까지 ▲▲▲
    }

    // ▼▼▼ [추가] 더보기/접기 버튼 클릭 리스너 설정 함수 ▼▼▼
    private fun setupClickListeners() {
        binding.btnExpandIngredients.setOnClickListener { viewModel.toggleIngredientsExpansion() }
        binding.btnCollapseIngredients.setOnClickListener { viewModel.toggleIngredientsExpansion() }
    }
    // ▲▲▲ [추가] 여기까지 ▲▲▲

    private fun setupSectionClickListeners() {
        // 각 구역 클릭 시, 위치 정보로 필터링하여 목록 화면으로 이동
        binding.sectionFreezer.setOnClickListener { navigateToIngredientList("location", "냉동실") }
        binding.sectionMain.setOnClickListener { navigateToIngredientList("location", "냉장실") }
        binding.sectionVegetable.setOnClickListener { navigateToIngredientList("location", "야채실") }
        binding.sectionDoor.setOnClickListener { navigateToIngredientList("location", "문짝") }
    }

    // IngredientListActivity를 여는 함수
    private fun navigateToIngredientList(filterType: String, filterValue: String) {
        val intent = Intent(requireActivity(), IngredientListActivity::class.java).apply {
            putExtra("FILTER_TYPE", filterType)
            putExtra("FILTER_VALUE", filterValue)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}