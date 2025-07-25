package com.kims.recipe2.ui.mypage

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
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
    private lateinit var barChart: BarChart

    private var selectedPeriod = TimePeriod.DAILY
    private var selectedChartType = ChartType.TREND
    private var selectedNutrient = "칼로리"
    private val nutrients = listOf("칼로리", "탄수화물", "단백질", "지방")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lineChart = binding.lineChart
        barChart = binding.barChart

        setupUIControls()
        setupRecyclerViews()
        setupLogoutButton()
        observeViewModel()
    }

    private fun setupUIControls() {
        // 탭 리스너
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.tvChartTitle.text = "📊 ${tab?.text} 영양소 분석"
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

        // 차트 종류 칩 리스너 (오류 수정된 부분)
        binding.chipGroupChartType.setOnCheckedStateChangeListener { _, checkedIds ->
            // checkedIds는 List<Int>이므로, 첫 번째 항목을 가져옵니다.
            val singleCheckedId = checkedIds.firstOrNull()

            selectedChartType = if (singleCheckedId == R.id.chip_trend) ChartType.TREND else ChartType.CUMULATIVE
            updateChartVisibility()
            requestDataUpdate()
        }

        // 영양소 스피너 설정 및 리스너
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nutrients)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNutrientSelect.adapter = adapter
        binding.spinnerNutrientSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedNutrient = nutrients[position]
                requestDataUpdate()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun requestDataUpdate() {
        viewModel.loadNutritionDataFor(selectedPeriod, selectedChartType, selectedNutrient)
    }

    private fun updateChartVisibility() {
        binding.lineChart.isVisible = selectedChartType == ChartType.TREND
        binding.barChart.isVisible = selectedChartType == ChartType.CUMULATIVE
    }

    private fun observeViewModel() {
        viewModel.chartDataList.observe(viewLifecycleOwner) { data ->
            val hasData = data.isNotEmpty()
            binding.tvNoData.isVisible = !hasData
            if (selectedChartType == ChartType.TREND && hasData) {
                updateLineChartData(data)
            } else if (selectedChartType == ChartType.TREND) {
                lineChart.clear()
                lineChart.invalidate()
            }
        }
        viewModel.cumulativeChartData.observe(viewLifecycleOwner) { data ->
            val hasData = data.isNotEmpty()
            binding.tvNoData.isVisible = !hasData
            if (selectedChartType == ChartType.CUMULATIVE && hasData) {
                updateBarChartData(data)
            } else if (selectedChartType == ChartType.CUMULATIVE){
                barChart.clear()
                barChart.invalidate()
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            if (stats.size >= 2) {
                ItemMypageStatBinding.bind(binding.statCard1.root).apply {
                    tvStatIcon.text = stats[0].icon; tvStatLabel.text = stats[0].label; tvStatValue.text = stats[0].value
                }
                ItemMypageStatBinding.bind(binding.statCard2.root).apply {
                    tvStatIcon.text = stats[1].icon; tvStatLabel.text = stats[1].label; tvStatValue.text = stats[1].value
                }
            }
        }
        viewModel.weeklyProgress.observe(viewLifecycleOwner) {
            (binding.rvWeeklyGoals.adapter as? NutritionAdapter)?.submitList(it)
        }
        viewModel.achievement.observe(viewLifecycleOwner) {
            binding.tvAchievementTitle.text = it.first
            binding.tvAchievementDesc.text = it.second
        }
    }

    private fun updateLineChartData(data: List<DailyNutrition>) {
        val dateFormat = SimpleDateFormat(if (selectedPeriod == TimePeriod.MONTHLY) "d" else "E", Locale.KOREA)
        val xLabels = data.map { dateFormat.format(it.date!!) }

        val entries = data.mapIndexed { index, daily ->
            Entry(index.toFloat(), viewModel.getNutrientValue(daily, selectedNutrient).toFloat())
        }
        val dataSet = createLineDataSet(entries, selectedNutrient, requireContext().getColor(R.color.protein_color))
        lineChart.data = LineData(dataSet)
        configureChartAppearance(lineChart, xLabels)
        lineChart.invalidate()
    }

    private fun updateBarChartData(data: Map<String, Double>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList(data.keys)
        labels.forEachIndexed { index, key ->
            entries.add(BarEntry(index.toFloat(), data[key]!!.toFloat()))
        }

        val dataSet = BarDataSet(entries, selectedNutrient)
        dataSet.color = requireContext().getColor(R.color.protein_color)
        barChart.data = BarData(dataSet)
        configureChartAppearance(barChart, labels)
        barChart.invalidate()
    }

    // configureChartAppearance 함수 (오류 수정된 부분)
    private fun configureChartAppearance(chart: com.github.mikephil.charting.charts.Chart<*>, xLabels: List<String>) {
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.animateY(1000)

        // chart의 타입에 따라 구체적인 클래스로 형변환하여 속성에 접근합니다.
        when (chart) {
            is LineChart -> {
                chart.axisRight.isEnabled = false
                chart.xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return xLabels.getOrNull(value.toInt()) ?: ""
                        }
                    }
                    labelCount = xLabels.size.coerceAtMost(7)
                    granularity = 1f
                }
            }
            is BarChart -> {
                chart.axisRight.isEnabled = false
                chart.xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return xLabels.getOrNull(value.toInt()) ?: ""
                        }
                    }
                    labelCount = xLabels.size
                    granularity = 1f
                }
            }
        }
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