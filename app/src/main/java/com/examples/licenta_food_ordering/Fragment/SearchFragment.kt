package com.examples.licenta_food_ordering.Fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.FragmentSearchBinding
import com.examples.licenta_food_ordering.Restaurant
import com.examples.licenta_food_ordering.RestaurantDetailsActivity
import com.examples.licenta_food_ordering.model.courier.Courier
import com.examples.licenta_food_ordering.model.courier.CourierStatus
import com.examples.licenta_food_ordering.service.courier.DistanceCalculationUtility
import com.examples.licenta_food_ordering.service.courier.FirebaseCourierService
import com.examples.licenta_food_ordering.service.courier.RouteManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.*
import kotlin.random.Random

class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsRef: DatabaseReference
    private lateinit var firebaseCourierService: FirebaseCourierService
    private val restaurantsList = mutableListOf<Restaurant>()
    private lateinit var binding: FragmentSearchBinding
    private val courierRoutes = mutableMapOf<String, List<LatLng>>()
    private val distanceCalculationUtility = DistanceCalculationUtility()

    // Coordonatele pentru centrul Timișoarei (Piața Victoriei)
    private val timisoaraCenter = LatLng(45.753256, 21.225543)

    // Raza de 500m
    private val hotspotRadius = 500f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Ensure a placeholder is always visible when there's no input
        binding.searchView.queryHint = "Enter the restaurant name"

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

        // Permite zoom cu trackpad-ul sau pinch-to-zoom
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = true
        mMap.uiSettings.isTiltGesturesEnabled = true

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } ?: run {
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        fetchRestaurants()  // Fetch restaurants and show them
        fetchCouriers()  // Fetch couriers and show them
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
                Toast.makeText(requireContext(), "Error fetching restaurants", Toast.LENGTH_SHORT).show()
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
                        val restaurantLocation = LatLng(courier.restaurantLatitude, courier.restaurantLongitude)
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
        val a = sin(dlat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2.0)
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

        val bitmap = Bitmap.createBitmap(bounds.width() + 30, bounds.height() + 30, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawText(text, (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), paint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap)
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}