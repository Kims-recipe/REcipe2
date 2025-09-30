package com.kims.recipe2.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kims.recipe2.R
import com.kims.recipe2.databinding.FragmentHomeBinding
import com.kims.recipe2.databinding.ItemMealRecordHomeBinding
import com.kims.recipe2.model.MealRecord
import com.kims.recipe2.model.NutritionItem
import com.kims.recipe2.ui.fridge.IngredientAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var nutritionAdapter: NutritionAdapter
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var priorityIngredientAdapter: IngredientAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var updateTextRunnable: Runnable? = null
    private var phraseIndex = 0
    private var currentNutritionItems: List<NutritionItem> = emptyList()

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

    private fun startTextAnimation() {
        val initialPhrases = generateDynamicPhrases()
        if (initialPhrases.isNotEmpty()) {
            binding.tvNutritionSummary.text = initialPhrases.first()
        }

        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                // ▼▼▼ [수정] 애니메이션 종료 시점에도 binding이 null인지 확인하는 안전장치 추가 ▼▼▼
                if (_binding == null) return
                // ▲▲▲ [수정] 여기까지 ▲▲▲

                val dynamicPhrases = generateDynamicPhrases()
                if (dynamicPhrases.isNotEmpty()) {
                    phraseIndex = (phraseIndex + 1) % dynamicPhrases.size
                    binding.tvNutritionSummary.text = dynamicPhrases[phraseIndex]
                }
                binding.tvNutritionSummary.startAnimation(fadeIn)
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })

        updateTextRunnable = object : Runnable {
            override fun run() {
                // ▼▼▼ [수정] Runnable 실행 시점에도 binding이 null인지 확인하는 안전장치 추가 ▼▼▼
                if (_binding == null) return
                // ▲▲▲ [수정] 여기까지 ▲▲▲

                binding.tvNutritionSummary.startAnimation(fadeOut)
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(updateTextRunnable!!, 5000)
    }

    private fun stopTextAnimation() {
        updateTextRunnable?.let { handler.removeCallbacks(it) }
        updateTextRunnable = null
    }

    private fun setupClickListeners() {
        binding.btnExpandNutrition.setOnClickListener { viewModel.toggleNutritionExpansion() }
        binding.btnCollapseNutrition.setOnClickListener { viewModel.toggleNutritionExpansion() }
    }

    private fun observeViewModel() {
        viewModel.todayDate.observe(viewLifecycleOwner) { date ->
            binding.tvDate.text = date
        }

        var isAnimationStarted = false
        // ▼▼▼ 1. 기존 영양소 관련 observer들을 아래 코드로 대체 ▼▼▼
        viewModel.nutritionUIState.observe(viewLifecycleOwner) { state ->
            currentNutritionItems = state.displayList

            if (!isAnimationStarted && state.displayList.isNotEmpty()) {
                startTextAnimation()
                isAnimationStarted = true
            }

            if (_binding != null) {
                nutritionAdapter.submitList(state.displayList)
                binding.btnExpandNutrition.isVisible = !state.isExpanded && state.shouldShowButtons
                binding.btnCollapseNutrition.isVisible = state.isExpanded && state.shouldShowButtons
            }

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

    private fun generateDynamicPhrases(): List<String> {
        val phrases = mutableListOf<String>()

        // 기본 문구
        phrases.add("오늘도 건강한 당신을 응원합니다. 💪")

        // 칼로리 확인
        currentNutritionItems.find { it.name == "칼로리" }?.let {
            if (it.current < it.goal * 0.75 && it.current > it.goal * 1.75 ) {
                phrases.add("목표 칼로리를 달성했어요! 🎉")
            }
        }

        // 단백질 확인
        currentNutritionItems.find { it.name == "단백질" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("단백질을 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("단백질 섭취가 부족해요!" )
            }
        }

        currentNutritionItems.find { it.name == "탄수화물" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("탄수화물을 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("탄수화물 섭취가 부족해요. 밥과 빵을 먹어볼까요?" )
            }
        }

        currentNutritionItems.find { it.name == "지방" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("지방을 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("지방 섭취가 부족해요. 기름진 음식을 먹어도 좋아요!" )
            }
        }

        currentNutritionItems.find { it.name == "칼슘" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("칼슘을 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("칼슘 섭취가 부족해요. 채소 위주로 먹어볼까요?" )
            }
        }
        currentNutritionItems.find { it.name == "철분" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("철분을 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("철분 섭취가 부족해요." )
            }
        }
        currentNutritionItems.find { it.name == "비타민A" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("비타민A를 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("비타민A 섭취가 부족해요." )
            }
        }
        currentNutritionItems.find { it.name == "비타민C" }?.let {
            if (it.current > it.goal * 0.75 && it.current < it.goal * 1.75 ) {
                phrases.add("비타민C을 충분히 섭취했네요! 멋져요. 👍")
            }
            else {
                phrases.add("비타민C 섭취가 부족해요" )
            }
        }

        currentNutritionItems.find { it.name == "나트륨" }?.let {
            if (it.current > it.goal) {
                phrases.add("나트륨 섭취가 조금 많아요. 물을 충분히 마셔주세요! 💧")
            }
        }
        return phrases
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

    override fun onPause() {
        super.onPause()
        stopTextAnimation() // [추가] 화면이 보이지 않게 될 때 애니메이션 중지
    }

    override fun onResume() {
        super.onResume()
        if (currentNutritionItems.isNotEmpty()) {
            startTextAnimation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTextAnimation() // [수정] onDestroyView에서도 호출하여 이중으로 보호
        _binding = null
    }
}