package com.examples.licenta_food_ordering.adaptar

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.licenta_food_ordering.databinding.PopulerItemBinding
import com.examples.licenta_food_ordering.DetailsActivity

class PopularAdapter (private val items:List<String>, private val price:List<String>, private val image:List<Int>, private val requireContext : Context) : RecyclerView.Adapter<PopularAdapter.PopulerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopulerViewHolder {
        return PopulerViewHolder(PopulerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PopulerViewHolder, position: Int) {
        val item = items[position]
        val images = image[position]
        val price = price[position]
        holder.bind(item, price, images)

        holder.itemView.setOnClickListener {
            val intent = Intent(requireContext, DetailsActivity::class.java)
            intent.putExtra("MenuItemName", item)
            intent.putExtra("MenuItemImage", images)
            requireContext.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class PopulerViewHolder (private val binding: PopulerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private val imagesView = binding.foodImage
        fun bind(item: String, price: String, images: Int) {
            binding.foodName.text = item
            binding.PricePopuler.text = price
            imagesView.setImageResource(images)
        }

    }
}