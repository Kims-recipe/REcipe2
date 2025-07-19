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

        // 1. 각 구역 클릭 리스너 설정 (로직 변경)
        setupSectionClickListeners()

        // --- 기존 코드 (카테고리 목록, 재료 목록 RecyclerView 설정) ---
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

        val ingredientAdapter = IngredientAdapter()
        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ingredientAdapter
        }
        viewModel.ingredients.observe(viewLifecycleOwner) { ingredients ->
            ingredientAdapter.submitList(ingredients)
        }
        // --- 여기까지 ---
    }

    /**
     * 냉동실, 냉장실 등 각 구역을 클릭했을 때의 동작을 정의합니다.
     * 이제 재료 추가 다이얼로그를 호출합니다.
     */
    private fun setupSectionClickListeners() {
        // 각 구역 클릭 시, 위치 정보로 필터링하여 목록 화면으로 이동
        binding.sectionFreezer.setOnClickListener { navigateToIngredientList("location", "냉동실") }
        binding.sectionMain.setOnClickListener { navigateToIngredientList("location", "냉장실") }
        binding.sectionVegetable.setOnClickListener { navigateToIngredientList("location", "야채실") }
        binding.sectionDoor.setOnClickListener { navigateToIngredientList("location", "문짝") }
    }

    /**
     * 재료 추가 다이얼로그를 보여주는 함수입니다.
     * @param preselectedCategory 다이얼로그가 열릴 때 미리 선택될 카테고리 이름
     */
//    private fun showAddIngredientDialog(preselectedCategory: String) {
//        // 1. 다이얼로그에 표시될 커스텀 레이아웃을 inflate 합니다.
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_ingredient, null)
//        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.et_ingredient_name)
//        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
//
//        // 2. ViewModel에서 전체 카테고리 목록을 가져와 Spinner에 설정합니다.
//        val categoryNames = viewModel.categories.value?.map { it.name } ?: emptyList()
//        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        categorySpinner.adapter = adapter
//
//        // 3. 미리 선택해야 할 카테고리의 위치(index)를 찾아 Spinner의 기본 선택값으로 설정합니다.
//        val categoryIndex = categoryNames.indexOf(preselectedCategory)
//        if (categoryIndex != -1) {
//            categorySpinner.setSelection(categoryIndex)
//        }
//
//        // 4. MaterialAlertDialog를 생성하여 보여줍니다.
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle("내 재료 추가")
//            .setView(dialogView)
//            .setNegativeButton("취소", null)
//            .setPositiveButton("추가") { _, _ ->
//                val name = nameEditText.text.toString().trim()
//                val selectedCategory = categorySpinner.selectedItem.toString()
//
//                if (name.isNotEmpty()) {
//                    viewModel.addIngredient(name, selectedCategory)
//                }
//            }
//            .show()
//    }
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