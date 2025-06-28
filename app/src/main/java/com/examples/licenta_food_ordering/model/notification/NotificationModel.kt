package com.examples.licenta_food_ordering.model

data class NotificationModel(
    val orderId: String = "",
    val message: String = "",
    val restaurantName: String = "",
    val foodNames: List<String> = emptyList(),
    val estimatedDeliveryTime: String = "",
)