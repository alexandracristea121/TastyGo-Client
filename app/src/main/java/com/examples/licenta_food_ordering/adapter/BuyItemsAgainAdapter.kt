package com.examples.licenta_food_ordering.adaptar

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.licenta_food_ordering.databinding.BuyAgainItemBinding

class BuyItemsAgainAdapter(
    private val buyAgainFoodName: MutableList<String>,
    private val buyAgainFoodPrice: MutableList<String>,
    private val buyAgainFoodImage: MutableList<String>,
    private var requireContext: Context
) : RecyclerView.Adapter<BuyItemsAgainAdapter.BuyAgainViewHolder>() {

    private var itemClickListener: OnItemClickListener? = null

    // Define an interface for item clicks
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    // Allow setting the listener from outside
    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }

    override fun onBindViewHolder(holder: BuyAgainViewHolder, position: Int) {
        val validPosition = minOf(buyAgainFoodName.size, buyAgainFoodPrice.size, buyAgainFoodImage.size)
        if (position < validPosition) {
            holder.bind(
                buyAgainFoodName[position],
                buyAgainFoodPrice[position],
                buyAgainFoodImage[position]
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyAgainViewHolder {
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuyAgainViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return minOf(buyAgainFoodName.size, buyAgainFoodPrice.size, buyAgainFoodImage.size)
    }

    inner class BuyAgainViewHolder(private val binding: BuyAgainItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(foodName: String, foodPrice: String, foodImage: String) {
            binding.buyAgainFoodName.text = foodName
            binding.buyAgainFoodPrice.text = foodPrice
            val uriString = foodImage
            val uri = Uri.parse(uriString)
            Glide.with(requireContext).load(uri).into(binding.buyAgainFoodImage)

            // Handle click event
            itemView.setOnClickListener {
                itemClickListener?.onItemClick(adapterPosition)
            }
        }
    }
}