package com.example.licenta_food_ordering

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.licenta_food_ordering.databinding.ActivityRecentOrderItemsBinding
import com.examples.licenta_food_ordering.adaptar.RecentBuyAdapter
import com.examples.licenta_food_ordering.model.OrderDetails

class RecentOrderItems : AppCompatActivity() {

    private  val  binding : ActivityRecentOrderItemsBinding by lazy {
        ActivityRecentOrderItemsBinding.inflate(layoutInflater)
    }
    private lateinit var allFoodNames:ArrayList<String>
    private lateinit var allFoodImages:ArrayList<String>
    private lateinit var allFoodPrices:ArrayList<String>
    private lateinit var allFoodQuantities:ArrayList<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        //lista de ob order details prin cheia ""
        val recentOrderItems=intent.getSerializableExtra("RecentBuyOrderItem") as ArrayList<OrderDetails>
        recentOrderItems?.let{ orderDetails ->
            if(orderDetails.isNotEmpty()){
                val recentOrderItem=orderDetails[0]
                //populare liste cu datele comenzii
                allFoodNames=recentOrderItem.foodNames as ArrayList<String>
                allFoodImages=recentOrderItem.foodImages as ArrayList<String>
                allFoodPrices=recentOrderItem.foodPrices as ArrayList<String>
                allFoodQuantities=recentOrderItem.foodQuantities as ArrayList<Int>
            }
        }
        setAdapter()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setAdapter() {
        val rv=binding.recyclerViewRecentBuy
        rv.layoutManager=LinearLayoutManager(this) //afiseaza elem in lista
        val adapter=RecentBuyAdapter(this, allFoodNames, allFoodImages, allFoodPrices, allFoodQuantities)
        rv.adapter=adapter
    }
}