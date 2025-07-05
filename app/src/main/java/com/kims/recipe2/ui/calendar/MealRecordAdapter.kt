package com.kims.recipe2.ui.calendar


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.databinding.ItemMealRecordBinding
import com.kims.recipe2.model.MealRecord

class MealRecordAdapter(private val onShareClick: (MealRecord) -> Unit) :
    ListAdapter<MealRecord, MealRecordAdapter.MealViewHolder>(MealDiffCallback) {

    private val mealIcons = mapOf("아침" to "🍳", "점심" to "🍜", "저녁" to "🥗", "간식" to "🍰")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding, onShareClick, mealIcons)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealViewHolder(
        private val binding: ItemMealRecordBinding,
        private val onShareClick: (MealRecord) -> Unit,
        private val mealIcons: Map<String, String>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: MealRecord) {
            binding.mealImage.text = mealIcons[meal.type] ?: "🍴"
            binding.tvMealName.text = meal.name
            binding.tvMealInfo.text = "칼로리: ${meal.calories}kcal | 단백질: ${meal.protein}g"
            if(meal.isPlanned) {
                binding.btnShare.text = "📝 계획"
            } else {
                binding.btnShare.text = "📸 공유"
            }
            binding.btnShare.setOnClickListener { onShareClick(meal) }
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