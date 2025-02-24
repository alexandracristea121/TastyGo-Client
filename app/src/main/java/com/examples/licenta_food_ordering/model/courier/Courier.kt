package com.examples.licenta_food_ordering.model.courier

data class Courier(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: CourierStatus = CourierStatus.AVAILABLE,  // Statusul curierului
    val lastUpdate: Long = System.currentTimeMillis()  // Timpul ultimei actualizări
)

enum class CourierStatus {
    AVAILABLE,   // Disponibil
    DELIVERING,  // În livrare
    OFFLINE      // Offline
}