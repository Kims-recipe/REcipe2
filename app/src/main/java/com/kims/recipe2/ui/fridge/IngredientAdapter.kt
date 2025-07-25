package com.kims.recipe2.ui.fridge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.model.Ingredient
import com.kims.recipe2.databinding.ItemIngredientBinding

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
