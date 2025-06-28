package com.examples.licenta_food_ordering.Fragment

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.licenta_food_ordering.R

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val restaurantName = arguments?.getString("restaurantName") ?: "Unknown"
        val foodNames = arguments?.getStringArrayList("foodNames") ?: listOf("No items")
        val estimatedDeliveryTime = arguments?.getString("estimatedDeliveryTime") ?: "Unknown"

        val details = """
        Restaurant: $restaurantName
        Food Items:
        ${foodNames.joinToString("\n- ", prefix = "- ")}
        Estimated Delivery Time: $estimatedDeliveryTime
    """.trimIndent()
        // also add the distance using the DistanceUtils class


        view.findViewById<TextView>(R.id.notificationDetails).text = details
    }
}