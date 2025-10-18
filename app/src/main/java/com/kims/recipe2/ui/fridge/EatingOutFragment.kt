package com.kims.recipe2.ui.fridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kims.recipe2.R
import com.kims.recipe2.databinding.FragmentEatingOutBinding
import com.kims.recipe2.model.MealRecord
import java.util.UUID

class EatingOutFragment : Fragment() {

    private var _binding: FragmentEatingOutBinding? = null
    private val binding get() = _binding!!

    private val mealViewModel: MealViewModel by viewModels()

    private var selectedMealTime: String = "아침"
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.ivMealPreview.setImageURI(selectedImageUri)

            binding.llPlaceholderGroup.visibility = View.GONE
            binding.ivMealPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEatingOutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMealTimeToggleGroup()
        setupImagePicker()
        setupSaveButton()
    }

    private fun setupMealTimeToggleGroup() {
        // 토글 버튼 그룹에 리스너 설정
        binding.toggleMealTime.addOnButtonCheckedListener { group, checkedId, isChecked ->
            // isChecked는 버튼이 선택되었을 때만 true가 됨
            if (isChecked) {
                selectedMealTime = when (checkedId) {
                    R.id.btn_breakfast -> "아침"
                    R.id.btn_lunch -> "점심"
                    R.id.btn_dinner -> "저녁"
                    else -> "아침" // 혹시 모를 기본값
                }
            }
        }
    }

    private fun setupImagePicker() {
        binding.flImageContainer.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveMeal.setOnClickListener {
            saveMealRecord()
        }
    }

    private fun saveMealRecord() {
        val mealName = binding.etMealName.text.toString().trim()

        if (mealName.isEmpty()) {
            Toast.makeText(requireContext(), "식사 이름을 입력하세요", Toast.LENGTH_SHORT).show()
            return
        }

        // MealViewModel에 isHomemade=false, imageUri 전달
        mealViewModel.saveEatingOutRecord(
            mealName = mealName,
            mealType = selectedMealTime,
            imageUri = selectedImageUri?.toString(),
            isHomemade = false, // 외식이므로 false 설정
            onSuccess = {
                Toast.makeText(requireContext(), "✅ 외식 기록 완료!", Toast.LENGTH_SHORT).show()
                binding.etMealName.text?.clear()
                selectedImageUri = null
                // ✅ MealActivity를 종료하고 MainActivity에 신호를 보냅니다.
                val resultIntent = Intent().apply {
                    putExtra("NAVIGATE_TO_CALENDAR", true)
                }
                requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                requireActivity().finish()
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "❌ 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}