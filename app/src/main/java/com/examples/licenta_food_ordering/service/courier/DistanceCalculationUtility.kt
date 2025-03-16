package com.examples.licenta_food_ordering.service.courier

import com.google.maps.GeoApiContext
import com.google.maps.DirectionsApi
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng as GoogleLatLng
import com.google.maps.model.TrafficModel
import com.google.maps.model.TravelMode
import org.joda.time.DateTime

class DistanceCalculationUtility {

    private val apiKey = "AIzaSyCHP4wYR_Qe0d1sasOGQ-vlmCncYK2F4KQ" // Replace with your Google API key

    private val context: GeoApiContext = GeoApiContext.Builder()
        .apiKey(apiKey)
        .build()

    // 🔹 Calculate real distance between two locations using Google Maps API
    fun getRealDistance(origin: String, destination: String): Double {
        val matrix = DirectionsApi.newRequest(context)
            .origin(origin)
            .destination(destination)
            .mode(TravelMode.DRIVING)  // Calculate for driving
            .trafficModel(TrafficModel.PESSIMISTIC)  // Use pessimistic traffic model
            .departureTime(DateTime.now())  // Set departure time to now
            .await() // Wait for the result

        // Return distance in kilometers
        return matrix.routes[0].legs[0].distance.inMeters / 1000.0
    }

    // 🔹 Get traffic estimation between the courier, restaurant, and user
    fun getTrafficEstimation(courier: String, restaurant: String, user: String): Double {
        // Get current time (to simulate traffic conditions)
        val departureTime = DateTime.now()

        val request = DirectionsApi.newRequest(context)
            .mode(TravelMode.DRIVING)  // Set travel mode to driving
            .origin(courier)  // Set courier as the origin
            .destination(user)  // Set user as the destination
            .departureTime(departureTime)  // Set departure time to now
            .trafficModel(TrafficModel.BEST_GUESS)  // Use traffic model for estimation

        // Execute the request
        val result: DirectionsResult = request.await()

        // Assuming there is at least one route available
        val route = result.routes[0]

        // Get the duration in traffic for the first leg (first part of the route)
        val leg = route.legs[0]
        val durationInTraffic = leg.durationInTraffic  // Time including traffic conditions

        // Return the traffic estimated duration in minutes
        return durationInTraffic.inSeconds.toDouble()
    }

    // 🔹 Select the minimum real distance between courier, restaurant, and user
    fun getMinimumRealDistance(courier: String, restaurant: String, user: String): Double {
        // Calculate distances considering traffic
        val d1 = getRealDistance(courier, restaurant)  // Courier → Restaurant
        val d2 = getRealDistance(restaurant, user)     // Restaurant → User
        val d3 = getRealDistance(courier, user)        // Courier → User (direct)

        val route1 = d1 + d2  // Classic route: Courier → Restaurant → User
        val route2 = d3       // Direct route: Courier → User (if shorter)

        return minOf(route1, route2)  // Return the shortest distance
    }

    // 🔹 Geocode an address to get its coordinates (latitude, longitude)
    fun getCoordinatesFromAddress(address: String): com.google.android.gms.maps.model.LatLng {
        try {
            val results = com.google.maps.GeocodingApi.geocode(context, address).await()
            if (results.isNotEmpty()) {
                val location: GoogleLatLng = results[0].geometry.location
                return com.google.android.gms.maps.model.LatLng(location.lat, location.lng)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return com.google.android.gms.maps.model.LatLng(0.0, 0.0) // Return invalid location in case of error
    }
}