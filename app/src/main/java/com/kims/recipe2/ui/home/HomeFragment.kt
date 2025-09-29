package com.kims.recipe2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
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

    private lateinit var nutritionAdapter: NutritionAdapter
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var priorityIngredientAdapter: IngredientAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupAdapters() {
        nutritionAdapter = NutritionAdapter()
        foodAdapter = FoodAdapter { clickedFood ->
            Toast.makeText(context, "${clickedFood.name} 선택", Toast.LENGTH_SHORT).show()
        }
        priorityIngredientAdapter = IngredientAdapter(showDeleteButton = false)
    }

    private fun setupRecyclerViews() {
        binding.rvNutritionStatus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = nutritionAdapter
        }
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodAdapter
        }
        binding.rvPriorityIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = priorityIngredientAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnExpandNutrition.setOnClickListener { viewModel.toggleNutritionExpansion() }
        binding.btnCollapseNutrition.setOnClickListener { viewModel.toggleNutritionExpansion() }
    }

    private fun observeViewModel() {
        viewModel.todayDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        // ▼▼▼ 1. 기존 영양소 관련 observer들을 아래 코드로 대체 ▼▼▼
        viewModel.nutritionUIState.observe(viewLifecycleOwner) { state ->
            // 어댑터에 목록 제출
            nutritionAdapter.submitList(state.displayList)

            // 버튼 표시 여부 설정
            binding.btnExpandNutrition.isVisible = !state.isExpanded && state.shouldShowButtons
            binding.btnCollapseNutrition.isVisible = state.isExpanded && state.shouldShowButtons
        }
        // ▲▲▲ 여기까지 대체 ▲▲▲

        viewModel.isFoodsLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.recipesProgressBar.isVisible = isLoading
            binding.rvRecipes.isVisible = !isLoading
        }
        viewModel.foods.observe(viewLifecycleOwner) { list ->
            foodAdapter.submitList(list)
        }

        viewModel.priorityIngredients.observe(viewLifecycleOwner) { ingredients ->
            val hasIngredients = ingredients.isNotEmpty()
            binding.rvPriorityIngredients.isVisible = hasIngredients
            binding.tvNoPriorityIngredients.isVisible = !hasIngredients
            priorityIngredientAdapter.submitList(ingredients)
        }

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