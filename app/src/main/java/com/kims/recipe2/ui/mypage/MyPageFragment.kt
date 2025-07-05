package com.kims.recipe2.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kims.recipe2.databinding.FragmentMypageBinding
import com.kims.recipe2.ui.home.NutritionAdapter

class MyPageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    // 1. ViewModel 선언
    private val viewModel: MyPageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. 리사이클러뷰와 어댑터를 설정하는 부분
        // NutritionAdapter는 HomeFragment에서 사용한 것을 재활용합니다.
        val progressAdapter = NutritionAdapter()
        binding.rvWeeklyGoals.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = progressAdapter
        }

        // 3. ViewModel의 getWeeklyProgress 함수를 호출하여 데이터를 가져옵니다.
        val weeklyProgressList = viewModel.getWeeklyProgress()

        // 4. 가져온 데이터를 어댑터에 전달하여 화면에 표시합니다.
        progressAdapter.submitList(weeklyProgressList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}