package com.examples.licenta_food_ordering.Fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.licenta_food_ordering.RecentOrderItems
import com.example.licenta_food_ordering.databinding.FragmentHistoryBinding
import com.examples.licenta_food_ordering.OrderDetailsActivity
import com.examples.licenta_food_ordering.model.OrderDetails
import com.examples.licenta_food_ordering.adaptar.BuyAgainAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private lateinit var binding: FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var listOfOrderItem: ArrayList<OrderDetails> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(layoutInflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        retrievePendingOrders()  // Retrieve pending orders based on the condition
        retrieveCompletedOrders()  // Retrieve completed orders based on the condition

        binding.recentbuyitem.setOnClickListener {
            seeItemsRecentBuy()
        }

        binding.receivedButton.setOnClickListener {
            updateOrderStatus()
        }

        return binding.root
    }

    private fun updateOrderStatus() {
        val itemPushKey = listOfOrderItem.getOrNull(0)?.itemPushkey
        itemPushKey?.let {
            val completeOrderReference = database.reference.child("CompletedOrder").child(it)
            completeOrderReference.child("paymentReceived").setValue(true)
        }
    }

    private fun seeItemsRecentBuy() {
        listOfOrderItem.firstOrNull()?.let { recentBuy ->
            val intent = Intent(requireContext(), RecentOrderItems::class.java)
            intent.putExtra("RecentBuyOrderItem", listOfOrderItem)
            startActivity(intent)
        }
    }

    private fun retrievePendingOrders() {
        binding.recentbuyitem.visibility = View.INVISIBLE  // Hide UI initially
        userId = auth.currentUser?.uid ?: ""
        val ordersReference: DatabaseReference = database.reference.child("orders")

        // Query to get orders where orderAccepted = true
        val pendingOrderQuery = ordersReference
            .orderByChild("orderAccepted")   // Order by orderAccepted
            .equalTo(true)  // Filter orders where orderAccepted = true

        // Add listener to fetch orders
        pendingOrderQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ordersList = mutableListOf<OrderDetails>()
                var lastOrderItem: OrderDetails? = null  // To store the last pending order

                // Iterate through the orders to filter both orderAccepted = true and orderDelivered = false
                for (orderSnapshot in snapshot.children) {
                    val orderItem = orderSnapshot.getValue(OrderDetails::class.java)

                    // Only consider orders where orderAccepted = true and orderDelivered = false
                    if (orderItem?.orderAccepted == true && orderItem.orderDelivered == false) {
                        ordersList.add(orderItem)  // Add valid pending orders to the list
                    }
                }

                // Sort orders by orderTime to get the most recent one first
                ordersList.sortByDescending { it.orderTime }

                // The first item in the sorted list will be the most recent order
                lastOrderItem = ordersList.firstOrNull()

                // Check if a pending order was found and update the UI
                if (lastOrderItem != null) {
                    listOfOrderItem.clear()  // Clear previous list of orders
                    listOfOrderItem.add(lastOrderItem)  // Add only the last pending order

                    binding.recentbuyitem.visibility = View.VISIBLE  // Make it visible
                    setDataInRecentBuyItem()  // Show the details of the last order
                } else {
                    binding.recentbuyitem.visibility = View.GONE  // Hide if no pending orders
                }

                setPreviousBuyItemsRecyclerView()  // Optional: Update the RecyclerView if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors in retrieving data
            }
        })
    }
    private fun retrieveCompletedOrders() {
        binding.recentbuyitem.visibility = View.INVISIBLE  // Hide UI initially
        userId = auth.currentUser?.uid ?: ""
        val ordersReference: DatabaseReference = database.reference.child("orders")

        // Query to get orders where orderAccepted = true (we'll filter orderDelivered = true later)
        val completedOrderQuery = ordersReference
            .orderByChild("orderAccepted")
            .equalTo(true)  // only retrieve accepted orders

        completedOrderQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var completedOrderItem: OrderDetails? = null  // To store the last completed order

                // Iterate through the orders to find the one where both orderAccepted = true and orderDelivered = true
                for (orderSnapshot in snapshot.children) {
                    val orderItem = orderSnapshot.getValue(OrderDetails::class.java)

                    if (orderItem?.orderAccepted == true && orderItem.orderDelivered == true) {
                        completedOrderItem = orderItem  // Store the order if both conditions are true
                        break  // We only need the first found completed order
                    }
                }

                // Check if a completed order was found and update the UI
                if (completedOrderItem != null) {
                    listOfOrderItem.clear()  // Clear previous list of orders
                    listOfOrderItem.add(completedOrderItem)  // Add only the completed order

                    binding.recentbuyitem.visibility = View.VISIBLE  // Make it visible
                    setDataInRecentBuyItem()  // Show the details of the completed order
                } else {
                    binding.recentbuyitem.visibility = View.GONE  // Hide if no completed orders
                }

                setPreviousBuyItemsRecyclerView()  // Optional: Update the RecyclerView if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors in retrieving data
            }
        })
    }

    private fun setDataInRecentBuyItem() {
        binding.recentbuyitem.visibility = View.VISIBLE
        val recentOrderItem = listOfOrderItem.firstOrNull()  // Get the most recent order

        recentOrderItem?.let {
            with(binding) {
                // Set the food name
                buyAgainFoodName.text = it.foodNames?.firstOrNull() ?: ""

                // Set the food price (which is the total price)
                buyAgainFoodPrice.text = it.totalPrice ?: ""  // Set the total price from the order

                // Set the food image using Glide
                val image = it.foodImages?.firstOrNull() ?: ""
                val uri = Uri.parse(image)
                Glide.with(requireContext()).load(uri).into(buyAgainFoodImage)

                // Set the restaurant name
                buyAgainRestaurantName.text = it.restaurantName  // Set the restaurant name

                // Set the order status
                val isOrderAccepted = it.orderAccepted
                if (isOrderAccepted) {
                    orderedStatus.background.setTint(Color.GREEN)  // Green for accepted orders
                } else {
                    orderedStatus.background.setTint(Color.parseColor("#FFA500"))  // Orange for pending orders
                }
            }
        }
    }

    private fun setPreviousBuyItemsRecyclerView() {
        val buyAgainFoodName = mutableListOf<String>()
        val buyAgainFoodPrice = mutableListOf<String>()
        val buyAgainFoodImage = mutableListOf<String>()
        for (i in listOfOrderItem.indices) {
            listOfOrderItem[i].foodNames?.firstOrNull()?.let {
                buyAgainFoodName.add(it)
                listOfOrderItem[i].foodPrices?.firstOrNull()?.let {
                    buyAgainFoodPrice.add(it)
                    listOfOrderItem[i].foodImages?.firstOrNull()?.let {
                        buyAgainFoodImage.add(it)
                    }
                }
            }
        }
        val rv = binding.BuyAgainRecyclerView
        rv.layoutManager = LinearLayoutManager(requireContext())
        buyAgainAdapter = BuyAgainAdapter(
            buyAgainFoodName,
            buyAgainFoodPrice,
            buyAgainFoodImage,
            requireContext()
        )
        rv.adapter = buyAgainAdapter

        // Set item click listener for navigating to OrderDetailsActivity
        buyAgainAdapter.setOnItemClickListener(object : BuyAgainAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val orderDetails = listOfOrderItem[position]
                val intent = Intent(requireContext(), OrderDetailsActivity::class.java)
                intent.putExtra("orderDetails", orderDetails)
                startActivity(intent)
            }
        })
    }
}