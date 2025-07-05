package com.kims.recipe2.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.model.Recipe
import com.kims.recipe2.databinding.ItemRecipeCardBinding

// 생성자에서 (Recipe) -> Unit 타입의 함수를 인자로 받습니다.
class RecipeAdapter(private val onItemClick: (Recipe) -> Unit) :
    ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback) {

    class RecipeViewHolder(private val binding: ItemRecipeCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // bind 함수에 클릭 리스너를 전달받아 설정합니다.
        fun bind(recipe: Recipe, onItemClick: (Recipe) -> Unit) {
            binding.tvRecipeTitle.text = recipe.title
            binding.tvRecipeDescription.text = recipe.description

            // 아이템 뷰 전체에 클릭 리스너를 설정합니다.
            itemView.setOnClickListener {
                // 클릭이 발생하면, 생성자에서 전달받은 onItemClick 함수를 호출합니다.
                onItemClick(recipe)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding =
            ItemRecipeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = getItem(position)
        // ViewHolder에 데이터와 함께 클릭 리스너 함수를 전달합니다.
        holder.bind(recipe, onItemClick)
    }

    companion object RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}