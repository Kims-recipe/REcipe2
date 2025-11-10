package com.kims.recipe2.ui.fridge

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.kims.recipe2.R
import com.kims.recipe2.databinding.FragmentEatingOutBinding
import android.provider.OpenableColumns // 파일 이름 추출을 위해 추가
import android.app.AlertDialog // AlertDialog import 추가

/**
 * 푸드샷: 갤러리 이미지를 선택하고, 파일 이름(더미)을 기반으로 이름을 자동 결정하여 다음 단계로 전달합니다.
 */
class FoodShotFragment : Fragment() {

    private var _binding: FragmentEatingOutBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    // 💡 [수정] 파일 이름과 한글 이름을 매핑합니다.
    private val mealFilenameMap = mapOf(
        "hamburger.jpg" to "햄버거",
        "salad.jpg" to "샐러드",
        "pizza.webp" to "피자"
    )

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivMealPreview.setImageURI(selectedImageUri)

                binding.llPlaceholderGroup.visibility = View.GONE
                binding.ivMealPreview.visibility = View.VISIBLE

                // 💡 [수정] URI를 기반으로 파일 이름 추출 및 식사 이름 자동 결정
                val mealName = getMealNameFromUri(uri)
                binding.etMealName.setText(mealName)
            }
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

        binding.etMealName.hint = "사진을 선택하면 식사 이름이 자동 입력됩니다."

        binding.toggleMealTime.visibility = View.GONE

        setupImagePicker()

        binding.btnSaveMeal.text = "다음"
        binding.btnSaveMeal.setOnClickListener {
            passResultToNextFragment()
        }
    }

    private fun setupImagePicker() {
        binding.flImageContainer.setOnClickListener {
            openImagePicker()
        }
        binding.llPlaceholderGroup.visibility = View.VISIBLE
        binding.ivMealPreview.visibility = View.GONE
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }



    // 💡 [수정] ContentResolver를 사용하여 URI에서 파일 이름을 추출하고 매핑합니다.
    private fun getMealNameFromUri(uri: Uri): String {
        var fileName: String? = null

        // ContentResolver를 사용하여 파일 이름을 가져옵니다.
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // OpenableColumns.DISPLAY_NAME은 파일 시스템의 실제 파일 이름과 일치하지 않을 수 있으나,
                // 시뮬레이션 환경에서 가장 접근하기 쉬운 '이름' 필드입니다.
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        // 추출된 파일 이름(예: "hamburger.jpg")을 기반으로 매핑합니다.
        // 💡 [중요]: 파일 이름은 소문자로 변환하여 매핑합니다.
        val lowerCaseFileName = fileName?.toLowerCase()?.trim() ?: ""

        return mealFilenameMap[lowerCaseFileName] ?: "자동 인식 식사 (불일치)"
    }

    private fun passResultToNextFragment() {
        val mealName = binding.etMealName.text.toString().trim()

        if (mealName.isEmpty() || selectedImageUri == null) {
            Toast.makeText(requireContext(), "식사 사진을 선택하고 기록을 완료하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (mealName == "자동 인식 식사 (불일치)") {
            AlertDialog.Builder(requireContext())
                .setTitle("인식 실패")
                .setMessage("선택된 사진은 (hamburger.jpg, pizza.webp, salad.jpeg)와 일치하지 않습니다. 직접 이름을 수정하거나 다른 사진을 선택하세요.")
                .setPositiveButton("확인", null)
                .show()
            return
        }

        val resultBundle = Bundle().apply {
            putString("mealName", mealName)
            putString("imageUri", selectedImageUri.toString())
            // 💡 [핵심 수정] mealType을 전달하지 않거나 null을 전달
            // putString("mealType", null) // String은 기본적으로 null 가능
        }

        // MealActivity의 setFragmentResultListener로 결과를 전달
        setFragmentResult("foodShotResult", resultBundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}