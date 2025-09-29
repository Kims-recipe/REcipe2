package com.kims.recipe2.ui.fridge

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kims.recipe2.R
import com.kims.recipe2.databinding.ActivityIngredientListBinding
import com.kims.recipe2.model.Ingredient
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class IngredientListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngredientListBinding
    private val viewModel: IngredientListViewModel by viewModels()
    private val fridgeViewModel: FridgeViewModel by viewModels() // 카테고리/위치 목록을 가져오기 위해 필요

    private var filterType = ""
    private var filterValue = ""
    private var selectedExpirationDate: Date? = null

    // 사용자가 입력한 재료 이름이 food_ingredients DB에 있는지 확인하기 위한 변수
    private var validIngredients = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngredientListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filterType = intent.getStringExtra("FILTER_TYPE") ?: ""
        filterValue = intent.getStringExtra("FILTER_VALUE") ?: ""

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        // 자동완성을 위해 Firestore의 전체 재료 목록을 미리 로드
        viewModel.fetchAllFoodIngredients()

        binding.fabAddIngredient.setOnClickListener {
            showAddIngredientDialog()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = "$filterValue 재료 목록"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val ingredientAdapter = IngredientAdapter(
            onItemClick = { ingredient ->
                showEditIngredientDialog(ingredient)
            },
            onDeleteClick = { ingredient ->
                // 삭제 버튼(➖)을 클릭하면 '전체 삭제'를 확인합니다.
                MaterialAlertDialogBuilder(this)
                    .setTitle("재료 삭제")
                    .setMessage("'${ingredient.name}'을(를) 정말 삭제하시겠습니까?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("삭제") { _, _ ->
                        viewModel.deleteIngredient(ingredient)
                    }
                    .show()
            }
        )
        binding.rvIngredientList.apply {
            layoutManager = LinearLayoutManager(this@IngredientListActivity)
            adapter = ingredientAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.ingredients.observe(this) { ingredients ->
            (binding.rvIngredientList.adapter as IngredientAdapter).submitList(ingredients)
        }
        // 액티비티가 시작될 때 필터링된 재료 목록을 가져옴
        viewModel.fetchFilteredIngredients(filterType, filterValue)
    }

    private fun showAddIngredientDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient_detail, null)

        // 다이얼로그의 View들 찾기
        val nameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.et_ingredient_name)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_category)
        val locationSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_location)
        val expirationDateEditText = dialogView.findViewById<TextInputEditText>(R.id.et_expiration_date)
        val toggleInputType = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggle_input_type)
        val quantityLayout = dialogView.findViewById<LinearLayout>(R.id.ll_quantity_input)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.til_amount)
        val quantityEditText = dialogView.findViewById<TextInputEditText>(R.id.et_quantity)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.et_amount)

        // 1. 재료 이름 자동완성 설정
        val autocompleteAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        nameEditText.setAdapter(autocompleteAdapter)
        nameEditText.threshold = 1

        nameEditText.setOnItemClickListener { _, _, position, _ ->
            val selectedName = autocompleteAdapter.getItem(position)
            if (selectedName != null) {
                viewModel.calculateExpirationDateFor(selectedName)
            }
        }

        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchFoodIngredients(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.searchResults.observe(this) { results ->
            validIngredients = results
            autocompleteAdapter.clear()
            autocompleteAdapter.addAll(results)
            autocompleteAdapter.notifyDataSetChanged()
        }

        viewModel.calculatedExpirationDate.observe(this) { date ->
            selectedExpirationDate = date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            expirationDateEditText.setText(dateFormat.format(date))
        }

        // 2. 유통기한 날짜 선택 다이얼로그 설정
        expirationDateEditText.setOnClickListener {
            showDatePickerDialog(expirationDateEditText)
        }

        // 3. 카테고리 및 위치 스피너(드롭다운 메뉴) 설정
        val categoryNames = fridgeViewModel.categories.value?.map { it.name } ?: emptyList()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        categorySpinner.setAdapter(categoryAdapter)

        val locationNames = listOf("냉동실", "냉장실", "야채실", "문짝")
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationNames)
        locationSpinner.setAdapter(locationAdapter)

        // FridgeFragment에서 전달받은 필터 값으로 기본 선택값 설정
        if (filterType == "category") {
            val catIndex = categoryNames.indexOf(filterValue)
            if (catIndex != -1) categorySpinner.setText(categoryNames[catIndex], false)
        } else if (filterType == "location") {
            val locIndex = locationNames.indexOf(filterValue)
            if (locIndex != -1) locationSpinner.setText(locationNames[locIndex], false)
        }

        // 4. '개수'/'양' 입력 방식 선택 토글 리스너
        toggleInputType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_select_quantity -> {
                        quantityLayout.visibility = View.VISIBLE
                        amountLayout.visibility = View.GONE
                        amountEditText.setText("0") // 반대쪽 값 초기화
                    }
                    R.id.btn_select_amount -> {
                        quantityLayout.visibility = View.GONE
                        amountLayout.visibility = View.VISIBLE
                        quantityEditText.setText("0") // 반대쪽 값 초기화
                    }
                }
            }
        }

        // 5. 다이얼로그 생성 및 '추가' 버튼 로직
        MaterialAlertDialogBuilder(this)
            .setTitle("새 재료 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val ingredientName = nameEditText.text.toString()
                val quantity = quantityEditText.text.toString().toIntOrNull() ?: 0
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0

                // 유효성 검사: 이름이 있고, 자동완성 목록에 있는 이름이어야 함
                if (ingredientName.isNotBlank() && validIngredients.contains(ingredientName)) {
                    val newIngredient = Ingredient(
                        name = ingredientName,
                        category = categorySpinner.text.toString(),
                        location = locationSpinner.text.toString(),
                        quantity = quantity,
                        amount = amount.toInt(),
                        unit = if (amount > 0) "g" else "개",
                        expirationDate = selectedExpirationDate
                    )
                    viewModel.addIngredient(newIngredient)
                    selectedExpirationDate = null // 다음 입력을 위해 초기화
                } else {
                    Toast.makeText(this, "목록에 있는 유효한 재료를 선택해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 2. [핵심] '재료 수정' 다이얼로그를 띄우는 함수 새로 추가
    private fun showEditIngredientDialog(ingredientToEdit: Ingredient) {
        // '재료 추가'와 동일한 레이아웃을 재사용합니다.
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient_detail, null)

        // View들 찾기 (showAddIngredientDialog와 동일)
        val nameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.et_ingredient_name)
        val categorySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_category)
        val locationSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_location)
        val expirationDateEditText = dialogView.findViewById<TextInputEditText>(R.id.et_expiration_date)
        val toggleInputType = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.toggle_input_type)
        val quantityLayout = dialogView.findViewById<LinearLayout>(R.id.ll_quantity_input)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.til_amount)
        val quantityEditText = dialogView.findViewById<TextInputEditText>(R.id.et_quantity)
        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.et_amount)


        // 3. 전달받은 ingredientToEdit의 정보로 다이얼로그의 각 필드를 채워줍니다.
        nameEditText.setText(ingredientToEdit.name)
        nameEditText.isEnabled = false // 재료 이름은 수정 불가로 설정

        // 카테고리/위치 스피너 설정 (showAddIngredientDialog와 동일)
        val categoryNames = fridgeViewModel.categories.value?.map { it.name } ?: emptyList()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        categorySpinner.setAdapter(categoryAdapter)
        categorySpinner.setText(ingredientToEdit.category, false)

        val locationNames = listOf("냉동실", "냉장실", "야채실", "문짝")
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locationNames)
        locationSpinner.setAdapter(locationAdapter)
        locationSpinner.setText(ingredientToEdit.location, false)

        // 개수/양 설정
        if (ingredientToEdit.amount > 0) {
            toggleInputType.check(R.id.btn_select_amount)
            quantityLayout.visibility = View.GONE
            amountLayout.visibility = View.VISIBLE
            amountEditText.setText(ingredientToEdit.amount.toString())
            quantityEditText.setText("0")
        } else {
            toggleInputType.check(R.id.btn_select_quantity)
            quantityLayout.visibility = View.VISIBLE
            amountLayout.visibility = View.GONE
            quantityEditText.setText(ingredientToEdit.quantity.toString())
            amountEditText.setText("0")
        }

        // 유통기한 설정
        ingredientToEdit.expirationDate?.let {
            selectedExpirationDate = it
            expirationDateEditText.setText(SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(it))
        }
        expirationDateEditText.setOnClickListener { showDatePickerDialog(expirationDateEditText) }

        toggleInputType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_select_quantity -> {
                        quantityLayout.visibility = View.VISIBLE
                        amountLayout.visibility = View.GONE
                        amountEditText.setText("0") // 반대쪽 값 초기화
                    }
                    R.id.btn_select_amount -> {
                        quantityLayout.visibility = View.GONE
                        amountLayout.visibility = View.VISIBLE
                        quantityEditText.setText("0") // 반대쪽 값 초기화
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("재료 정보 수정") // 다이얼로그 제목 변경
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ -> // 버튼 텍스트 변경
                val quantity = quantityEditText.text.toString().toIntOrNull() ?: 0
                val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0

                // 4. 기존 ingredientToEdit 객체의 내용을 업데이트
                val updatedIngredient = ingredientToEdit.copy(
                    category = categorySpinner.text.toString(),
                    location = locationSpinner.text.toString(),
                    quantity = quantity,
                    amount = amount.toInt(),
                    unit = if (amount > 0) "g" else "개",
                    expirationDate = selectedExpirationDate
                )
                // 5. ViewModel의 updateIngredient 함수 호출
                viewModel.updateIngredient(updatedIngredient)
                selectedExpirationDate = null
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDatePickerDialog(dateEditText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        selectedExpirationDate?.let { date -> calendar.time = date }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth, 0, 0, 0)
                selectedExpirationDate = calendar.time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateEditText.setText(dateFormat.format(selectedExpirationDate!!))
            },
            year, month, day
        ).show()
    }
}