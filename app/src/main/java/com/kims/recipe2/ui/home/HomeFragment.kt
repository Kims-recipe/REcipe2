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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // HomeViewModel 인스턴스 생성
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

        // 1. 각 RecyclerView에 사용할 어댑터 인스턴스 생성
        val nutritionAdapter = NutritionAdapter()
        val deficientAdapter = NutritionAdapter()
        val foodAdapter = FoodAdapter { clickedFood ->
            // 음식 아이템 클릭 시 실행될 코드
            Toast.makeText(context, "${clickedFood.name}을(를) 선택했습니다.", Toast.LENGTH_SHORT).show()
        }

        // 2. RecyclerView 설정
        setupRecyclerViews(nutritionAdapter, deficientAdapter, foodAdapter)

        // 3. ViewModel의 LiveData 관찰 시작
        observeViewModel(nutritionAdapter, deficientAdapter, foodAdapter)
    }

    /**
     * 3개의 RecyclerView에 각각 LayoutManager와 Adapter를 설정합니다.
     */
    private fun setupRecyclerViews(
        nutritionAdapter: NutritionAdapter,
        deficientAdapter: NutritionAdapter,
        foodAdapter: FoodAdapter
    ) {
        binding.rvNutritionStatus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = nutritionAdapter
        }
        binding.rvDeficientNutrition.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = deficientAdapter
        }
        binding.rvRecipes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = foodAdapter
        }
    }

    /**
     * ViewModel의 LiveData들을 관찰하고, 데이터 변경 시 UI를 업데이트합니다.
     */
    private fun observeViewModel(
        nutritionAdapter: NutritionAdapter,
        deficientAdapter: NutritionAdapter,
        foodAdapter: FoodAdapter
    ) {
        // 날짜 LiveData 관찰
        viewModel.todayDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        // 영양 섭취 현황 LiveData 관찰
        viewModel.nutritionList.observe(viewLifecycleOwner) { list ->
            nutritionAdapter.submitList(list)
        }

        // 부족한 영양소 LiveData 관찰
        viewModel.deficientList.observe(viewLifecycleOwner) { list ->
            deficientAdapter.submitList(list)
        }

        // 음식 목록 로딩 상태 LiveData 관찰
        viewModel.isFoodsLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.recipesProgressBar.visibility = View.VISIBLE
                binding.rvRecipes.visibility = View.GONE
            } else {
                binding.recipesProgressBar.visibility = View.GONE
                binding.rvRecipes.visibility = View.VISIBLE
            }
        }

        // 음식 목록 LiveData 관찰
        viewModel.foods.observe(viewLifecycleOwner) { list ->
            foodAdapter.submitList(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}