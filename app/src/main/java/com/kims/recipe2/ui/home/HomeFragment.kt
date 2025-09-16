package com.kims.recipe2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kims.recipe2.databinding.FragmentHomeBinding
import com.kims.recipe2.databinding.ItemMealRecordHomeBinding
import com.kims.recipe2.model.MealRecord
import com.kims.recipe2.ui.fridge.IngredientAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nutritionAdapter = NutritionAdapter()
        val deficientAdapter = NutritionAdapter()
        val foodAdapter = FoodAdapter { clickedFood ->
            Toast.makeText(context, "${clickedFood.name}을(를) 선택했습니다.", Toast.LENGTH_SHORT).show()
        }
        // 👇 [추가] 우선 소비 재료를 위한 어댑터 생성
        val priorityIngredientAdapter = IngredientAdapter()

        // 👇 setupRecyclerViews 함수에 새 어댑터 전달
        setupRecyclerViews(nutritionAdapter, deficientAdapter, foodAdapter, priorityIngredientAdapter)
        // 👇 observeViewModel 함수에 새 어댑터 전달
        observeViewModel(nutritionAdapter, deficientAdapter, foodAdapter, priorityIngredientAdapter)
    }

    // 👇 setupRecyclerViews 함수 시그니처 변경
    private fun setupRecyclerViews(
        nutritionAdapter: NutritionAdapter,
        deficientAdapter: NutritionAdapter,
        foodAdapter: FoodAdapter,
        priorityIngredientAdapter: IngredientAdapter
    ) {
        binding.rvNutritionStatus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = nutritionAdapter
        }
//        binding.rvDeficientNutrition.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = deficientAdapter
//        }
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodAdapter
        }
        // 👇 [추가] 우선 소비 재료 RecyclerView 설정
        binding.rvPriorityIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = priorityIngredientAdapter
        }
    }

    // 👇 observeViewModel 함수 시그니처 변경
    private fun observeViewModel(
        nutritionAdapter: NutritionAdapter,
        deficientAdapter: NutritionAdapter,
        foodAdapter: FoodAdapter,
        priorityIngredientAdapter: IngredientAdapter
    ) {
        viewModel.todayDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        viewModel.nutritionList.observe(viewLifecycleOwner) { list ->
            nutritionAdapter.submitList(list)
        }

        viewModel.deficientList.observe(viewLifecycleOwner) { list ->
            deficientAdapter.submitList(list)
        }

        viewModel.isFoodsLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.recipesProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvRecipes.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.foods.observe(viewLifecycleOwner) { list ->
            foodAdapter.submitList(list)
        }

        // 👇 [추가] 우선 소비 재료 LiveData 관찰
        viewModel.priorityIngredients.observe(viewLifecycleOwner) { ingredients ->
            priorityIngredientAdapter.submitList(ingredients)
        }
        // 오늘의 식단 LiveData 관찰
        viewModel.todayMealRecords.observe(viewLifecycleOwner) { meals ->
            binding.llMealRecords.removeAllViews()
            if (meals.isEmpty()) {
                binding.tvNoMealRecords.visibility = View.VISIBLE
                binding.hsvMealRecords.visibility = View.GONE
            } else {
                binding.tvNoMealRecords.visibility = View.GONE
                binding.hsvMealRecords.visibility = View.VISIBLE
                meals.forEach { meal ->
                    val mealView = createMealRecordView(meal)
                    binding.llMealRecords.addView(mealView)
                }
            }
        }
    }

    // 식단 기록 뷰를 생성하는 함수
    private fun createMealRecordView(meal: MealRecord): View {
        val mealIcons = mapOf("아침" to "🍳", "점심" to "🍜", "저녁" to "🥗", "간식" to "🍰")
        val viewBinding = ItemMealRecordHomeBinding.inflate(LayoutInflater.from(context))

        viewBinding.tvMealTypeIcon.text = mealIcons[meal.type] ?: "🍴"
        viewBinding.tvMealName.text = meal.name
        viewBinding.tvMealType.text = meal.type
        viewBinding.tvCalories.text = "${meal.calories}kcal"

        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}