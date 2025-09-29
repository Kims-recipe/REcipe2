package com.kims.recipe2.ui.fridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kims.recipe2.R
import com.kims.recipe2.databinding.DialogConsumeQuantityBinding // 새로 추가
import com.kims.recipe2.databinding.FragmentHomemadeMealBinding
import com.kims.recipe2.model.Ingredient

class HomemadeMealFragment : Fragment() {

    private var _binding: FragmentHomemadeMealBinding? = null
    private val binding get() = _binding!!

    private val fridgeViewModel: FridgeViewModel by viewModels()
    private val mealViewModel: MealViewModel by viewModels()

    // Map을 사용하여 재료와 '소비할 양'을 관리합니다. Key: 재료 ID, Value: 소비할 Ingredient 객체
    private val selectedIngredientsMap = mutableMapOf<String, Ingredient>()
    private lateinit var selectedIngredientAdapter: IngredientAdapter

    private var selectedMealTime: String = "아침"
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.ivMealPreview.setImageURI(selectedImageUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomemadeMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinnersAndPickers()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupSpinnersAndPickers() {
        val mealTimes = listOf("아침", "점심", "저녁")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mealTimes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMealTime.adapter = adapter
        binding.spinnerMealTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedMealTime = mealTimes[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRecyclerViews() {
        // ▼▼▼ [수정] 아이템 클릭 시 수량 조절 다이얼로그 호출 ▼▼▼
        selectedIngredientAdapter = IngredientAdapter { ingredient ->
            // 어댑터에서 넘어온 ingredient는 '소비할 양'이 담긴 객체입니다.
            // 원본 재료 정보를 찾기 위해 fridgeViewModel을 사용합니다.
            val originalIngredient = fridgeViewModel.ingredients.value?.find { it.id == ingredient.id }
            if (originalIngredient != null) {
                showConsumeQuantityDialog(originalIngredient, ingredient)
            }
        }
        binding.selectedIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedIngredientAdapter
        }
        // ▲▲▲ [수정] 여기까지 ▲▲▲

        val categoryAdapter = FridgeCategoryAdapter { category ->
            val ingredients = fridgeViewModel.ingredients.value?.filter { it.category == category.name } ?: emptyList()
            showIngredientSelectionDialog(ingredients)
        }
        binding.rvFridgeCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
        fridgeViewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.submitList(it)
        }
    }

    // ▼▼▼ [추가] 수량 조절 다이얼로그를 보여주는 함수 ▼▼▼
    private fun showConsumeQuantityDialog(originalIngredient: Ingredient, currentConsumed: Ingredient) {
        val dialogBinding = DialogConsumeQuantityBinding.inflate(LayoutInflater.from(requireContext()))
        val isByAmount = originalIngredient.amount > 0 // g 단위 재료인지, 개수 단위 재료인지 확인

        // 다이얼로그 제목과 힌트 설정
        val title: String
        val currentAmount: Number = if (isByAmount) originalIngredient.amount else originalIngredient.quantity
        val unit = if (isByAmount) "g" else "개"
        title = "${originalIngredient.name} (남은 양: $currentAmount$unit)"
        dialogBinding.tvIngredientNameTitle.text = title
        dialogBinding.etConsumeQuantity.hint = "사용할 양 ($unit)을 입력하세요"

        // 현재 소비량으로 EditText 초기값 설정
        val currentConsumedValue = if (isByAmount) currentConsumed.amount else currentConsumed.quantity
        dialogBinding.etConsumeQuantity.setText(currentConsumedValue.toString())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("재료 사용량 조절")
            .setView(dialogBinding.root)
            .setPositiveButton("확인") { dialog, _ ->
                val inputText = dialogBinding.etConsumeQuantity.text.toString()
                val consumeValue = inputText.toIntOrNull()

                if (consumeValue == null || consumeValue <= 0) {
                    Toast.makeText(requireContext(), "올바른 값을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // 남은 양보다 많이 사용할 수 없도록 체크
                if ((isByAmount && consumeValue > originalIngredient.amount) || (!isByAmount && consumeValue > originalIngredient.quantity)) {
                    Toast.makeText(requireContext(), "재고보다 많은 양을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // selectedIngredientsMap에 있는 재료 정보 업데이트
                val updatedIngredient = if (isByAmount) {
                    currentConsumed.copy(amount = consumeValue, quantity = 0)
                } else {
                    currentConsumed.copy(quantity = consumeValue, amount = 0)
                }
                selectedIngredientsMap[originalIngredient.id] = updatedIngredient
                updateSelectedIngredientsList() // 리스트 및 어댑터 갱신
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .setNeutralButton("목록에서 제거") { _, _ -> // '제거' 버튼 추가
                selectedIngredientsMap.remove(originalIngredient.id)
                updateSelectedIngredientsList()
            }
            .show()
    }


    private fun setupClickListeners() {
        binding.ivMealPreview.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }
        binding.btnSaveMeal.setOnClickListener { saveMealRecord() }
        binding.btnLoadRecipe.setOnClickListener { showRecipeSelectionDialog() }
    }

    private fun observeViewModel() {
        mealViewModel.fetchUserRecipes()
    }

    private fun showRecipeSelectionDialog() {
        val recipes = mealViewModel.userRecipes.value ?: emptyList()
        if (recipes.isEmpty()) {
            Toast.makeText(requireContext(), "저장된 레시피가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeNames = recipes.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("레시피 선택")
            .setItems(recipeNames) { dialog, which ->
                val selectedRecipe = recipes[which]
                binding.etMealName.setText(selectedRecipe.name)
                loadIngredientsFromRecipe(selectedRecipe.ingredients)
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun loadIngredientsFromRecipe(recipeIngredients: List<Map<String, Any>>) {
        val currentFridgeIngredients = fridgeViewModel.ingredients.value ?: emptyList()
        selectedIngredientsMap.clear()

        recipeIngredients.forEach { recipeIngredient ->
            val name = recipeIngredient["name"] as? String ?: ""
            val fridgeIngredient = currentFridgeIngredients.find { it.name == name }
            if (fridgeIngredient != null) {
                // 레시피의 재료는 기본적으로 전체 수량을 사용하는 것으로 간주
                selectedIngredientsMap[fridgeIngredient.id] = fridgeIngredient
            } else {
                Toast.makeText(requireContext(), "'$name' 재료가 냉장고에 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        updateSelectedIngredientsList()
    }

    private fun saveMealRecord() {
        val mealName = binding.etMealName.text.toString().trim()
        val ingredientsToConsume = selectedIngredientsMap.values.toList() // Map의 value들로 리스트 생성

        if (mealName.isEmpty() || ingredientsToConsume.isEmpty()) {
            Toast.makeText(requireContext(), "식사 이름과 재료를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (binding.switchSaveRecipe.isChecked) {
            mealViewModel.saveUserRecipe(mealName, ingredientsToConsume)
        }

        mealViewModel.saveMealRecord(
            mealName, selectedMealTime, ingredientsToConsume,
            selectedImageUri?.toString(), true,
            onSuccess = {
                Toast.makeText(requireContext(), "✅ 식사 기록 완료!", Toast.LENGTH_SHORT).show()
                // ▼▼▼ [수정] 사용자가 입력한 만큼만 재료 소비 ▼▼▼
                ingredientsToConsume.forEach { consumed ->
                    val original = fridgeViewModel.ingredients.value?.find { it.id == consumed.id }
                    if(original != null) {
                        val consumedAmount = if (consumed.amount > 0) consumed.amount else consumed.quantity
                        fridgeViewModel.consumeIngredient(original, consumedAmount)
                    }
                }
                selectedIngredientsMap.clear()
                updateSelectedIngredientsList()
                binding.etMealName.text.clear()
                binding.switchSaveRecipe.isChecked = false

                val resultIntent = Intent().apply {
                    putExtra("NAVIGATE_TO_CALENDAR", true)
                }
                requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                requireActivity().finish() 
            },
            onFailure = { e -> Toast.makeText(requireContext(), "❌ 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun showIngredientSelectionDialog(ingredients: List<Ingredient>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ingredient_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rv_dialog_ingredients)
        val dialogSelectableAdapter = SelectableIngredientAdapter {}
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dialogSelectableAdapter
        }
        dialogSelectableAdapter.submitList(ingredients)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("재료 선택")
            .setView(dialogView)
            .setPositiveButton("선택") { dialog, _ ->
                val newlySelected = dialogSelectableAdapter.getSelectedItems()
                // ▼▼▼ [수정] Map을 사용하여 재료 관리 ▼▼▼
                newlySelected.forEach {
                    // 새로 추가되는 재료는 기본적으로 전체 수량을 소비하는 것으로 설정
                    if (!selectedIngredientsMap.containsKey(it.id)) {
                        selectedIngredientsMap[it.id] = it
                    }
                }
                updateSelectedIngredientsList() // 리스트 및 어댑터 갱신
                // ▲▲▲ [수정] 여기까지 ▲▲▲
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateSelectedIngredientsList() {
        val ingredientList = selectedIngredientsMap.values.toList().sortedBy { it.name }
        selectedIngredientAdapter.submitList(ingredientList)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}