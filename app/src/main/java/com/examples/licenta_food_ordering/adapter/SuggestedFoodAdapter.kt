package com.examples.licenta_food_ordering.adaptar

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.licenta_food_ordering.R
import com.examples.licenta_food_ordering.presentation.activity.DetailsActivity
import com.examples.licenta_food_ordering.model.SuggestedFood

class SuggestedFoodAdapter(
    private val foodList: List<SuggestedFood>,
    private val listener: OnAddToCartClickListener
) : RecyclerView.Adapter<SuggestedFoodAdapter.SuggestedFoodViewHolder>() {

    interface OnAddToCartClickListener {
        fun onAddToCartClicked(food: SuggestedFood)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestedFoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.suggested_menu_item, parent, false)
        return SuggestedFoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestedFoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.bind(food)
    }

    override fun getItemCount(): Int = foodList.size

    inner class SuggestedFoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodImage: ImageView = itemView.findViewById(R.id.foodImage)
        private val restaurantName: TextView = itemView.findViewById(R.id.restaurantName)
        private val foodName: TextView = itemView.findViewById(R.id.foodName)
        private val foodPrice: TextView = itemView.findViewById(R.id.foodPrice)
        private val addToCartButton: TextView = itemView.findViewById(R.id.addToCartButton)

        fun bind(food: SuggestedFood) {
            // Load the image using Glide from the URL
            Glide.with(itemView.context)
                .load(food.foodImageResId) // URL from Firebase Storage
                .into(foodImage)

            restaurantName.text = food.restaurantName
            foodName.text = food.foodName
            foodPrice.text = food.foodPrice

            // Set the click listener for the Add to Cart button
            addToCartButton.setOnClickListener {
                listener.onAddToCartClicked(food)

                // Redirect to DetailsActivity when clicking on Add to Cart button
                openDetailsActivity(itemView.context, food)
            }
        }

        private fun openDetailsActivity(context: Context, food: SuggestedFood) {
            val intent = Intent(context, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", food.foodName)
                putExtra("MenuItemImage", food.foodImageResId)
                putExtra("MenuItemPrice", food.foodPrice)
            }
            context.startActivity(intent)
        }
    }
}