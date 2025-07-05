package com.kims.recipe2.ui.fridge
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.databinding.ItemFridgeCategoryBinding
import com.kims.recipe2.model.FridgeCategory

class FridgeCategoryAdapter(private val onClick: (FridgeCategory) -> Unit) :
    ListAdapter<FridgeCategory, FridgeCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemFridgeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(private val binding: ItemFridgeCategoryBinding, val onClick: (FridgeCategory) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(category: FridgeCategory) {
            binding.tvIcon.text = category.icon
            binding.iconCard.setCardBackgroundColor(Color.parseColor(category.backgroundColorHex))
            binding.tvCategoryName.text = category.name
            binding.tvCategoryExamples.text = category.examples
            binding.root.setOnClickListener {
                onClick(category)
            }
        }
    }

    object CategoryDiffCallback : DiffUtil.ItemCallback<FridgeCategory>() {
        override fun areItemsTheSame(oldItem: FridgeCategory, newItem: FridgeCategory): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: FridgeCategory, newItem: FridgeCategory): Boolean {
            return oldItem == newItem
        }
    }
}