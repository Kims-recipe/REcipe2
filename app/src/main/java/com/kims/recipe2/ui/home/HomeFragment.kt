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

        // 'deficientAdapter'가 더 이상 필요 없으므로 삭제합니다.
        val nutritionAdapter = NutritionAdapter()
        val foodAdapter = FoodAdapter { clickedFood ->
            Toast.makeText(context, "${clickedFood.name}을(를) 선택했습니다.", Toast.LENGTH_SHORT).show()
        }
        val priorityIngredientAdapter = IngredientAdapter()

        // 함수 호출 부분을 업데이트합니다.
        setupRecyclerViews(nutritionAdapter, foodAdapter, priorityIngredientAdapter)
        observeViewModel(nutritionAdapter, foodAdapter, priorityIngredientAdapter)
    }

    // 함수의 파라미터에서 'deficientAdapter'를 제거합니다.
    private fun setupRecyclerViews(
        nutritionAdapter: NutritionAdapter,
        foodAdapter: FoodAdapter,
        priorityIngredientAdapter: IngredientAdapter
    ) {
        binding.rvNutritionStatus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = nutritionAdapter
        }
        // 'rvDeficientNutrition' 관련 코드를 완전히 삭제합니다.
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodAdapter
        }
        binding.rvPriorityIngredients.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = priorityIngredientAdapter
        }
    }

    // 함수의 파라미터에서 'deficientAdapter'를 제거합니다.
    private fun observeViewModel(
        nutritionAdapter: NutritionAdapter,
        foodAdapter: FoodAdapter,
        priorityIngredientAdapter: IngredientAdapter
    ) {
        viewModel.todayDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        viewModel.nutritionList.observe(viewLifecycleOwner) { list ->
            nutritionAdapter.submitList(list)
        }

        // 'deficientList'를 관찰하는 코드를 완전히 삭제합니다.
        viewModel.isFoodsLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.recipesProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.rvRecipes.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.foods.observe(viewLifecycleOwner) { list ->
            foodAdapter.submitList(list)
        }

        viewModel.priorityIngredients.observe(viewLifecycleOwner) { ingredients ->
            val hasIngredients = ingredients.isNotEmpty()
            binding.rvPriorityIngredients.isVisible = hasIngredients
            binding.tvNoPriorityIngredients.isVisible = !hasIngredients

            if (hasIngredients) {
                priorityIngredientAdapter.submitList(ingredients)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}