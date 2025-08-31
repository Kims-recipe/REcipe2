package com.kims.recipe2.ui.fridge

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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
    private val fridgeViewModel: FridgeViewModel by viewModels()
    private var filterType = ""
    private var filterValue = ""
    private var selectedExpirationDate: Date? = null

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
        val ingredientAdapter = IngredientAdapter()
        binding.rvIngredientList.apply {
            layoutManager = LinearLayoutManager(this@IngredientListActivity)
            adapter = ingredientAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.ingredients.observe(this) {
            (binding.rvIngredientList.adapter as IngredientAdapter).submitList(it)
        }
        viewModel.fetchFilteredIngredients(filterType, filterValue)
    }

    private fun showAddIngredientDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient_detail, null)
        val nameEditText = dialogView.findViewById<AutoCompleteTextView>(R.id.et_ingredient_name)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val locationSpinner = dialogView.findViewById<Spinner>(R.id.spinner_location)
        val quantityEditText = dialogView.findViewById<TextInputEditText>(R.id.et_quantity)
        val expirationDateTextView = dialogView.findViewById<TextView>(R.id.tv_expiration_date)

        val autocompleteAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        nameEditText.setAdapter(autocompleteAdapter)
        nameEditText.threshold = 1

        // 👇 [추가] 사용자가 추천 목록의 아이템을 클릭했을 때의 동작
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

        // 👇 [추가] 계산된 유통기한 LiveData 관찰 및 UI 업데이트
        viewModel.calculatedExpirationDate.observe(this) { date ->
            selectedExpirationDate = date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            expirationDateTextView.text = dateFormat.format(date)
        }

        expirationDateTextView.setOnClickListener {
            showDatePickerDialog(expirationDateTextView)
        }

        val categoryNames = fridgeViewModel.categories.value?.map { it.name } ?: emptyList()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinner.adapter = categoryAdapter

        val locationNames = listOf("냉동실", "냉장실", "야채실", "문짝")
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationNames)
        locationSpinner.adapter = locationAdapter

        if (filterType == "category") {
            val catIndex = categoryNames.indexOf(filterValue)
            if (catIndex != -1) categorySpinner.setSelection(catIndex)
        } else if (filterType == "location") {
            val locIndex = locationNames.indexOf(filterValue)
            if (locIndex != -1) locationSpinner.setSelection(locIndex)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("새 재료 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val ingredientName = nameEditText.text.toString()
                if (ingredientName.isNotEmpty() && validIngredients.contains(ingredientName)) {
                    val newIngredient = Ingredient(
                        name = ingredientName,
                        category = categorySpinner.selectedItem.toString(),
                        location = locationSpinner.selectedItem.toString(),
                        quantity = quantityEditText.text.toString().toIntOrNull() ?: 1,
                        expirationDate = selectedExpirationDate
                    )
                    viewModel.addIngredient(newIngredient)
                    selectedExpirationDate = null
                } else {
                    Toast.makeText(this, "목록에 있는 유효한 재료를 선택해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showDatePickerDialog(expirationDateTextView: TextView) {
        val calendar = Calendar.getInstance()
        selectedExpirationDate?.let { date -> calendar.time = date }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                selectedExpirationDate = calendar.time
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                expirationDateTextView.text = dateFormat.format(selectedExpirationDate!!)
            },
            year, month, day
        ).show()
    }
}