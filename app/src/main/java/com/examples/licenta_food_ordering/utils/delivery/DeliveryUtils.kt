package com.examples.licenta_food_ordering.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.IOException

object DeliveryUtils {
    data class DistanceMatrixResponse(val rows: List<Row>)
    data class Row(val elements: List<Element>)
    data class Element(val duration: Duration)
    data class Duration(val text: String)

    // API key
    private const val API_KEY =
        "AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A"

    fun calculateDeliveryTime(
        userLat: Double,
        userLng: Double,
        restaurantLat: Double,
        restaurantLng: Double,
        callback: (String) -> Unit
    ) {
        val client = OkHttpClient()
        val origins = "$userLat,$userLng"
        val destinations = "$restaurantLat,$restaurantLng"
        val url =
            "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$origins&destinations=$destinations&key=$API_KEY"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Unavailable")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    val distanceMatrixResponse =
                        Gson().fromJson(jsonResponse, DistanceMatrixResponse::class.java)
                    val duration =
                        distanceMatrixResponse?.rows?.firstOrNull()?.elements?.firstOrNull()?.duration?.text
                    callback(duration ?: "Unavailable")
                } else {
                    callback("Unavailable")
                }
            }
        })
    }

    // Function to convert an address to coordinates using Google Maps Geocoding API
    fun getUserLocationCoordinates(callback: (Double, Double) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            callback(0.0, 0.0) // If no user is logged in, return (0.0, 0.0)
            return
        }

        // Reference to the "users" table in Firebase Realtime Database
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId)

        // Get user location from Firebase
        userRef.child("userLocation").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userLocation = task.result?.getValue(String::class.java)

                if (!userLocation.isNullOrEmpty()) {
                    // Call your existing method to get coordinates based on the user location
                    getCoordinatesFromAddress(userLocation, callback)
                } else {
                    callback(0.0, 0.0) // Return (0.0, 0.0) if location is not available
                }
            } else {
                callback(0.0, 0.0) // Return (0.0, 0.0) in case of an error
            }
        }
    }

    fun getCoordinatesFromAddress(
        address: String,
        callback: (Double, Double) -> Unit
    ) {
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "+")}&key=$API_KEY"

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(0.0, 0.0)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    val jsonObject = JSONObject(jsonResponse)
                    val results = jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")
                        callback(lat, lng)
                    } else {
                        callback(0.0, 0.0)
                    }
                } else {
                    callback(0.0, 0.0)
                }
            }
        })
    }
}