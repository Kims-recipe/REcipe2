package com.kims.recipe2.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.model.NutritionItem
import com.kims.recipe2.databinding.ItemNutritionBarBinding

class NutritionAdapter : ListAdapter<NutritionItem, NutritionAdapter.NutritionViewHolder>(DiffCallback) {

    /**
     * ViewHolder: 각 아이템의 뷰를 보관하는 객체입니다.
     * bind 함수를 통해 데이터와 뷰를 연결합니다.
     */
    class NutritionViewHolder(private val binding: ItemNutritionBarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NutritionItem) {
            // 1. 데이터 모델의 값들을 뷰에 설정합니다.
            binding.tvIcon.text = item.icon
            binding.tvName.text = item.name
            binding.tvAmount.text = "${item.current} / ${item.goal} ${item.unit}"

            // 2. 아이콘 배경색과 ProgressBar 색상을 데이터에 맞게 동적으로 변경합니다.
            val itemColor = Color.parseColor(item.backgroundColorHex)
            binding.tvIcon.backgroundTintList = ColorStateList.valueOf(itemColor)
            binding.progressBar.progressTintList = ColorStateList.valueOf(itemColor)

            // 3. 목표량 대비 현재 섭취량의 비율(%)을 계산하여 ProgressBar에 적용합니다.
            val progress = if (item.goal > 0) {
                (item.current / item.goal * 100).toInt()
            } else {
                0
            }
            // progress 값은 100을 넘지 않도록 합니다.
            binding.progressBar.progress = progress.coerceAtMost(100)
        }
    }

    /**
     * DiffUtil.ItemCallback: ListAdapter가 리스트의 변경사항을 효율적으로 계산하기 위해 사용합니다.
     * 이를 통해 부드러운 애니메이션과 높은 성능을 얻을 수 있습니다.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<NutritionItem>() {
        // 두 아이템이 동일한 객체인지 ID 등으로 확인합니다.
        override fun areItemsTheSame(oldItem: NutritionItem, newItem: NutritionItem): Boolean {
            return oldItem.name == newItem.name
        }

        // 두 아이템의 내용이 동일한지 확인합니다. 내용이 다를 경우에만 뷰를 다시 그립니다.
        override fun areContentsTheSame(oldItem: NutritionItem, newItem: NutritionItem): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * RecyclerView가 새로운 ViewHolder를 필요로 할 때 호출됩니다.
     * 여기에서 아이템 레이아웃을 인플레이트(inflate)하고 ViewHolder를 생성합니다.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NutritionViewHolder {
        val binding = ItemNutritionBarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NutritionViewHolder(binding)
    }

    /**
     * ViewHolder를 데이터와 연결(bind)할 때 호출됩니다.
     */
    override fun onBindViewHolder(holder: NutritionViewHolder, position: Int) {
        // 현재 위치의 데이터를 가져옵니다.
        val item = getItem(position)
        // ViewHolder의 bind 함수를 호출하여 데이터를 뷰에 표시합니다.
        holder.bind(item)
    }
}