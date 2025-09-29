package com.kims.recipe2.ui.fridge

import android.app.DatePickerDialog
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
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputLayout
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.ui.fridge.IngredientListActivity // 새로 만들 액티비티
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FridgeFragment : Fragment() {

    private var _binding: FragmentFridgeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FridgeViewModel by viewModels()
    private var selectedExpirationDate: Date? = null

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
                showEditIngredientDialog(ingredient)
            },
            onDeleteClick = { ingredient ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("재료 삭제")
                    .setMessage("'${ingredient.name}'을(를) 정말 삭제하시겠습니까?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("삭제") { _, _ ->
                        viewModel.deleteIngredient(ingredient)
                    }
                    .show()
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
    // ▼▼▼ [추가] 재료 수정 다이얼로그를 보여주는 함수 ▼▼▼
    private fun showEditIngredientDialog(ingredientToEdit: Ingredient) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ingredient_detail, null)

        val nameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.et_ingredient_name)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_category)
        val locationSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_location)
        val expirationDateEditText = dialogView.findViewById<TextInputEditText>(R.id.et_expiration_date)
        val toggleInputType = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggle_input_type)
        val quantityLayout = dialogView.findViewById<LinearLayout>(R.id.ll_quantity_input)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.til_amount)
        val quantityEditText = dialogView.findViewById<TextInputEditText>(R.id.et_quantity)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.et_amount)

        nameEditText.setText(ingredientToEdit.name)
        nameEditText.isEnabled = false // 재료 이름은 수정 불가

        val categoryNames = viewModel.categories.value?.map { it.name } ?: emptyList()
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames)
        categorySpinner.setAdapter(categoryAdapter)
        categorySpinner.setText(ingredientToEdit.category, false)

        val locationNames = listOf("냉동실", "냉장실", "야채실", "문짝")
        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, locationNames)
        locationSpinner.setAdapter(locationAdapter)
        locationSpinner.setText(ingredientToEdit.location, false)

        if (ingredientToEdit.amount > 0) {
            toggleInputType.check(R.id.btn_select_amount)
            quantityLayout.visibility = View.GONE
            amountLayout.visibility = View.VISIBLE
            amountEditText.setText(ingredientToEdit.amount.toString())
            quantityEditText.setText("0")
        } else {
            toggleInputType.check(R.id.btn_select_quantity)
            quantityLayout.visibility = View.VISIBLE
            amountLayout.visibility = View.GONE
            quantityEditText.setText(ingredientToEdit.quantity.toString())
            amountEditText.setText("0")
        }

        ingredientToEdit.expirationDate?.let {
            selectedExpirationDate = it
            expirationDateEditText.setText(SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(it))
        }
        expirationDateEditText.setOnClickListener { showDatePickerDialog(expirationDateEditText) }

        toggleInputType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_select_quantity -> {
                        quantityLayout.visibility = View.VISIBLE
                        amountLayout.visibility = View.GONE
                        amountEditText.setText("0")
                    }
                    R.id.btn_select_amount -> {
                        quantityLayout.visibility = View.GONE
                        amountLayout.visibility = View.VISIBLE
                        quantityEditText.setText("0")
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("재료 정보 수정")
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                val quantity = quantityEditText.text.toString().toIntOrNull() ?: 0
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0

                val updatedIngredient = ingredientToEdit.copy(
                    category = categorySpinner.text.toString(),
                    location = locationSpinner.text.toString(),
                    quantity = quantity,
                    amount = amount.toInt(),
                    unit = if (amount > 0) "g" else "개",
                    expirationDate = selectedExpirationDate
                )
                viewModel.updateIngredient(updatedIngredient)
                selectedExpirationDate = null
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // ▼▼▼ [추가] 날짜 선택 다이얼로그를 보여주는 함수 ▼▼▼
    private fun showDatePickerDialog(dateEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        selectedExpirationDate?.let { date -> calendar.time = date }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth, 0, 0, 0)
                selectedExpirationDate = calendar.time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateEditText.setText(dateFormat.format(selectedExpirationDate!!))
            },
            year, month, day
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}