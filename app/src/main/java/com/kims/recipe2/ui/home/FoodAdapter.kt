package com.kims.recipe2.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.model.Food
import com.kims.recipe2.databinding.ItemFoodBinding // 새로 만들 레이아웃의 바인딩

class FoodAdapter(private val onItemClick: (Food) -> Unit) :
    ListAdapter<Food, FoodAdapter.FoodViewHolder>(FoodDiffCallback) {

    class FoodViewHolder(private val binding: ItemFoodBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(food: Food, onItemClick: (Food) -> Unit) {
            binding.tvFoodName.text = food.name
            binding.tvFoodInfo.text = "열량: ${food.calories}kcal | 단백질: ${food.protein}g"
            itemView.setOnClickListener { onItemClick(food) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding =
            ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    companion object FoodDiffCallback : DiffUtil.ItemCallback<Food>() {
        override fun areItemsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Food, newItem: Food): Boolean {
            return oldItem == newItem
        }
    }
}