package com.examples.licenta_food_ordering.adaptar

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.PopulerItemBinding
import com.examples.licenta_food_ordering.Restaurant
import com.examples.licenta_food_ordering.RestaurantDetailsActivity

class PopularAdapter(
    private val items: List<Restaurant>,
    private val context: Context
) : RecyclerView.Adapter<PopularAdapter.PopulerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopulerViewHolder {
        val binding = PopulerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PopulerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopulerViewHolder, position: Int) {
        val restaurant = items[position]
        holder.bind(restaurant)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RestaurantDetailsActivity::class.java).apply {
                putExtra("RESTAURANT_ID", restaurant.id)
                putExtra("RestaurantName", restaurant.name)
                putExtra("RestaurantAddress", restaurant.address)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class PopulerViewHolder(private val binding: PopulerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(restaurant: Restaurant) {
            binding.restaurantName.text = restaurant.name
            binding.restaurantAddress.text = restaurant.address

            Glide.with(binding.root.context)
                .load(R.drawable.restaurant_logo)
                .into(binding.restaurantImage)
        }
    }
}