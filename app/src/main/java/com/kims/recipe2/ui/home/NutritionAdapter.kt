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
            // 소수점 없이 정수로 표시하도록 수정
            binding.tvAmount.text = "${item.current.toInt()} / ${item.goal.toInt()} ${item.unit}"

            // 👇 [수정] isDeficient 값에 따라 itemColor를 한 번만 선언합니다.
            val itemColor = if (item.isDeficient) {
                ContextCompat.getColor(binding.root.context, R.color.deficient_color)
            } else {
                Color.parseColor(item.backgroundColorHex)
            }

            binding.tvIcon.backgroundTintList = ColorStateList.valueOf(itemColor)
            binding.progressBar.progressTintList = ColorStateList.valueOf(itemColor)

            // 배경색 설정 (부족할 경우에만 특별한 배경색 적용)
            if (item.isDeficient) {
                val backgroundColor = ContextCompat.getColor(binding.root.context, R.color.deficient_background_color)
                binding.root.setBackgroundColor(backgroundColor)
            } else {
                binding.root.setBackgroundColor(Color.TRANSPARENT) // 기본 배경색
            }

            val progress = if (item.goal > 0) {
                (item.current / item.goal * 100).toInt()
            } else {
                0
            }
            binding.progressBar.progress = progress.coerceAtMost(100)
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