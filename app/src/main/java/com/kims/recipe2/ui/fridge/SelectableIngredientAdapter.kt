package com.kims.recipe2.ui.fridge

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.databinding.ItemIngredientBinding
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.util.DateUtil
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SelectableIngredientAdapter(
    private val onSelectionChanged: (List<Ingredient>) -> Unit
) : ListAdapter<Ingredient, SelectableIngredientAdapter.ViewHolder>(DiffCallback) {

    private val selectedItems = mutableSetOf<Ingredient>()

    inner class ViewHolder(private val binding: ItemIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: Ingredient) {
            binding.tvIngredientName.text = ingredient.name
            binding.tvIngredientCategory.text = ingredient.category
            binding.tvIngredientQuantity.text = "${ingredient.quantity}${ingredient.unit}"

            // D-day 계산 및 표시
            ingredient.expirationDate?.let { expirationDate ->
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val diffInMillis = expirationDate.time - today.time
                val diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)

                binding.tvIngredientDday.text = when {
                    diffInDays == 0L -> "D-day"
                    diffInDays > 0 -> "D-${diffInDays}"
                    else -> "D+${-diffInDays}" // 유통기한 지남
                }
                // 유통기한이 임박하거나 지난 경우 색상 변경 (선택 사항)
                if (diffInDays <= 3 && diffInDays >=0) {
                    binding.tvIngredientDday.setTextColor(Color.parseColor("#FF9800")) // 주황색
                } else if (diffInDays < 0) {
                    binding.tvIngredientDday.setTextColor(Color.RED) // 빨간색
                } else {
                    binding.tvIngredientDday.setTextColor(Color.BLACK) // 기본색 (검정)
                }
            } ?: run {
                binding.tvIngredientDday.text = "" // 유통기한 정보가 없으면 빈 문자열
            }

            // 선택 여부에 따라 배경색 변경
            binding.root.setBackgroundColor(
                if (selectedItems.contains(ingredient)) Color.parseColor("#E0F7FA")
                else Color.WHITE
            )

            binding.root.setOnClickListener {
                if (selectedItems.contains(ingredient)) {
                    selectedItems.remove(ingredient)
                } else {
                    selectedItems.add(ingredient)
                }
                notifyItemChanged(adapterPosition)
                onSelectionChanged(selectedItems.toList())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 현재 선택된 재료 목록을 반환하는 함수 추가
    fun getSelectedItems(): List<Ingredient> {
        return selectedItems.toList()
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
