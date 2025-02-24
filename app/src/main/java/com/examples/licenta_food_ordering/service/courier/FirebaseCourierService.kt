package com.examples.licenta_food_ordering.service.courier

import android.util.Log
import com.examples.licenta_food_ordering.model.courier.CourierStatus
import com.examples.licenta_food_ordering.model.courier.Hotspot
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class FirebaseCourierService {
    private val database = FirebaseDatabase.getInstance().reference.child("couriers")

    // Coordonatele centrale ale Timișoarei
    private val centerLat = 45.7489
    private val centerLng = 21.2087

    // Raza de 1 km
    private val radiusInMeters = 1000.0

    fun updateCourierLocation(courierId: String, location: LatLng) {
        val locationMap = mapOf("latitude" to location.latitude, "longitude" to location.longitude)
        database.child(courierId).setValue(locationMap)
    }

    fun getCouriersLocations(callback: (List<LatLng>) -> Unit) {
        val couriersRef = FirebaseDatabase.getInstance().getReference("couriers")

        couriersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = mutableListOf<LatLng>()

                // Iterăm prin fiecare curier din baza de date
                for (courierSnapshot in snapshot.children) {
                    val lat = courierSnapshot.child("latitude").getValue(Double::class.java)
                    val lng = courierSnapshot.child("longitude").getValue(Double::class.java)

                    if (lat != null && lng != null) {
                        val courierLocation = LatLng(lat, lng)
                        locations.add(courierLocation)
                        Log.d("Firebase", "Curier găsit: ${courierSnapshot.key} - Lat: $lat, Lng: $lng")
                    } else {
                        Log.e("Firebase", "Locație invalidă pentru curier: ${courierSnapshot.key}")
                    }
                }

                // Trimit locațiile prin callback
                callback(locations)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching couriers: ${error.message}")
            }
        })
    }

    // Generăm locația aleatorie în jurul centrului Timișoarei
    fun generateRandomLocationInsideHotspot(): LatLng {
        // Raza aleatorie în metri
        val randomRadius = Random.nextDouble(0.0, radiusInMeters)
        val randomAngle = Random.nextDouble(0.0, 2 * Math.PI) // Unghi aleator pentru distribuirea uniformă pe cerc

        // Calculăm offset-ul pe latitudine și longitudine
        val offsetLat = randomRadius * Math.cos(randomAngle)
        val offsetLng = randomRadius * Math.sin(randomAngle)

        // Calculăm noile coordonate în jurul centrului Timișoarei
        val newLat = centerLat + (offsetLat / 111.32)  // 1 grad de latitudine ≈ 111.32 km
        val newLng = centerLng + (offsetLng / (111.32 * Math.cos(Math.toRadians(centerLat))))  // Calculul pentru longitudine

        return LatLng(newLat, newLng)
    }

    // Generăm și salvăm curierii în Firebase
    fun generateAndSaveCouriers(numberOfCouriers: Int) {
        val courierRef = FirebaseDatabase.getInstance().getReference("couriers")

        for (i in 1..numberOfCouriers) {
            // Generăm locația aleatorie pentru fiecare curier
            val randomLocation = generateRandomLocationInsideHotspot()

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
}