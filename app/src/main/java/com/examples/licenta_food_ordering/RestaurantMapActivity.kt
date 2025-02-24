package com.examples.licenta_food_ordering

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.licenta_food_ordering.R
import com.examples.licenta_food_ordering.model.MenuItem
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.database.*

class RestaurantMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_map)

        // Set the status bar color to white
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        // Use WindowInsetsController for modern status bar styling
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        restaurantsRef = database.getReference("Restaurants")

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if Google Play services is available
        if (isGooglePlayServicesAvailable()) {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.map, mapFragment)
                .commit()

            mapFragment.getMapAsync(this)
        } else {
            Toast.makeText(this, "Google Play Services not available.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check if the app has permission to access location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Enable the My Location layer
            mMap.isMyLocationEnabled = true

            // Get the last known location of the device
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // If a location is found, move the camera to that location
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    } ?: run {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // If permission is not granted, request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        // Fetch restaurant data from Firebase and add markers
        fetchRestaurants()
    }

    private fun fetchRestaurants() {
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    if (restaurant != null) {
                        val restaurantLocation = LatLng(restaurant.latitude, restaurant.longitude)
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(restaurantLocation)
                                .title(restaurant.name)
                        )

                        // Set a tag on the marker (restaurant ID) to identify it later
                        marker?.tag = restaurant.id
                    }
                }

                // Set up a listener for when a marker is clicked
                mMap.setOnMarkerClickListener { marker ->
                    val restaurantId = marker.tag as? String
                    if (restaurantId != null) {
                        val intent = Intent(
                            this@RestaurantMapActivity,
                            RestaurantDetailsActivity::class.java
                        )
                        intent.putExtra("RESTAURANT_ID", restaurantId)
                        startActivity(intent)
                    }
                    true // Indicate that we've handled the click
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@RestaurantMapActivity,
                    "Error fetching restaurants",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
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
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val menu: Map<String, MenuItem> = mapOf(),
    val categories: List<String> = listOf(),  // Default empty list
    val adminUserId: String = ""
)