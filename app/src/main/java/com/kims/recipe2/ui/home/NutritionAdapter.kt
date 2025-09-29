package com.kims.recipe2.ui.home

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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

            // 섭취량의 퍼센트 계산
            val progress = if (item.goal > 0) {
                (item.current / item.goal * 100).toInt()
            } else {
                0
            }
            val percentageText = "${progress}%"
            binding.tvPercentage.text = percentageText

            // 퍼센트에 따른 색상 변경
            if (progress <= 125 && progress >= 75){
                binding.tvPercentage.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_blue_light))
            } else {
                binding.tvPercentage.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_light))
            }

            // 2. 아이콘 배경색과 ProgressBar 색상을 데이터에 맞게 동적으로 변경합니다.
            val itemColor = Color.parseColor(item.backgroundColorHex)
            binding.tvIcon.backgroundTintList = ColorStateList.valueOf(itemColor)
            binding.progressBar.progressTintList = ColorStateList.valueOf(itemColor)

            // 현재 진행 상태에서 목표 진행 상태까지 애니메이트
            val animator = ValueAnimator.ofInt(0, progress.coerceAtMost(100))
            animator.duration = 500L
            animator.addUpdateListener { animation ->
                val animatedProgress = animation.animatedValue as Int
                binding.progressBar.progress = animatedProgress
            }
            animator.start()
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
        // 현재 위치의 데이터를 가져옵니다.
        val item = getItem(position)
        // ViewHolder의 bind 함수를 호출하여 데이터를 뷰에 표시합니다.
        holder.bind(item)
    }
}