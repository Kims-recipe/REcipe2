// MealRecordAdapter.kt
package com.kims.recipe2.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kims.recipe2.databinding.ItemMealRecordBinding
import com.kims.recipe2.model.MealRecord

class MealRecordAdapter(
    private val onShareClick: (MealRecord) -> Unit,
    private val onItemClick: (MealRecord) -> Unit
) : ListAdapter<MealRecord, MealRecordAdapter.MealViewHolder>(MealDiffCallback) {

    private val mealIcons = mapOf("아침" to "🍳", "점심" to "🍜", "저녁" to "🥗", "간식" to "🍰")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding, onShareClick, onItemClick, mealIcons)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealViewHolder(
        private val binding: ItemMealRecordBinding,
        private val onShareClick: (MealRecord) -> Unit,
        private val onItemClick: (MealRecord) -> Unit, // ✨ 새로운 클릭 리스너 추가
        private val mealIcons: Map<String, String>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: MealRecord) {
            binding.tvMealName.text = meal.name
            binding.tvMealInfo.text = "칼로리: ${meal.calories}kcal | 단백질: ${meal.protein}g"

            // imageUri가 있으면 사진을 표시하고, 없으면 아이콘을 표시
            if (meal.imageUri != null && meal.imageUri.isNotEmpty()) {
                binding.mealImage.visibility = View.VISIBLE
                binding.mealIcon.visibility = View.GONE
                binding.mealImage.load(meal.imageUri)
            } else {
                binding.mealImage.visibility = View.GONE
                binding.mealIcon.visibility = View.VISIBLE
                binding.mealIcon.text = mealIcons[meal.type] ?: "🍴"
            }
            if (meal.isPlanned) {
                binding.btnShare.text = "📝 계획"
            } else {
                binding.btnShare.text = "📸 공유"
            }
            binding.btnShare.setOnClickListener { onShareClick(meal) }
            binding.root.setOnClickListener {
                onItemClick(meal)
            }
        }
    }

    object MealDiffCallback : DiffUtil.ItemCallback<MealRecord>() {
        override fun areItemsTheSame(oldItem: MealRecord, newItem: MealRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MealRecord, newItem: MealRecord): Boolean {
            return oldItem == newItem
        }
    }
}