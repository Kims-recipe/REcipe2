package com.kims.recipe2.ui.mypage

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter // << 이 import가 추가되었습니다.
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.kims.recipe2.R
import com.kims.recipe2.model.DailyNutrition
import com.kims.recipe2.databinding.FragmentMypageBinding
import com.kims.recipe2.databinding.ItemMypageStatBinding
import com.kims.recipe2.ui.auth.LoginActivity
import com.kims.recipe2.ui.home.NutritionAdapter
import java.text.SimpleDateFormat
import java.util.*

class MyPageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyPageViewModel by viewModels()
    private lateinit var lineChart: LineChart

    private var selectedPeriod = TimePeriod.DAILY
    private var selectedNutrient = "칼로리"
    private val nutrients = listOf("칼로리", "탄수화물", "단백질", "지방")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lineChart = binding.lineChart

        setupUIControls()
        setupRecyclerViews()
        setupLogoutButton()
        observeViewModel()
    }

    private fun setupUIControls() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedPeriod = when (tab?.position) {
                    1 -> TimePeriod.WEEKLY
                    2 -> TimePeriod.MONTHLY
                    else -> TimePeriod.DAILY
                }
                requestDataUpdate()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nutrients)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNutrientSelect.adapter = adapter
        binding.spinnerNutrientSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedNutrient = nutrients[position]
                requestDataUpdate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun requestDataUpdate() {
        viewModel.loadNutritionDataFor(selectedPeriod, selectedNutrient)
    }

    private fun observeViewModel() {
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            val hasData = data.isNotEmpty()
            binding.tvNoData.isVisible = !hasData
            lineChart.isVisible = hasData

            if (hasData) {
                updateLineChartData(data)
            } else {
                lineChart.clear()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.chartProgressBar.isVisible = isLoading
            if (isLoading) {
                lineChart.visibility = View.INVISIBLE
                binding.tvNoData.visibility = View.GONE
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats.size >= 2) {
                ItemMypageStatBinding.bind(binding.statCard1.root).apply {
                    tvStatIcon.text = stats[0].icon
                    tvStatLabel.text = stats[0].label
                    tvStatValue.text = stats[0].value
                }
                ItemMypageStatBinding.bind(binding.statCard2.root).apply {
                    tvStatIcon.text = stats[1].icon
                    tvStatLabel.text = stats[1].label
                    tvStatValue.text = stats[1].value
                }
            }
        }

        viewModel.weeklyProgress.observe(viewLifecycleOwner) { progressList ->
            (binding.rvWeeklyGoals.adapter as? NutritionAdapter)?.submitList(progressList)
        }

        viewModel.achievement.observe(viewLifecycleOwner) { achievement ->
            binding.tvAchievementTitle.text = achievement.first
            binding.tvAchievementDesc.text = achievement.second
        }
    }

    private fun updateLineChartData(data: Map<String, Float>) {
        val labels = when(selectedPeriod) {
            TimePeriod.DAILY -> data.keys.sorted()
            TimePeriod.WEEKLY -> listOf("5주전", "4주전", "3주전", "2주전", "1주전", "이번주")
            TimePeriod.MONTHLY -> listOf("5달전", "4달전", "3달전", "2달전", "1달전", "이번달")
        }

        val entries = ArrayList<Entry>()
        labels.forEachIndexed { index, label ->
            entries.add(Entry(index.toFloat(), data[label] ?: 0f))
        }

        val dataSet = createLineDataSet(entries, selectedNutrient, ContextCompat.getColor(requireContext(), R.color.protein_color))
        lineChart.data = LineData(dataSet)
        configureChartAppearance(lineChart, labels)
        lineChart.invalidate()
    }

    private fun configureChartAppearance(chart: LineChart, xLabels: List<String>) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(xLabels) // 이 부분을 위해 import가 필요했습니다.
            labelCount = xLabels.size
            granularity = 1f
        }
        chart.axisRight.isEnabled = false
        chart.animateY(1000)
    }

    private fun createLineDataSet(entries: List<Entry>, label: String, color: Int) = LineDataSet(entries, label).apply {
        this.color = color; valueTextColor = color; lineWidth = 2f; setCircleColor(color); circleRadius = 4f; setDrawCircleHole(false)
    }

    private fun setupRecyclerViews() {
        binding.rvWeeklyGoals.layoutManager = LinearLayoutManager(context)
        binding.rvWeeklyGoals.adapter = NutritionAdapter()
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}