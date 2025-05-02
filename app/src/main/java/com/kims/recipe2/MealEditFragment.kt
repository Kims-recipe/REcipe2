package com.kims.recipe2

import coil.load
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kims.recipe2.databinding.FragmentMealEditBinding
import com.kims.recipe2.ml.DetectedFood
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MealEditFragment : Fragment(R.layout.fragment_meal_edit) {

    private val items by lazy {
        Gson().fromJson(
            requireArguments().getString("detected")!!,
            object : TypeToken<List<DetectedFood>>() {}.type
        ) as MutableList<DetectedFood>
    }
    private lateinit var adapter: FoodEditAdapter
    private val photoUrl by lazy { requireArguments().getString("photoUrl")!! }

    override fun onViewCreated(v: View, s: Bundle?) {
        val binding = FragmentMealEditBinding.bind(v)
        Coil.load(binding.imgMeal.context, photoUrl) {
            target(binding.imgMeal)
        }
        adapter = FoodEditAdapter(items)
        binding.rvItems.adapter = adapter
        binding.btnSave.setOnClickListener { saveMeal() }
    }

    private fun saveMeal() {
        val user = Firebase.auth.currentUser ?: return
        lifecycleScope.launch {
            // 1. 영양 계산 Cloud Function 호출
            val nutrient = Firebase.functions
                .getHttpsCallable("getNutrients")
                .call(mapOf("items" to adapter.toItemMap()))
                .await<HashMap<String, Double>>()

            // 2. Firestore 저장
            val mealRef = Firebase.firestore.collection("meals").add(
                mapOf(
                    "uid" to user.uid,
                    "takenAt" to Timestamp.now(),
                    "photoUrl" to photoUrl,
                    "totalKcal" to nutrient["kcal"]
                )
            ).await()
            adapter.toItemMap().forEach {
                mealRef.collection("items").add(it).await()
            }
            Toast.makeText(requireContext(), "저장 완료!", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
}
