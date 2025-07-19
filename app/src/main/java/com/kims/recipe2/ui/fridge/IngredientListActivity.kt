package com.kims.recipe2.ui.fridge

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.kims.recipe2.R
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.databinding.ActivityIngredientListBinding

class IngredientListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngredientListBinding
    private val viewModel: IngredientListViewModel by viewModels()
    private val fridgeViewModel: FridgeViewModel by viewModels() // 카테고리 목록을 가져오기 위함
    private var filterType = ""
    private var filterValue = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngredientListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 필터 정보 가져오기
        filterType = intent.getStringExtra("FILTER_TYPE") ?: ""
        filterValue = intent.getStringExtra("FILTER_VALUE") ?: ""

        // 툴바 설정
        binding.toolbar.title = "$filterValue 재료 목록"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // RecyclerView 및 Adapter 설정
        val ingredientAdapter = IngredientAdapter()
        binding.rvIngredientList.apply {
            layoutManager = LinearLayoutManager(this@IngredientListActivity)
            adapter = ingredientAdapter
        }

        // 필터링된 재료 목록 관찰
        viewModel.ingredients.observe(this) {
            ingredientAdapter.submitList(it)
        }
        viewModel.fetchFilteredIngredients(filterType, filterValue)

        // 재료 추가 FAB 클릭 리스너
        binding.fabAddIngredient.setOnClickListener {
            showAddIngredientDialog()
        }
    }

    private fun showAddIngredientDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient_detail, null)
        val nameEditText = dialogView.findViewById<TextInputEditText>(R.id.et_ingredient_name)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinner_category)
        val locationSpinner = dialogView.findViewById<Spinner>(R.id.spinner_location)
        val quantityEditText = dialogView.findViewById<TextInputEditText>(R.id.et_quantity)

        // 카테고리 스피너 설정
        val categoryNames = fridgeViewModel.categories.value?.map { it.name } ?: emptyList()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categorySpinner.adapter = categoryAdapter

        // 위치 스피너 설정
        val locationNames = listOf("냉동실", "냉장실", "야채실", "문짝")
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationNames)
        locationSpinner.adapter = locationAdapter

        // 필터 정보에 따라 기본값 설정
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
                val newIngredient = Ingredient(
                    name = nameEditText.text.toString(),
                    category = categorySpinner.selectedItem.toString(),
                    location = locationSpinner.selectedItem.toString(),
                    quantity = quantityEditText.text.toString().toIntOrNull() ?: 1
                )
                if (newIngredient.name.isNotEmpty()) {
                    viewModel.addIngredient(newIngredient)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}