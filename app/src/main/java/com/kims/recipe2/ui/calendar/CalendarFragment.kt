package com.kims.recipe2.ui.calendar


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kims.recipe2.R
import com.kims.recipe2.databinding.CalendarDayLayoutBinding
import com.kims.recipe2.databinding.FragmentCalendarBinding
import com.google.android.material.snackbar.Snackbar
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()
    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val mealAdapter = MealRecordAdapter { meal ->
        Snackbar.make(binding.root, "${meal.name} 공유 기능 구현", Snackbar.LENGTH_SHORT).show()
    }
    private var datesWithMeals = emptySet<LocalDate>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMealRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMealRecords.adapter = mealAdapter

        viewModel.getDatesWithMeals().observe(viewLifecycleOwner) {
            datesWithMeals = it
            binding.calendarView.notifyCalendarChanged()
        }

        viewModel.selectedDateMeals.observe(viewLifecycleOwner) { meals ->
            binding.tvNoMeals.isVisible = meals.isEmpty()
            binding.rvMealRecords.isVisible = meals.isNotEmpty()
            mealAdapter.submitList(meals)
        }


        // onViewCreated 내부

        val daysOfWeek = daysOfWeek()
// XML에 이미 TextView가 있으므로, 각 TextView에 텍스트만 설정합니다.
        binding.legendLayout.children.forEachIndexed { index, view ->
            (view as? TextView)?.text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        binding.calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        binding.calendarView.scrollToMonth(currentMonth)


        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay
            val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
            val dotView: View = CalendarDayLayoutBinding.bind(view).dotView

            init {
                view.setOnClickListener {
                    if (day.position == DayPosition.MonthDate) {
                        selectDate(day.date)
                    }
                }
            }
        }

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.textView
                val dotView = container.dotView
                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.visibility = View.VISIBLE
                    when (data.date) {
                        selectedDate -> {
                            textView.setTextColor(Color.WHITE)
                            textView.setBackgroundResource(R.drawable.calendar_selected_day_bg)
                            dotView.visibility = View.INVISIBLE
                        }
                        today -> {
                            textView.setTextColor(Color.BLUE)
                            textView.background = null
                            dotView.isVisible = data.date in datesWithMeals
                        }
                        else -> {
                            textView.setTextColor(Color.BLACK)
                            textView.background = null
                            dotView.isVisible = data.date in datesWithMeals
                        }
                    }
                } else {
                    textView.visibility = View.INVISIBLE
                    dotView.visibility = View.INVISIBLE
                }
            }
        }

        binding.calendarView.monthScrollListener = { month ->
            binding.tvMonthTitle.text = monthTitleFormatter.format(month.yearMonth)
        }

        binding.btnNextMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }

        binding.btnPreviousMonth.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }

        selectDate(today) // 초기 날짜 선택
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { binding.calendarView.notifyDateChanged(it) }
            binding.calendarView.notifyDateChanged(date)

            viewModel.getMealsForDate(date)
            binding.tvSelectedDateMealsTitle.text = "🍽️ 식단 (${date.monthValue}/${date.dayOfMonth})"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}