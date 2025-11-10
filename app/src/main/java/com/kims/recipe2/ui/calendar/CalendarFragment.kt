// CalendarFragment.kt
package com.kims.recipe2.ui.calendar

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kims.recipe2.R
import com.kims.recipe2.databinding.CalendarDayLayoutBinding
import com.kims.recipe2.databinding.FragmentCalendarBinding
import com.google.android.material.snackbar.Snackbar
import com.kims.recipe2.model.MealRecord
import com.kims.recipe2.ui.fridge.MealViewModel
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class
CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarViewModel by viewModels()
    private val mealViewModel: MealViewModel by viewModels() // ✨ MealViewModel 인스턴스 추가
    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    // 사진 추가를 위한 변수
    private var currentMealForImageUpdate: MealRecord? = null
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            if (selectedImageUri != null && currentMealForImageUpdate != null) {
                updateMealImage(currentMealForImageUpdate!!, selectedImageUri)
            }
        }
    }

    private val mealAdapter = MealRecordAdapter(
        onShareClick = { meal: MealRecord ->
            shareToInstagram(meal)
        },
        onItemClick = { meal: MealRecord ->
            // ✨ 아이템 클릭 시 상세 다이얼로그 띄우기
            showMealDetailDialog(meal)
        }
    )
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

        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        viewModel.datesWithMeals.observe(viewLifecycleOwner) {
            datesWithMeals = it
            binding.calendarView.notifyCalendarChanged()
        }

        viewModel.mealRecords.observe(viewLifecycleOwner) { meals ->
            binding.tvNoMeals.isVisible = meals.isEmpty()
            binding.rvMealRecords.isVisible = meals.isNotEmpty()
            mealAdapter.submitList(meals)
        }

        setupCalendar()
        selectDate(today) // 초기 날짜 선택
    }

    private fun showMealDetailDialog(meal: MealRecord) {
        val dialogView = LayoutInflater.from(context).inflate(com.kims.recipe2.R.layout.dialog_meal_detail, null)
        val bindingDialog = com.kims.recipe2.databinding.DialogMealDetailBinding.bind(dialogView)

        // 데이터 바인딩
        if (meal.imageUri != null && meal.imageUri.isNotEmpty()) {
            bindingDialog.ivMealImageDetail.load(meal.imageUri)
            bindingDialog.btnAddPhoto.text = "📷 사진 변경"
        } else {
            // 이미지가 없으면 기본 아이콘이나 배경색을 설정
            bindingDialog.ivMealImageDetail.setImageResource(android.R.color.transparent)
            bindingDialog.ivMealImageDetail.setBackgroundColor(Color.parseColor("#E0E0E0"))
            bindingDialog.btnAddPhoto.text = "📷 사진 추가"
        }

        bindingDialog.tvMealNameDetail.text = meal.name

        // 식사 시간 표시 및 색상 설정
        bindingDialog.tvMealTime.text = meal.type
        val mealTimeColor = when (meal.type) {
            "아침" -> Color.parseColor("#FF9800") // 주황색
            "점심" -> Color.parseColor("#4CAF50") // 초록색
            "저녁" -> Color.parseColor("#2196F3") // 파란색
            "간식" -> Color.parseColor("#9C27B0") // 보라색
            else -> Color.parseColor("#757575") // 회색
        }
        bindingDialog.tvMealTime.backgroundTintList = ColorStateList.valueOf(mealTimeColor)

        // 영양소 정보 표시
        bindingDialog.tvCalories.text = "${meal.calories.toInt()} kcal"
        bindingDialog.tvCarbs.text = "${meal.carbs.toInt()} g"
        bindingDialog.tvProtein.text = "${meal.protein.toInt()} g"
        bindingDialog.tvFat.text = "${meal.fat.toInt()} g"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .show()

        // 사진 추가/변경 버튼 클릭 리스너
        bindingDialog.btnAddPhoto.setOnClickListener {
            currentMealForImageUpdate = meal
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
            dialog.dismiss()
        }

        // 삭제 버튼 클릭 리스너
        bindingDialog.btnDeleteMeal.setOnClickListener {
            mealViewModel.deleteMealRecord(
                meal,
                onSuccess = {
                    Toast.makeText(requireContext(), "✅ 식단 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // 삭제 성공 시 다이얼로그 닫기
                    viewModel.fetchMealRecordsForDate(selectedDate ?: today) // 목록 새로고침
                },
                onFailure = { e ->
                    Toast.makeText(requireContext(), "❌ 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupCalendar() {
        val daysOfWeek = daysOfWeek()
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
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { binding.calendarView.notifyDateChanged(it) }
            binding.calendarView.notifyDateChanged(date)

            viewModel.fetchMealRecordsForDate(date)
            binding.tvSelectedDateMealsTitle.text = "🍽️ 식단 (${date.monthValue}/${date.dayOfMonth})"
        }
    }

    private fun shareToInstagram(meal: MealRecord) {
        if (meal.imageUri.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "공유할 이미지가 없습니다", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageUri = Uri.parse(meal.imageUri)
                val bitmap = if (meal.imageUri.startsWith("http")) {
                    // URL에서 이미지 다운로드
                    BitmapFactory.decodeStream(URL(meal.imageUri).openStream())
                } else {
                    // 로컬 파일에서 이미지 로드
                    requireContext().contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "이미지를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 임시 파일로 저장
                val cachePath = File(requireContext().cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "share_image_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }

                val contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                withContext(Dispatchers.Main) {
                    // 인스타그램이 설치되어 있는지 확인
                    val instagramIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, "#${meal.name} #REcipe")
                        setPackage("com.instagram.android")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    try {
                        startActivity(instagramIntent)
                    } catch (e: Exception) {
                        // 인스타그램이 없으면 일반 공유창 열기
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_TEXT, "#${meal.name} #REcipe")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "이미지 공유"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "공유 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateMealImage(meal: MealRecord, imageUri: Uri) {
        // 로딩 표시 (선택사항)
        Toast.makeText(requireContext(), "사진을 업로드 중...", Toast.LENGTH_SHORT).show()

        mealViewModel.updateMealImage(
            mealRecordId = meal.id,
            imageUri = imageUri,
            onSuccess = {
                Toast.makeText(requireContext(), "✅ 사진이 추가되었습니다", Toast.LENGTH_SHORT).show()
                // 목록 새로고침
                viewModel.fetchMealRecordsForDate(selectedDate ?: today)
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "❌ 사진 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}