package com.examples.licenta_food_ordering.Fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsRef: DatabaseReference
    private val restaurantsList = mutableListOf<Restaurant>()
    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.searchView.queryHint = "Enter the restaurant name"

        database = FirebaseDatabase.getInstance()
        restaurantsRef = database.getReference("Restaurants")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupSearchView()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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
                        Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        fetchRestaurants()
    }

    private fun fetchRestaurants() {
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    restaurant?.let {
                        restaurantsList.add(it)
                    }
                }
                showRestaurantsOnMap(restaurantsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching restaurants",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun showRestaurantsOnMap(restaurants: List<Restaurant>) {
        mMap.clear()

        for (restaurant in restaurants) {
            val restaurantLocation = LatLng(restaurant.latitude, restaurant.longitude)

            Log.d("SearchFragment", "Adding marker for: ${restaurant.name}")

            val markerOptions = MarkerOptions()
                .position(restaurantLocation)
                .title(restaurant.name)

            val marker = mMap.addMarker(markerOptions)

            marker?.tag = restaurant.id

            val markerIcon = createCustomMarkerWithText(requireContext(), restaurant.name)

            mMap.addMarker(
                MarkerOptions()
                    .position(restaurantLocation)
                    .icon(markerIcon)
                    .title(restaurant.name)
            )
        }

        mMap.setOnMarkerClickListener { marker ->
            val restaurantId = marker.tag as? String
            if (restaurantId != null) {
                val intent = Intent(
                    requireContext(),
                    RestaurantDetailsActivity::class.java
                )
                intent.putExtra("RESTAURANT_ID", restaurantId)
                startActivity(intent)
            }
            true
        }
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

        //focus the map on that restaurant
        if (filteredRestaurants.size == 1) {
            val restaurant = filteredRestaurants.first()
            val restaurantLocation = LatLng(restaurant.latitude, restaurant.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(restaurantLocation, 15f))
        }
    }

    //marker with text
    fun createCustomMarkerWithText(context: Context, text: String): BitmapDescriptor {
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

    //permisiune la harta
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