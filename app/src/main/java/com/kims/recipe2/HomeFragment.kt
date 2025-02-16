package com.kims.recipe2

import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var ingredientAdapter: IngredientAdapter
    private var currentStorageType = StorageType.REFRIGERATOR

    // 임시 데이터 (나중에 데이터베이스로 교체)
    private val ingredientsByStorage = mutableMapOf<StorageType, MutableList<IngredientItem>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeDummyData() // 임시 데이터 초기화
        setupRecyclerView()
        setupStorageNavigation()
        setupAddButton()
        updateIngredientList() // 초기 데이터 표시
    }

    private fun initializeDummyData() {
        // 임시 데이터 초기화 (나중에 데이터베이스로 교체)
        StorageType.values().forEach { storageType ->
            ingredientsByStorage[storageType] = mutableListOf()
        }

        // 샘플 데이터
        ingredientsByStorage[StorageType.REFRIGERATOR]?.addAll(
            listOf(
//                IngredientItem("1", "우유", StorageType.REFRIGERATOR, 1, 2025, "5"),
//                IngredientItem("2", "계란", StorageType.REFRIGERATOR)
            )
        )
        ingredientsByStorage[StorageType.FREEZER]?.addAll(
            listOf(

            )
        )
        ingredientsByStorage[StorageType.PANTRY]?.addAll(
            listOf(

            )
        )
    }

    private fun setupRecyclerView() {
        ingredientAdapter = IngredientAdapter()
        binding.rvIngredients.apply {
            adapter = ingredientAdapter
            layoutManager = GridLayoutManager(context, 4)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(4, 4, 4, 4)
                }
            })
        }
    }

    private fun setupStorageNavigation() {
        updateStorageName()

        binding.btnPrevStorage.setOnClickListener {
            val values = StorageType.values()
            val currentIndex = values.indexOf(currentStorageType)
            if (currentIndex > 0) {
                currentStorageType = values[currentIndex - 1]
                updateStorageName()
                updateIngredientList()
            }
        }

        binding.btnNextStorage.setOnClickListener {
            val values = StorageType.values()
            val currentIndex = values.indexOf(currentStorageType)
            if (currentIndex < values.size - 1) {
                currentStorageType = values[currentIndex + 1]
                updateStorageName()
                updateIngredientList()
            }
        }
    }

    private fun updateStorageName() {
        binding.tvStorageName.text = currentStorageType.getDisplayName()

        val values = StorageType.values()
        val currentIndex = values.indexOf(currentStorageType)
        binding.btnPrevStorage.isEnabled = currentIndex > 0
        binding.btnNextStorage.isEnabled = currentIndex < values.size - 1
    }

    private fun updateIngredientList() {
        // 현재 선택된 저장공간의 재료 목록만 표시
        ingredientsByStorage[currentStorageType]?.let { ingredients ->
            ingredientAdapter.updateIngredients(ingredients)
        }
    }

    private fun setupAddButton() {
        binding.fabAddIngredient.setOnClickListener {
            addNewIngredient()
        }
    }

    private fun addNewIngredient() {
        // 임시로 재료 추가 (나중에 다이얼로그나 새 화면으로 대체)
        val newId = System.currentTimeMillis().toString()
        val newIngredient = IngredientItem(
            id = newId,
            name = "새로운 재료 $newId",
            storageType = currentStorageType,
            number = 10,
            expiration = null,
            imageUrl=""
        )

        ingredientsByStorage[currentStorageType]?.add(newIngredient)
        updateIngredientList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
