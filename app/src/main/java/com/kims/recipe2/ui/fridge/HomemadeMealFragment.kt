package com.kims.recipe2.ui.fridge

import android.R
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kims.recipe2.databinding.FragmentHomemadeMealBinding
import com.kims.recipe2.model.FridgeCategory
import com.kims.recipe2.model.Ingredient

class HomemadeMealFragment : Fragment() {

    private var _binding: FragmentHomemadeMealBinding? = null
    private val binding get() = _binding!!

    private val fridgeViewModel: FridgeViewModel by viewModels()
    private val selectedIngredients = mutableListOf<Ingredient>()

    private lateinit var categoryAdapter: FridgeCategoryAdapter
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
        setupMealTimeSpinner()
        setupImagePicker()

        categoryAdapter = FridgeCategoryAdapter { category ->
            val ingredients = fridgeViewModel.ingredients.value?.filter { it.category == category.name } ?: emptyList()
            showIngredientSelectionDialog(ingredients)
        }

        selectedIngredientAdapter = IngredientAdapter(onItemClick = { ingredient ->
            showRemoveDialog(ingredient)
        })

        binding.rvFridgeCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        binding.selectedIngredients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedIngredientAdapter
        }

        fridgeViewModel.categories.observe(viewLifecycleOwner) {
            categoryAdapter.submitList(it)
        }

        binding.btnSaveMeal.setOnClickListener {
            saveMealRecord()
        }
    }

    private fun setupMealTimeSpinner() {
        val mealTimes = listOf("아침", "점심", "저녁")
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, mealTimes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMealTime.adapter = adapter

        binding.spinnerMealTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedMealTime = mealTimes[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때는 기본값 유지
            }
        }
    }

    private fun setupImagePicker() {
        binding.ivMealPreview.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }
    }

    private fun showIngredientSelectionDialog(ingredients: List<Ingredient>) {
        val ingredientNames = ingredients.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("재료 선택")
            .setItems(ingredientNames) { _, which ->
                val selectedIngredient = ingredients[which]
                showQuantityPickerDialog(selectedIngredient)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showQuantityPickerDialog(ingredient: Ingredient) {
        val picker = NumberPicker(requireContext()).apply {
            minValue = 1
            maxValue = ingredient.quantity
            value = 1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("사용할 개수 선택: ${ingredient.name}")
            .setView(picker)
            .setPositiveButton("확인") { _, _ ->
                val selected = ingredient.copy(quantity = picker.value)
                selectedIngredients.removeAll { it.name == selected.name }
                selectedIngredients.add(selected)
                selectedIngredientAdapter.submitList(selectedIngredients.toList())
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

    private fun saveMealRecord() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val mealName = binding.etMealName.text.toString()
        val mealRecord = hashMapOf(
            "mealType" to "집밥",
            "mealTime" to selectedMealTime,
            "mealName" to mealName,
            "timestamp" to System.currentTimeMillis(),
            "ingredients" to selectedIngredients.map {
                mapOf(
                    "name" to it.name,
                    "category" to it.category,
                    "quantity" to it.quantity,
                    "unit" to it.unit
                )
            },
            "imageUri" to selectedImageUri?.toString().orEmpty()
        )

        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("mealRecords")
            .add(mealRecord)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "식사 기록 완료!", Toast.LENGTH_SHORT).show()
                selectedIngredients.clear()
                selectedIngredientAdapter.submitList(emptyList())
                binding.etMealName.setText("")
                binding.ivMealPreview.setImageResource(android.R.color.transparent)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "기록 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
