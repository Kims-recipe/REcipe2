package com.kims.recipe2.ui.fridge

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.databinding.ItemIngredientBinding
import com.kims.recipe2.model.Ingredient

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

    companion object DiffCallback : DiffUtil.ItemCallback<Ingredient>() {
        override fun areItemsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ingredient, newItem: Ingredient): Boolean {
            return oldItem == newItem
        }
    }
}
