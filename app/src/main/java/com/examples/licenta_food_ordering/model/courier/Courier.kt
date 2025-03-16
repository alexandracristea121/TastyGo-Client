package com.examples.licenta_food_ordering.model.courier

data class Courier(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val restaurantLatitude: Double = 0.0,
    val restaurantLongitude: Double = 0.0,
    val userLatitude: Double = 0.0,
    val userLongitude: Double = 0.0,
    val userUid: String = "",  // UID-ul utilizatorului care a comandat
    val status: CourierStatus = CourierStatus.AVAILABLE,  // Statusul curierului
    val lastUpdate: Long = System.currentTimeMillis(),  // Timpul ultimei actualizări
    var minDistance: Double = 0.0,
    var trafficEstimationInMinutes: Int = 0
)

enum class CourierStatus {
    AVAILABLE,   // Disponibil
    DELIVERING,  // În livrare
    OFFLINE      // Offline
}