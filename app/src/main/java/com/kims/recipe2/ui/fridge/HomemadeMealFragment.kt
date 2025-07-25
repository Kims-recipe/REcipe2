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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kims.recipe2.databinding.FragmentHomemadeMealBinding
import com.kims.recipe2.model.Ingredient

class HomemadeMealFragment : Fragment() {

    private var _binding: FragmentHomemadeMealBinding? = null
    private val binding get() = _binding!!

    private val fridgeViewModel: FridgeViewModel by viewModels()
    private lateinit var mealViewModel: MealViewModel

    private val selectedIngredients = mutableListOf<Ingredient>()
    private lateinit var allIngredientAdapter: IngredientAdapter

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewModel 인스턴스 초기화. ViewModelProvider.Factory를 사용해야 안전합니다.
        // Hilt/Koin 사용 시 @Inject 어노테이션 등으로 자동 주입됩니다.
        // 임시 방편으로, fridgeViewModel이 이미 주입되었다고 가정하고 생성자에 넘겨줍니다.
        // 실제 프로젝트에서는 ViewModelProvider.Factory 또는 DI 라이브러리를 사용하세요.
        mealViewModel = MealViewModel(fridgeViewModel)
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

        fridgeViewModel.ingredients.observe(viewLifecycleOwner) {
            // 이 Observer는 재료 데이터가 변경될 때마다 호출되므로,
            // showIngredientSelectionDialog에서 사용할 재료 목록도 최신 상태를 유지하게 됩니다.
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

    // 재료 선택 다이얼로그를 커스텀 RecyclerView로 변경
    private fun showIngredientSelectionDialog(ingredients: List<Ingredient>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(com.kims.recipe2.R.layout.dialog_ingredient_selection, null) // 새로운 레이아웃 파일 사용
        val recyclerView = dialogView.findViewById<RecyclerView>(com.kims.recipe2.R.id.rv_dialog_ingredients)

        // 다이얼로그에서 사용할 SelectableIngredientAdapter 인스턴스 생성
        val dialogSelectableAdapter = SelectableIngredientAdapter { selectedList ->
            // 이 콜백은 다이얼로그 내에서 재료 선택이 변경될 때마다 호출됩니다.
            // 여기서는 아직 최종 선택이 아니므로 아무것도 하지 않습니다.
            // 최종 선택은 "선택" 버튼을 눌렀을 때 처리합니다.
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dialogSelectableAdapter
        }
        dialogSelectableAdapter.submitList(ingredients) // 다이얼로그에 표시할 재료 목록 제출

        // MaterialAlertDialogBuilder를 사용하여 다이얼로그 생성
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("재료 선택")
            .setView(dialogView) // 커스텀 뷰 설정
            .setPositiveButton("선택") { dialog, _ ->
                // 다이얼로그의 "선택" 버튼을 눌렀을 때, 선택된 재료들을 처리
                val newlySelected = dialogSelectableAdapter.getSelectedItems() // SelectableIngredientAdapter에 이 함수를 추가해야 함
                newlySelected.forEach { selected ->
                    // 이미 추가된 재료가 있다면 제거하고 새로 추가 (수량 업데이트)
                    selectedIngredients.removeAll { it.name == selected.name }
                    selectedIngredients.add(selected)
                }
                selectedIngredientAdapter.submitList(selectedIngredients.toList()) // 하단 목록 업데이트
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
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
        val mealName = binding.etMealName.text.toString()

        if (mealName.isEmpty() || selectedIngredients.isEmpty()) {
            Toast.makeText(requireContext(), "식사 이름과 재료를 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        mealViewModel.saveMealRecord(
            mealName = mealName,
            mealType = selectedMealTime,
            selectedIngredients = selectedIngredients.toList(), // mutableList를 toList()로 넘겨 불변성 유지
            imageUri = selectedImageUri?.toString(),
            onSuccess = {
                Toast.makeText(requireContext(), "✅ 식사 기록 완료!", Toast.LENGTH_SHORT).show()
                selectedIngredients.clear()
                selectedIngredientAdapter.submitList(emptyList())
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "❌ 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
