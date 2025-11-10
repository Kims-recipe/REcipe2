package com.kims.recipe2.ui.mypage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kims.recipe2.databinding.ItemUserRecipeBinding
import com.kims.recipe2.model.UserRecipe

class UserRecipeAdapter(private val onClick: (UserRecipe) -> Unit) :
    ListAdapter<UserRecipe, UserRecipeAdapter.RecipeViewHolder>(RecipeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemUserRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        private val binding: ItemUserRecipeBinding,
        private val onClick: (UserRecipe) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: UserRecipe) {
            binding.tvRecipeName.text = recipe.name
            binding.root.setOnClickListener {
                onClick(recipe)
            }
        }
    }

    object RecipeDiffCallback : DiffUtil.ItemCallback<UserRecipe>() {
        override fun areItemsTheSame(oldItem: UserRecipe, newItem: UserRecipe): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: UserRecipe, newItem: UserRecipe): Boolean {
            return oldItem == newItem
        }
    }
}