package com.kims.recipe2.ui.fridge

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.databinding.ItemIngredientBinding
import com.kims.recipe2.util.DateUtil

class IngredientAdapter(
    private val onItemClick: ((Ingredient) -> Unit)? = null
) : ListAdapter<Ingredient, IngredientAdapter.IngredientViewHolder>(DiffCallback) {

    class IngredientViewHolder(
        private val binding: ItemIngredientBinding,
        private val onItemClick: ((Ingredient) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient: Ingredient) {
            binding.tvIngredientName.text = ingredient.name
            binding.tvIngredientCategory.text = ingredient.category
            binding.tvIngredientQuantity.text = "${ingredient.quantity}${ingredient.unit}"

            // D-day 계산 및 표시
            ingredient.expirationDate?.let { expDate ->
                val dDay = DateUtil.calculateDDay(expDate)
                if (dDay != null) {
                    binding.tvIngredientDday.visibility = View.VISIBLE // TextView 보이게 설정
                    when {
                        dDay < 0 -> {
                            binding.tvIngredientDday.text = "D+${-dDay}" // 유통기한 지남
                            binding.tvIngredientDday.setTextColor(Color.parseColor("#FF0000")) // 빨간색
                        }
                        dDay == 0L -> {
                            binding.tvIngredientDday.text = "D-Day" // 오늘까지
                            binding.tvIngredientDday.setTextColor(Color.parseColor("#FFC107")) // 주황색 (예시)
                        }
                        dDay <= 7 -> {
                            binding.tvIngredientDday.text = "D-${dDay}" // 7일 이내
                            binding.tvIngredientDday.setTextColor(Color.parseColor("#FF9800")) // 노란색 (예시)
                        }
                        else -> {
                            binding.tvIngredientDday.text = "D-${dDay}" // 7일 초과
                            binding.tvIngredientDday.setTextColor(Color.parseColor("#4CAF50")) // 초록색 (예시)
                        }
                    }
                } else {
                    binding.tvIngredientDday.visibility = View.GONE // 유통기한 없으면 숨김
                }
            } ?: run {
                binding.tvIngredientDday.visibility = View.GONE // expirationDate가 null인 경우 숨김
            }


            binding.root.setOnClickListener {
                onItemClick?.invoke(ingredient)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IngredientViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Ingredient>() {
        override fun areItemsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean {
            return oldItem == newItem
        }
    }
}
