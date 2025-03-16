package com.examples.licenta_food_ordering

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.licenta_food_ordering.R
import com.examples.licenta_food_ordering.model.OrderDetails

class OrderDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        // Set the action bar color to white programmatically
        supportActionBar?.setBackgroundDrawable(resources.getDrawable(R.color.white))

        // Enable the back button in the action bar (optional, if you need the system back button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Retrieve the OrderDetails object passed via Intent
        val orderDetails: OrderDetails? = intent.getParcelableExtra("orderDetails")

        // Check if orderDetails is not null and update the UI with the order information
        orderDetails?.let {
            findViewById<TextView>(R.id.tvUserName).text = it.userName
            findViewById<TextView>(R.id.tvUserLocation).text = it.userLocation
            findViewById<TextView>(R.id.tvRestaurantLocation).text = it.restaurantLocation
            findViewById<TextView>(R.id.tvFoodNames).text = it.foodNames?.joinToString(", ")
            findViewById<TextView>(R.id.tvTotalPrice).text = it.totalPrice
            findViewById<TextView>(R.id.tvPhoneNumber).text = it.phoneNumber
            findViewById<TextView>(R.id.tvOrderStatus).text = "Delivered: ${it.orderDelivered}"
            findViewById<TextView>(R.id.tvCurrentTime).text = "Order Time: ${it.orderTime}"
        }

        // Handle the back button click event
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()  // Go back to the previous activity
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Handle the system back button (optional if you need custom behavior for back press)
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}