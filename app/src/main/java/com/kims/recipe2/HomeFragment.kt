package com.kims.recipe2

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.kims.recipe2.databinding.FragmentHomeBinding
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var ingredientAdapter: IngredientAdapter
    private var currentStorageType = StorageType.REFRIGERATOR

    // Firestore 매니저
    private val firestoreManager = FirestoreManager()

    // 재료 목록을 저장
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

        setupRecyclerView()
        setupStorageNavigation()
        setupAddButton()
        loadIngredientsFromFirestore() // Firestore에서 데이터 로드
    }

    private fun loadIngredientsFromFirestore() {
        // 초기화
        StorageType.values().forEach { storageType ->
            ingredientsByStorage[storageType] = mutableListOf()
        }

        // Firestore에서 모든 재료 가져오기
        firestoreManager.getAllIngredients(
            onSuccess = { ingredients ->
                // 모든 재료를 저장 위치별로 분류
                ingredients.forEach { ingredient ->
                    ingredientsByStorage[ingredient.storageType]?.add(ingredient)
                }
                updateIngredientList() // UI 업데이트
            },
            onFailure = { exception ->
                Toast.makeText(context, "데이터 로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )

        // 실시간 업데이트 감지 설정
        firestoreManager.observeIngredients { ingredients ->
            // 저장소별로 재료 분류
            StorageType.values().forEach { storageType ->
                ingredientsByStorage[storageType] = ingredients
                    .filter { it.storageType == storageType }
                    .toMutableList()
            }
            updateIngredientList() // UI 업데이트
        }
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
            // 새 액티비티로 이동
            val intent = Intent(requireContext(), AddIngredientActivity::class.java)
            intent.putExtra("storageType", currentStorageType)
            startActivity(intent)
        }
    }

    // 처음에 다이얼로그로만 할까 하다가 사진도 넣어야하고 음성인식도 해야해서 엑티비티로 바꾸고 일단 지움
//    private fun setupAddButton() {
//        binding.fabAddIngredient.setOnClickListener {
//            showAddIngredientDialog()
//        }
//    }
//
//
//
//    private fun showAddIngredientDialog() {
//        val dialogView = layoutInflater.inflate(R.layout.add_ingredient_dialog, null)
//        val etName = dialogView.findViewById<TextInputEditText>(R.id.etIngredientName)
//        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etIngredientAmount)
//        val datePicker = dialogView.findViewById<DatePicker>(R.id.dpExpiration)
//
//        val dialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .setPositiveButton("추가") { _, _ ->
//                val ingredientName = etName.text.toString()
//
//                if (ingredientName.isBlank()) {
//                    Toast.makeText(context, "재료 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
//                    return@setPositiveButton
//                }
//
//                val amount = etAmount.text.toString().toIntOrNull() ?: 0
//
//                // 유통기한 날짜 설정
//                val calendar = Calendar.getInstance()
//                calendar.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
//                val expirationDate = calendar.time
//
//                // 새 재료 객체 생성
//                val newIngredient = IngredientItem(
//                    id = UUID.randomUUID().toString(),
//                    name = ingredientName,
//                    storageType = currentStorageType,
//                    number = amount,
//                    expiration = expirationDate,
//                    imageUrl = null  // 이미지는 나중에 추가 가능
//                )
//
//                // Firestore에 저장
//                saveIngredientToFirestore(newIngredient)
//            }
//            .setNegativeButton("취소", null)
//            .create()
//
//        dialog.show()
//    }

//    private fun saveIngredientToFirestore(ingredient: IngredientItem) {
//        firestoreManager.saveIngredient(
//            ingredient = ingredient,
//            onSuccess = {
//                Toast.makeText(context, "${ingredient.name} 추가 완료", Toast.LENGTH_SHORT).show()
//
//                // 로컬 목록에도 추가 (실시간 리스너로 업데이트가 되지만, 즉각적인 UI 반응을 위해)
//                ingredientsByStorage[ingredient.storageType]?.add(ingredient)
//                updateIngredientList()
//            },
//            onFailure = { exception ->
//                Toast.makeText(context, "추가 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//        )
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}