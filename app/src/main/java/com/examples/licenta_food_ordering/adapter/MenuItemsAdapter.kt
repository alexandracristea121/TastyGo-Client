package com.examples.licenta_food_ordering.adaptar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.licenta_food_ordering.databinding.MenuItemBinding
import com.examples.licenta_food_ordering.presentation.activity.DetailsActivity
import com.examples.licenta_food_ordering.model.MenuItem

class MenuItemsAdapter(private var menuItems: List<MenuItem>, private val requireContext: Context) :
    RecyclerView.Adapter<MenuItemsAdapter.MenuViewHolder>() {

    // Method to submit a new list to update RecyclerView
    fun submitList(items: List<MenuItem>) {
        menuItems = items
        notifyDataSetChanged()  // Notify the adapter that the data has changed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuItems.size

    inner class MenuViewHolder(private val binding: MenuItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    openDetailsActivity(position)
                }
            }
        }

        private fun openDetailsActivity(position: Int) {
            val menuItem = menuItems[position]

            val intent = Intent(requireContext, DetailsActivity::class.java).apply {
                putExtra("MenuItemName", menuItem.foodName)
                putExtra("MenuItemImage", menuItem.foodImage)
                putExtra("MenuItemDescription", menuItem.foodDescription)
                putExtra("MenuItemIngredients", menuItem.foodIngredient)
                putExtra("MenuItemPrice", menuItem.foodPrice)
            }

            requireContext.startActivity(intent)
        }

        fun bind(position: Int) {
            val menuItem = menuItems[position]
            binding.apply {
                menuFoodName.text = menuItem.foodName ?: ""
                menuPrice.text = menuItem.foodPrice ?: ""

                // Handle section headers (e.g., "-- Drinks --")
                val isSectionHeader = menuItem.foodName?.startsWith("--") == true

                if (isSectionHeader) {
                    menuPrice.visibility = View.GONE
                    menuImage.visibility = View.GONE
                } else {
                    menuPrice.visibility = View.VISIBLE
                    menuImage.visibility = View.VISIBLE

                    if (!menuItem.foodImage.isNullOrEmpty()) {
                        val uri = Uri.parse(menuItem.foodImage) // This works if the foodImage is a valid URL
                        Glide.with(requireContext).load(uri).into(menuImage)
                    } else {
                        // Optional: show placeholder or clear image view
                        menuImage.setImageResource(android.R.color.transparent)
                    }
                }
            }
        }
    }
}