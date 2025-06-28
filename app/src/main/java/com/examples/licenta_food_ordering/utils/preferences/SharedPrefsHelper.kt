package com.examples.licenta_food_ordering.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SharedPrefsHelper {

    private const val PREFS_NAME = "food_ordering_prefs"
    private const val KEY_ADMIN_USER_ID = "admin_user_id"
    private const val KEY_RESTAURANT_NAME = "restaurant_name"

    // New keys for user location
    private const val KEY_USER_LAT = "user_lat"
    private const val KEY_USER_LNG = "user_lng"

    // Save admin user ID
    fun saveAdminUserId(context: Context, adminUserId: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_ADMIN_USER_ID, adminUserId)
        editor.apply()
    }

    // Save restaurant names
    fun saveRestaurantNames(context: Context, restaurantName: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Get the existing list of restaurant names (or an empty list if none exists)
        val currentNamesJson = sharedPreferences.getString(KEY_RESTAURANT_NAME, null)
        val currentNames = if (currentNamesJson != null) {
            // Convert the JSON string to a list
            val type = object : TypeToken<MutableList<String>>() {}.type
            Gson().fromJson<MutableList<String>>(currentNamesJson, type)
        } else {
            mutableListOf()
        }

        // Add the new restaurant name to the list
        if (!currentNames.contains(restaurantName)) {
            currentNames.add(restaurantName)  // Add if not already in the list
        }

        // Save the updated list back to SharedPreferences
        val updatedNamesJson = Gson().toJson(currentNames)
        editor.putString(KEY_RESTAURANT_NAME, updatedNamesJson)
        editor.apply()
    }

    // Reset restaurant names
    fun resetRestaurantNames(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.remove(KEY_RESTAURANT_NAME)  // Remove the stored restaurant names
        editor.apply()
    }

    // Save user location
    fun saveUserLocation(context: Context, latitude: Double, longitude: Double) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat(KEY_USER_LAT, latitude.toFloat())  // Save latitude as Float
        editor.putFloat(KEY_USER_LNG, longitude.toFloat())  // Save longitude as Float
        editor.apply()
    }

    // Get user location
    fun getUserLocation(context: Context): Pair<Double, Double> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = sharedPreferences.getFloat(KEY_USER_LAT, 0f).toDouble()  // Get latitude
        val lng = sharedPreferences.getFloat(KEY_USER_LNG, 0f).toDouble()  // Get longitude
        return Pair(lat, lng)
    }

    // Get admin user ID
    fun getAdminUserId(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ADMIN_USER_ID, null)
    }

    // Get restaurant names
    fun getRestaurantNames(context: Context): List<String> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_RESTAURANT_NAME, null)

        return if (json != null) {
            // Convert JSON string back to a list of restaurant names
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(json, type)
        } else {
            // Return an empty list if no restaurant names are found
            emptyList()
        }
    }
}