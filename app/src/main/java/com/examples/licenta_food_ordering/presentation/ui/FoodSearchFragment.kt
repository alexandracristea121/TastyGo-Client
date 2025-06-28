package com.examples.licenta_food_ordering.presentation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.FragmentSearchBinding
import com.examples.licenta_food_ordering.presentation.activity.Restaurant
import com.examples.licenta_food_ordering.presentation.activity.RestaurantDetailsActivity
import com.examples.licenta_food_ordering.model.courier.Courier
import com.examples.licenta_food_ordering.model.courier.CourierStatus
import com.examples.licenta_food_ordering.service.courier.FirebaseCourierService
import com.examples.licenta_food_ordering.service.courier.RouteManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsRef: DatabaseReference
    private lateinit var firebaseCourierService: FirebaseCourierService
    private val restaurantsList = mutableListOf<Restaurant>()
    private lateinit var binding: FragmentSearchBinding
    private val courierRoutes = mutableMapOf<String, List<LatLng>>()
    private lateinit var placesClient: PlacesClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext(),
                "AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A"
            )  // Replace with your API key
        }

        placesClient = Places.createClient(requireContext())

        database = FirebaseDatabase.getInstance()
        restaurantsRef = database.getReference("Restaurants")
        firebaseCourierService = FirebaseCourierService()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupSearchView()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable zoom, scroll, rotate, and tilt gestures
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = true
        mMap.uiSettings.isTiltGesturesEnabled = true

        // Check for location permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are granted, enable location on the map
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } ?: run {
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // Request location permissions if not granted
            requestLocationPermissions()
        }

        // Fetch and display restaurants and couriers
        fetchFoodPlacesNearCurrentLocation()
        fetchRestaurants()  // Fetch restaurants from Firebase
        fetchCouriers()  // Fetch couriers from Firebase
    }
    // In your fragment or activity
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
//    AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A

    data class PlaceInfo(
        val name: String,
        val lat: Double,
        val lng: Double
    )

    private fun fetchFoodPlacesNearCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val geocoder = Geocoder(requireContext())
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)

                    if (addresses != null && addresses.isNotEmpty()) {
                        val cityName = addresses[0].locality
                        val apiKey = "AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A" // Secure your API key

                        val query = "restaurants OR fast food OR cafes OR bakeries OR food places OR dining OR takeout in $cityName"

                        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                                "?query=${Uri.encode(query)}" +
                                "&key=$apiKey"

                        // Launch coroutine to fetch places in parallel
                        fetchPaginatedPlaces(url)
                    }
                } ?: run {
                    Log.e("fetchFoodPlaces", "Location is null")
                    Toast.makeText(requireContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Log.e("fetchFoodPlaces", "Failed to get last location: ${it.message}")
                Toast.makeText(requireContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun fetchPaginatedPlaces(url: String) {
        val request = StringRequest(com.android.volley.Request.Method.GET, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                val results = jsonResponse.getJSONArray("results")

                // Collecting the place IDs
                val placeIds = mutableListOf<String>()
                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val placeId = place.getString("place_id")
                    placeIds.add(placeId)
                    val lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                    val lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                    val name = place.getString("name")

                    // Add custom marker
                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )

                    // Store the place info for later use
                    val placeInfo = PlaceInfo(name, lat, lng)
                    marker?.tag = placeInfo
                }

                // Parallel fetching place details
                GlobalScope.launch(Dispatchers.IO) {
                    val deferreds = placeIds.map { placeId ->
                        async {
                            fetchPlaceDetailsAsync(placeId)
                        }
                    }
                    val placeDetails = deferreds.awaitAll()
                    placeDetails.forEach { details ->
                        // Update markers with the details
                        details?.let {
                            // Loop through the markers and update their snippet
                        }
                    }
                }

                // Handle pagination to fetch more results
                val nextPageToken = jsonResponse.optString("next_page_token", null)
                if (!nextPageToken.isNullOrEmpty()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val nextPageUrl = "$url&pagetoken=$nextPageToken"
                        fetchPaginatedPlaces(nextPageUrl)
                    }, 5000)
                }
            } catch (e: JSONException) {
                Log.e("fetchFoodPlaces", "Error parsing JSON: ${e.message}")
            }
        }, { error ->
            Log.e("fetchFoodPlaces", "Error fetching food places: ${error.message}")
        })

        val requestQueue = Volley.newRequestQueue(requireContext())
        requestQueue.add(request)
    }

    private suspend fun fetchPlaceDetailsAsync(placeId: String): Map<String, String>? {
        val apiKey = "AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A"
        val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?placeid=$placeId" +
                "&key=$apiKey"

        return withContext(Dispatchers.IO) {
            try {
                val result = StringRequest(com.android.volley.Request.Method.GET, url, { response ->
                    val jsonResponse = JSONObject(response)
                    val resultObj = jsonResponse.getJSONObject("result")
                    val phone = resultObj.optString("formatted_phone_number", "Not available")
                    val website = resultObj.optString("website", "Not available")
                    val openingHours = resultObj.optJSONObject("opening_hours")
                    val openHours = openingHours?.optJSONArray("weekday_text")?.let {
                        val hoursList = mutableListOf<String>()
                        for (i in 0 until it.length()) {
                            hoursList.add(it.getString(i))
                        }
                        hoursList.joinToString(", ")
                    } ?: "Not available"
                    mapOf("phone" to phone, "website" to website, "open_hours" to openHours)
                }, { error ->
                    Log.e("fetchPlaceDetails", "Error fetching place details: ${error.message}")
                })

                // Perform request and return result
                val requestQueue = Volley.newRequestQueue(requireContext())
                requestQueue.add(result)
                null // This will eventually be replaced with the actual details
            } catch (e: Exception) {
                Log.e("fetchPlaceDetails", "Error fetching place details asynchronously: ${e.message}")
                null
            }
        }
    }

    private fun showRestaurantDetails(info: PlaceInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(info.name)
            .setMessage("Location: ${info.lat}, ${info.lng}")
            .setPositiveButton("OK", null)
            .show()
    }
    private fun fetchRestaurants() {
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                restaurantsList.clear()
                for (restaurantSnapshot in snapshot.children) {
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    restaurant?.let {
                        restaurantsList.add(it)
                    }
                }
                showRestaurantsOnMap(restaurantsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching restaurants", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun showRestaurantsOnMap(restaurants: List<Restaurant>) {
        // Clear existing markers
        mMap.clear()

        for (restaurant in restaurants) {
            val restaurantLocation = LatLng(restaurant.latitude, restaurant.longitude)

            Log.d("SearchFragment", "Adding marker for: ${restaurant.name}")

            val markerOptions = MarkerOptions()
                .position(restaurantLocation)
                .title(restaurant.name)

            // Add the restaurant marker
            val marker = mMap.addMarker(markerOptions)

            // Set the restaurant ID as the marker's tag
            marker?.tag = "restaurant_${restaurant.id}"

            val markerIcon = createCustomMarkerWithText(requireContext(), restaurant.name)

            mMap.addMarker(
                MarkerOptions()
                    .position(restaurantLocation)
                    .icon(markerIcon)
                    .title(restaurant.name)
            )
        }

        // Set listener for clicking markers on the map
        mMap.setOnMarkerClickListener { marker ->
            // Check if the marker is a restaurant
            if (marker.tag != null && marker.tag.toString().startsWith("restaurant_")) {
                val restaurantId = marker.tag.toString().removePrefix("restaurant_")
                // Open the RestaurantDetailsActivity
                val intent = Intent(requireContext(), RestaurantDetailsActivity::class.java)
                intent.putExtra("RESTAURANT_ID", restaurantId)
                startActivity(intent)
                true  // Return true to indicate that we have handled the click
            } else {
                // Handle courier marker click (no need to open restaurant details)
                val courierId = marker.tag as? String
                if (courierId != null) {
                    // If it's a courier, toggle the route on the map
                    val route = courierRoutes[courierId]
                    if (route != null) {
                        Log.d("SearchFragment", "Drawing route for courier $courierId")
                        RouteManager.toggleRoute(requireContext(), mMap, route)
                    }
                }
                false  // Return false to allow the default behavior (for example, opening the info window)
            }
        }
    }

    private fun fetchCouriers() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        // Get the couriers from Firebase
        val courierRef = FirebaseDatabase.getInstance().getReference("couriers")
        courierRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (courierSnapshot in snapshot.children) {
                    val courierId = courierSnapshot.key ?: "Unknown ID"
                    val courier = courierSnapshot.getValue(Courier::class.java)

                    courier?.let {
                        val courierLocation = LatLng(courier.latitude, courier.longitude)
                        val courierStatus = courier.status
                        val restaurantLocation =
                            LatLng(courier.restaurantLatitude, courier.restaurantLongitude)
                        val userLocation = LatLng(courier.userLatitude, courier.userLongitude)

                        // Add a marker for the courier
                        val courierIcon = when (courierStatus) {
                            CourierStatus.DELIVERING -> {
                                BitmapDescriptorFactory.fromResource(R.drawable.food_delivery_unavailable)
                            }

                            else -> {
                                BitmapDescriptorFactory.fromResource(R.drawable.food_delivery)
                            }
                        }

                        // Add the courier marker with a unique tag
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(courierLocation)
                                .title("Courier ID: $courierId")
                                .icon(courierIcon)
                        )

                        // Set the courier ID as the marker's tag
                        marker?.tag = courierId
                        courierRoutes[courierId] =
                            listOf(courierLocation, restaurantLocation, userLocation)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SearchFragment", "Error fetching couriers: ${error.message}")
            }
        })
    }

    private fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Float {
        val lat1 = Math.toRadians(startLatLng.latitude)
        val lon1 = Math.toRadians(startLatLng.longitude)
        val lat2 = Math.toRadians(endLatLng.latitude)
        val lon2 = Math.toRadians(endLatLng.longitude)

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1
        val a = sin(dlat / 2).pow(2.0) + (cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2.0))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val radius = 6371.0
        return (radius * c).toFloat() * 1000
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchRestaurantByName(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    searchRestaurantByName(it)
                }
                return true
            }
        })
    }

    private fun searchRestaurantByName(query: String) {
        val filteredRestaurants = restaurantsList.filter {
            it.name.contains(query, ignoreCase = true)
        }
        showRestaurantsOnMap(filteredRestaurants)

        if (filteredRestaurants.size == 1) {
            val restaurant = filteredRestaurants.first()
            val restaurantLocation = LatLng(restaurant.latitude, restaurant.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantLocation, 15f))
        }
    }

    private fun createCustomMarkerWithText(context: Context, text: String): BitmapDescriptor {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val bitmap =
            Bitmap.createBitmap(bounds.width() + 30, bounds.height() + 30, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawText(text, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Request permissions if not granted
    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permissions are not granted, request them
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location access
                Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_SHORT).show()
                mMap.isMyLocationEnabled = true
                fetchFoodPlacesNearCurrentLocation()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(
                    requireContext(),
                    "Permission to access location denied. You cannot fetch restaurants.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}