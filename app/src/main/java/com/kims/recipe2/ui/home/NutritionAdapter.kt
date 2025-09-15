package com.kims.recipe2.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.R
import com.kims.recipe2.databinding.ItemNutritionBarBinding
import com.kims.recipe2.model.NutritionItem

class NutritionAdapter : ListAdapter<NutritionItem, NutritionAdapter.NutritionViewHolder>(DiffCallback) {

    class NutritionViewHolder(private val binding: ItemNutritionBarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NutritionItem) {
            binding.tvIcon.text = item.icon
            binding.tvName.text = item.name
            binding.tvAmount.text = "${item.current.toInt()} / ${item.goal.toInt()} ${item.unit}"

            // 1. 퍼센트 계산
            val progress = if (item.goal > 0) {
                (item.current / item.goal * 100).toInt()
            } else {
                0
            }

            // 2. 퍼센트 값에 따라 ProgressBar 색상 결정
            val progressColor = when {
                progress < 80 -> ContextCompat.getColor(binding.root.context, R.color.nutrition_deficient) // 부족 (80% 미만)
                progress > 120 -> ContextCompat.getColor(binding.root.context, R.color.nutrition_excessive) // 초과 (120% 초과)
                else -> ContextCompat.getColor(binding.root.context, R.color.nutrition_good) // 정상 (80% ~ 120%)
            }

            // 3. UI에 적용
            binding.progressBar.progress = progress.coerceAtMost(200) // 초과 표현을 위해 최대값을 200까지 열어둠
            binding.progressBar.progressTintList = ColorStateList.valueOf(progressColor)
            binding.tvProgressPercentage.text = "$progress%"

            // 아이콘 배경색은 기존 로직 유지 (영양소 고유 색상)
            binding.tvIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(item.backgroundColorHex))

            // 부족/초과 상태에 따라 배경 하이라이트 (선택 사항)
            if (progress < 80 || progress > 120) {
                binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.deficient_background_color))
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NutritionItem>() {
        override fun areItemsTheSame(oldItem: NutritionItem, newItem: NutritionItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: NutritionItem, newItem: NutritionItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NutritionViewHolder {
        val binding = ItemNutritionBarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NutritionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NutritionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}