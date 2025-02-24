package com.examples.licenta_food_ordering.Fragment

import android.Manifest
import android.content.Context
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
import com.examples.licenta_food_ordering.model.courier.CourierStatus
import com.examples.licenta_food_ordering.service.courier.FirebaseCourierService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
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

    // Coordonatele pentru centrul Timișoarei (Piața Victoriei)
    private val timisoaraCenter = LatLng(45.753256, 21.225543)
    // Raza de 500m
    private val hotspotRadius = 500f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

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
        mMap.uiSettings.isZoomGesturesEnabled = true  // Permite zoom cu pinch-to-zoom (trackpad sau gesturi tactile)

        // Permite scroll cu mouse-ul sau trackpad-ul
        mMap.uiSettings.isScrollGesturesEnabled = true  // Permite scroll cu trackpad

        // Permite rotirea hărții cu trackpad-ul (dacă e necesar)
        mMap.uiSettings.isRotateGesturesEnabled = true

        // Permite tilt (înclinarea) hărții cu trackpad-ul
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

        // Funcția de generare și salvare a curierilor
        generateAndSaveCouriers(timisoaraCenter, hotspotRadius, 10)
//        fetchRestaurants()  // Rămâne activă
        fetchCouriers()  // Rămâne activă
    }

    private fun fetchRestaurants() {
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                restaurantsList.clear() // Resetează lista de restaurante pentru a evita duplicatele
                for (restaurantSnapshot in snapshot.children) {
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    restaurant?.let {
                        restaurantsList.add(it)
                    }
                }
                showRestaurantsOnMap(restaurantsList) // Arată restaurantele pe hartă
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
        // Înlăturăm doar markerii restaurantelor
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
    }

    private fun fetchCouriers() {
        firebaseCourierService.getCouriersLocations { locations ->
            Log.d("SearchFragment", "Fetched couriers locations: $locations")
            for (loc in locations) {
                val courierLocation = LatLng(loc.latitude, loc.longitude)

                // Verificăm distanța
                val distance = calculateDistance(timisoaraCenter, courierLocation)
                Log.d("SearchFragment", "Courier distance: $distance meters")

                if (distance <= hotspotRadius) {
                    Log.d("SearchFragment", "Adding marker for courier at: $courierLocation")

                    // Folosim un marker simplu pentru test
                    mMap.addMarker(
                        MarkerOptions()
                            .position(courierLocation)
                            .title("Curier")
                    )
                } else {
                    Log.d("SearchFragment", "Courier at $courierLocation is  outside the hotspot.")
                }
            }
        }
    }

    // Functie pentru calculul distantei intre 2 coordonate
    private fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Float {
        val lat1 = Math.toRadians(startLatLng.latitude)
        val lon1 = Math.toRadians(startLatLng.longitude)
        val lat2 = Math.toRadians(endLatLng.latitude)
        val lon2 = Math.toRadians(endLatLng.longitude)

        // Formula Haversine
        val dlat = lat2 - lat1
        val dlon = lon2 - lon1
        val a = sin(dlat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Raza Pamantului in km
        val radius = 6371.0

        // Distanța în kilometri
        return (radius * c).toFloat() * 1000  // Convertim la metri
    }

    private fun generateRandomLocationInsideHotspot(center: LatLng, radius: Float): LatLng {
        // Generăm o distanță aleatorie în interiorul cercului (în metri)
        val randomRadius = Random.nextDouble(0.0, radius.toDouble())  // Raza în metri

        // Generăm un unghi aleatoriu pentru distribuirea uniformă pe cerc
        val randomAngle = Random.nextDouble(0.0, 2 * Math.PI)

        // Calculăm offset-ul pe latitudine și longitudine
        val offsetLat = randomRadius * Math.cos(randomAngle) / 111320  // 1 grad latitudine ≈ 111.32 km * 1000 (pentru metri)
        val offsetLng = randomRadius * Math.sin(randomAngle) / (111320 * Math.cos(Math.toRadians(center.latitude)))  // 1 grad longitudine ≈ 111.32 km * 1000 pentru metri

        // Calculăm noile coordonate în jurul centrului
        val newLat = center.latitude + offsetLat
        val newLng = center.longitude + offsetLng

        return LatLng(newLat, newLng)
    }

    private fun generateAndSaveCouriers(center: LatLng, radius: Float, numberOfCouriers: Int) {
        val courierRef = FirebaseDatabase.getInstance().getReference("couriers")

        for (i in 1..numberOfCouriers) {
            // Generăm locația aleatorie pentru fiecare curier în zona specificată
            val randomLocation = generateRandomLocationInsideHotspot(center, radius)

            // Creăm un ID unic pentru curier
            val courierId = "courier_$i"

            // Salvăm locația curierului în Firebase
            val courierData = mapOf(
                "latitude" to randomLocation.latitude,
                "longitude" to randomLocation.longitude,
                "status" to CourierStatus.AVAILABLE.name,  // Statusul curierului poate fi disponibil
                "lastUpdate" to System.currentTimeMillis()
            )

            courierRef.child(courierId).setValue(courierData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "Curierul $courierId a fost salvat cu succes.")
                } else {
                    Log.e("Firebase", "Eroare la salvarea curierului $courierId.")
                }
            }
        }
    }

    private fun createCourierMarkerWithEmoji(context: Context): BitmapDescriptor {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#FF7043") // Portocaliu
        paint.textSize = 80f // Mărimea textului
        paint.textAlign = Paint.Align.CENTER

        val bounds = Rect()
        paint.getTextBounds("🚴", 0, 1, bounds)

        // Creăm bitmap-ul pentru marker
        val bitmap = Bitmap.createBitmap(bounds.width() + 40, bounds.height() + 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Desenăm emoji-ul pe canvas
        canvas.drawText("🚴", (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), paint)

        // Returnăm bitmap-ul ca descriptor de marker
        return BitmapDescriptorFactory.fromBitmap(bitmap)
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