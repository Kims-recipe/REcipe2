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
import com.kims.recipe2.databinding.FragmentHomemadeMealBinding
import com.kims.recipe2.model.Ingredient

class HomemadeMealFragment : Fragment() {

    private var _binding: FragmentHomemadeMealBinding? = null
    private val binding get() = _binding!!

    private val fridgeViewModel: FridgeViewModel by viewModels()
    private val mealViewModel: MealViewModel by viewModels()

    private val selectedIngredients = mutableListOf<Ingredient>()
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
        selectedIngredientAdapter = IngredientAdapter { ingredient -> showRemoveDialog(ingredient) }
        binding.selectedIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedIngredientAdapter
        }

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
        selectedIngredients.clear()

        recipeIngredients.forEach { recipeIngredient ->
            val name = recipeIngredient["name"] as? String ?: ""
            val fridgeIngredient = currentFridgeIngredients.find { it.name == name }
            if (fridgeIngredient != null) {
                selectedIngredients.add(fridgeIngredient)
            } else {
                Toast.makeText(requireContext(), "'$name' 재료가 냉장고에 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        selectedIngredientAdapter.submitList(selectedIngredients.toList())
    }

    private fun saveMealRecord() {
        val mealName = binding.etMealName.text.toString().trim()
        if (mealName.isEmpty() || selectedIngredients.isEmpty()) {
            Toast.makeText(requireContext(), "식사 이름과 재료를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (binding.switchSaveRecipe.isChecked) {
            mealViewModel.saveUserRecipe(mealName, selectedIngredients.toList())
        }

        mealViewModel.saveMealRecord(
            mealName, selectedMealTime, selectedIngredients.toList(),
            selectedImageUri?.toString(), true,
            onSuccess = {
                Toast.makeText(requireContext(), "✅ 식사 기록 완료!", Toast.LENGTH_SHORT).show()
                selectedIngredients.forEach { fridgeViewModel.consumeIngredient(it, it.quantity) }
                selectedIngredients.clear()
                selectedIngredientAdapter.submitList(emptyList())
                binding.etMealName.text.clear()
                binding.switchSaveRecipe.isChecked = false
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
                newlySelected.forEach { selected ->
                    selectedIngredients.removeAll { it.name == selected.name }
                    selectedIngredients.add(selected)
                }
                selectedIngredientAdapter.submitList(selectedIngredients.toList())
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showRemoveDialog(ingredient: Ingredient) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("재료 제거")
            .setMessage("${ingredient.name} 을(를) 선택 목록에서 제거할까요?")
            .setPositiveButton("제거") { _, _ ->
                selectedIngredients.remove(ingredient)
                selectedIngredientAdapter.submitList(selectedIngredients.toList())
            }
            .setNegativeButton("취소", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}