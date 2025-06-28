package com.examples.licenta_food_ordering.Fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.FragmentHomeBinding
import com.examples.licenta_food_ordering.Restaurant
import com.examples.licenta_food_ordering.adaptar.PopularAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.examples.licenta_food_ordering.AIChatbotActivity
import com.examples.licenta_food_ordering.adaptar.MenuAdapter
import com.examples.licenta_food_ordering.adaptar.SuggestedFoodAdapter
import com.examples.licenta_food_ordering.model.MenuItem
import com.examples.licenta_food_ordering.model.NotificationModel
import com.examples.licenta_food_ordering.model.SuggestedFood
import com.examples.licenta_food_ordering.utils.DeliveryUtils
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.*
import java.text.SimpleDateFormat

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var restaurantsList: MutableList<Restaurant>
    private lateinit var foodRecyclerView: RecyclerView
    private lateinit var menuAdapter: MenuAdapter
    private var menuItems: List<MenuItem> = listOf()
    private val apiKey =
        "AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A"  // Replace with your Google API key
    private lateinit var suggestedFoodAdapter: SuggestedFoodAdapter
    private var suggestedFoodList = mutableListOf<SuggestedFood>()
    private lateinit var chatbotIcon: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Add restaurants logos in firebase:
//        fetchLogosForRestaurants("timisoara")

        // TODO: getCurrentLocationAndFetchRestaurants -> implement to fetch the restaurants only if there are differences from the db
//    getCurrentLocationAndFetchRestaurants()
        retrieveAndDisplayAllRestaurantsFromDatabase()

//        convertAllRestaurantAddressesToLatLong(requireContext())
//        updateAdminUserIds()

        return binding.root
    }

    private fun deleteAllRestaurants() {
        val database = FirebaseDatabase.getInstance()
        val restaurantsRef = database.getReference("Restaurants")

        // Delete all restaurants
        restaurantsRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                    requireContext(),
                    "All restaurants deleted successfully.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Error deleting restaurants: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun getCurrentLocationAndFetchRestaurants() {
        // Initialize FusedLocationProviderClient
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        // Check if permission is granted to access location
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // If permission is not granted, request permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Get the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                // Use Geocoder to get the city name from the coordinates
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                val city = addresses?.get(0)?.locality

                if (city != null) {
                    // Normalize the city name by removing diacritical marks
                    val normalizedCity = normalizeCityName(city)

                    // Fetch restaurants from Tazz using the normalized city name
//                    fetchRestaurantsAddressFromTazz(city)
//                    fetchRestaurantNamesFromTazz(normalizedCity)
                    city?.let { detectedCity ->
                        fetchMenusFromTazz(detectedCity)
                    } ?: run {
                        Toast.makeText(
                            requireContext(),
                            "Could not detect your city",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "City not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Unable to retrieve location", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Handle the permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now fetch the location
                getCurrentLocationAndFetchRestaurants()
            } else {
                // Permission denied
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to fetch restaurants.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Define the constant for the permission request code
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun fetchRestaurantNamesFromTazz(city: String) {
        // Normalize the city name to remove diacritics
//        val normalizedCity = city
//            .lowercase()
//            .replace("ă", "a")
//            .replace("â", "a")
//            .replace("î", "i")
//            .replace("ș", "s")
//            .replace("ț", "t")
        val normalizedCity = "timisoara"

        val url = "https://tazz.ro/timisoara/restaurante"

        Thread {
            try {
                // Connect to the Tazz website and fetch the HTML
                val doc = Jsoup.connect(url).get()

                // Select all the restaurant names from <h3 class="store-name">
                val restaurantNames = doc.select("h3.store-name")

                // Loop through all restaurant names and save them to Firebase
                for (nameElement in restaurantNames) {
                    val restaurantName = nameElement.text().trim()

                    // Save restaurant name to Firebase (if necessary, add other details like coordinates, address)
//                    saveRestaurantNameToFirebase(restaurantName)
                }

                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Fetched restaurant names successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching from Tazz: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun fetchLogosForRestaurants(city: String) {
        val normalizedCity = "timisoara"

        val url = "https://tazz.ro/$normalizedCity/restaurante"

        Thread {
            try {
                val doc = Jsoup.connect(url).get()

                val restaurantCards = doc.select("div.store-card")  // Each restaurant box
                val updates = mutableListOf<Pair<String, String>>() // (name, logo)

                for (card in restaurantCards) {
                    val name = card.selectFirst("h3.store-name")?.text()?.trim()
                    val logo = card.selectFirst("img.logo-cover")?.attr("src")?.trim()

                    if (!name.isNullOrEmpty() && !logo.isNullOrEmpty()) {
                        updates.add(name to logo)
                    }
                }

                println("Found ${updates.size} restaurants with logos")

                for ((name, logoUrl) in updates) {
                    updateLogoInFirebase(name, logoUrl)
                }

                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Logos updated for ${updates.size} restaurants.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateLogoInFirebase(restaurantName: String, logoUrl: String) {
        val ref = FirebaseDatabase.getInstance().getReference("Restaurants")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var matchFound = false

                for (child in snapshot.children) {
                    val firebaseName = child.child("name").getValue(String::class.java)?.lowercase()?.trim()
                    val tazzName = restaurantName.lowercase().trim()

                    if (firebaseName == tazzName) {
                        child.ref.child("logo").setValue(logoUrl)
                        println("✅ Logo added for \"$restaurantName\" -> $logoUrl")
                        matchFound = true
                        break // stop after first match
                    }
                }

                if (!matchFound) {
                    println("❌ No match found in Firebase for: \"$restaurantName\"")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ Firebase error: ${error.message}")
            }
        })
    }
    private fun saveRestaurantNameToFirebase(restaurantName: String) {
        val database = FirebaseDatabase.getInstance()
        val restaurantsRef = database.getReference("Restaurants")

        // Normalize the restaurant name (remove accents, trim, lowercase, etc.)
        val normalizedName = normalizeName(restaurantName)

        // First, get all restaurants and compare normalized names manually
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var alreadyExists = false

                for (child in snapshot.children) {
                    val existingName = child.child("name").getValue(String::class.java) ?: continue
                    val normalizedExistingName = normalizeName(existingName)

                    if (normalizedExistingName == normalizedName) {
                        alreadyExists = true
                        break
                    }
                }

                if (!alreadyExists) {
                    val restaurantId = restaurantsRef.push().key
                    val restaurantData = mapOf(
                        "id" to restaurantId,
                        "name" to restaurantName,
                        "latitude" to 0.0,
                        "longitude" to 0.0,
                        "address" to "",
                        "adminUserId" to "adminUserId",
                        "menu" to mapOf<String, MenuItem>(),
                        "categories" to listOf<String>(),
                        "phoneNumber" to ""
                    )

                    restaurantId?.let {
                        restaurantsRef.child(it).setValue(restaurantData)
                        Toast.makeText(
                            requireContext(),
                            "$restaurantName added to the database!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "$restaurantName already exists in the database.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "DB Error: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun normalizeName(name: String): String {
        return name
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ț", "t")
            .replace(Regex("[^a-z0-9 ]"), "") // remove punctuation/special chars
            .replace("\\s+".toRegex(), " ") // normalize whitespace
            .trim()
    }

    private fun retrieveAndDisplayAllRestaurantsFromDatabase() {
        database = FirebaseDatabase.getInstance()
        val restaurantsRef: DatabaseReference = database.reference.child("Restaurants")
        restaurantsList = mutableListOf()

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    Log.d("RestaurantData", "Processing restaurant: ${restaurantSnapshot.key}")
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    restaurant?.let { restaurantsList.add(it) }
                }

                restaurantsList = restaurantsList.filter { restaurant ->
                    !restaurant.menu.isNullOrEmpty() && !restaurant.address.isNullOrBlank()
                }.toMutableList()

                if (restaurantsList.isNotEmpty()) {
                    displayAllRestaurants()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No restaurants with menu and address found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load restaurants: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun fetchRestaurantsNearCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT)
                .show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
//                fetchNearbyRestaurants(latitude, longitude)

                val city = getCityFromLocation(it)
                city?.let { detectedCity ->
                    fetchRestaurantsAddressFromTazz(detectedCity)
//                    fetchMenusFromTazz(detectedCity)
                } ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Could not detect your city",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } ?: run {
                Toast.makeText(
                    requireContext(),
                    "Failed to get current location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCityNameFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val city = addresses[0].locality ?: addresses[0].subAdminArea
                city?.lowercase(Locale.getDefault())?.replace("ș", "s")
                    ?.replace("ț", "t") // Normalize
            } else null
        } catch (e: Exception) {
            Log.e("Geocoding", "Failed to get city name: ${e.message}")
            null
        }
    }

    private fun fetchNearbyRestaurants(latitude: Double, longitude: Double) {
        val city = getCityNameFromLocation(latitude, longitude)

        if (city == null) {
            Toast.makeText(
                requireContext(),
                "Could not determine current city.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val radius = 50000  // 50 km radius to cover a larger area
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=$latitude,$longitude" +
                "&radius=$radius" +
                "&type=restaurant" +
                "&key=$apiKey"

        fetchRestaurantsFromGoogle(url, city)
    }

    private fun fetchRestaurantsFromGoogle(url: String, targetCity: String) {
        if (!isAdded) return

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            try {
                val jsonResponse = JSONObject(response)
                val results = jsonResponse.getJSONArray("results")
                val database = FirebaseDatabase.getInstance()
                val restaurantsRef = database.getReference("Restaurants")
                val adminUserId = "7oXgWeDriHU0HYatdVqY66ibN372"

                // Process each restaurant from the current page
                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val address = place.getString("vicinity")
                    if (!address.contains(targetCity, ignoreCase = true)) continue // ✅ city filter

                    val name = place.getString("name")
                    val placeId = place.getString("place_id")
                    val lat =
                        place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                    val lng =
                        place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")

                    // Create Restaurant object and add to the list
                    val restaurant = Restaurant(placeId, name, lat, lng, address)
                    restaurantsList.add(restaurant)

                    // Check if restaurant already exists in Firebase, and if not, add it
                    restaurantsRef.orderByChild("name").equalTo(name)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    val restaurantId = restaurantsRef.push().key
                                    val restaurantData = mapOf(
                                        "id" to restaurantId,
                                        "name" to name,
                                        "latitude" to lat,
                                        "longitude" to lng,
                                        "address" to address,
                                        "adminUserId" to adminUserId,
                                        "menu" to mapOf<String, MenuItem>(),
                                        "categories" to listOf<String>(),
                                        "phoneNumber" to ""
                                    )
                                    restaurantId?.let {
                                        restaurantsRef.child(it).setValue(restaurantData)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                if (isAdded) {
                                    Toast.makeText(
                                        requireContext(),
                                        "DB Error: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        })
                }

                // Handle pagination (fetch the next page if available)
                val nextPageToken = jsonResponse.optString("next_page_token", "")
                if (nextPageToken.isNotEmpty()) {
                    val nextPageUrl =
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                                "pagetoken=$nextPageToken" +
                                "&key=$apiKey"

                    // Delay to allow for Google API processing of next page
                    Handler().postDelayed({
                        fetchRestaurantsFromGoogle(nextPageUrl, targetCity)
                    }, 2000)  // 2-second delay required by Google API
                } else {
                    // All results fetched, display the restaurants
                    if (isAdded) {
                        displayAllRestaurants()
                    }
                }

            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Error parsing response: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }, { error ->
            if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        val requestQueue = Volley.newRequestQueue(requireContext())
        requestQueue.add(stringRequest)
    }

    private fun displayAllRestaurants() {
        Log.d("HomeFragment", "Total restaurants fetched: ${restaurantsList.size}")
        setRestaurantsAdapter(restaurantsList)
    }

    private fun setRestaurantsAdapter(allRestaurants: List<Restaurant>) {
        val adapter = PopularAdapter(allRestaurants, requireContext())
        binding.PopulerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.PopulerRecyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView
        foodRecyclerView = view.findViewById(R.id.foodRecyclerView)
        foodRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Initialize Adapter
        suggestedFoodAdapter = SuggestedFoodAdapter(suggestedFoodList, object : SuggestedFoodAdapter.OnAddToCartClickListener {
            override fun onAddToCartClicked(food: SuggestedFood) {
                Toast.makeText(requireContext(), "${food.foodName} added to cart!", Toast.LENGTH_SHORT).show()
            }
        })

        foodRecyclerView.adapter = suggestedFoodAdapter

        // Fetch food items from the database
        fetchMenuItems()

        // Access bell icon and red dot (from activity_main.xml)
        val bellIcon = requireActivity().findViewById<ImageView>(R.id.imageView5)
        val notificationDot = requireActivity().findViewById<View>(R.id.notificationDot)

        // Store notifications to show in dropdown
        val pendingNotifications = mutableListOf<NotificationModel>()

        // Check if there are pending orders and update the bell icon
        checkForPendingOrders { notifications ->
            if (notifications.isNotEmpty()) {
                notificationDot.visibility = View.VISIBLE
                pendingNotifications.clear()
                pendingNotifications.addAll(notifications)
            } else {
                notificationDot.visibility = View.GONE
            }
        }

        // Set up bell icon click listener
        bellIcon.setOnClickListener {
            if (pendingNotifications.isEmpty()) {
                Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val popup = PopupMenu(requireContext(), bellIcon)
            pendingNotifications.forEachIndexed { index, notification ->
                popup.menu.add(0, index, 0, notification.message)
            }

            popup.setOnMenuItemClickListener { item ->
                val selectedNotification = pendingNotifications[item.itemId]

                val bundle = Bundle().apply {
                    putString("orderId", selectedNotification.orderId)
                    putString("restaurantName", selectedNotification.restaurantName)
                    putStringArrayList("foodNames", ArrayList(selectedNotification.foodNames))
                    putString("estimatedDeliveryTime", selectedNotification.estimatedDeliveryTime)
                }

                findNavController().navigate(R.id.notificationsFragment, bundle)
                true
            }

            popup.show()
        }

        // Initialize chatbot button
        chatbotIcon = view.findViewById(R.id.chatbotIcon)

        chatbotIcon.setOnClickListener {
            Toast.makeText(requireContext(), "Launching chatbot...", Toast.LENGTH_SHORT).show()
             startActivity(Intent(requireContext(), AIChatbotActivity::class.java))
        }
    }
    private fun fetchMenuItems() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("orders")

        databaseRef.orderByChild("adminUserId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val foodCountMap = mutableMapOf<String, Triple<Int, String, String>>()
                    var restaurantName: String? = null

                    for (orderSnapshot in snapshot.children) {
                        val foodNames = orderSnapshot.child("foodNames").getValue(object : GenericTypeIndicator<List<String>>() {})
                        val foodImages = orderSnapshot.child("foodImages").getValue(object : GenericTypeIndicator<List<String>>() {})
                        val foodPrices = orderSnapshot.child("foodPrices").getValue(object : GenericTypeIndicator<List<String>>() {})
                        val foodQuantities = orderSnapshot.child("foodQuantities").getValue(object : GenericTypeIndicator<List<Int>>() {})
                        val orderRestaurantName = orderSnapshot.child("restaurantName").getValue(String::class.java)

                        if (restaurantName == null && orderRestaurantName != null) {
                            restaurantName = orderRestaurantName
                        }

                        if (foodNames != null && foodImages != null && foodPrices != null && foodQuantities != null) {
                            for (i in foodNames.indices) {
                                val name = foodNames[i]
                                val image = foodImages.getOrNull(i) ?: ""
                                val price = foodPrices.getOrNull(i) ?: ""
                                val quantity = foodQuantities.getOrNull(i) ?: 0

                                val existing = foodCountMap[name]
                                if (existing != null) {
                                    foodCountMap[name] = Triple(existing.first + quantity, existing.second, existing.third)
                                } else {
                                    foodCountMap[name] = Triple(quantity, image, price)
                                }
                            }
                        }
                    }

                    val topOrderedFoods = foodCountMap.entries
                        .sortedByDescending { it.value.first }
                        .take(2) // 🔥 Show top 2 foods (you can change this number as needed)

                    val topFoodItems = topOrderedFoods.map {
                        SuggestedFood(
                            restaurantName = restaurantName ?: "Unknown Restaurant",
                            foodName = it.key,
                            foodPrice = "$" + it.value.third,
                            foodImageResId = it.value.second
                        )
                    }.toMutableList()

                    // 👇 Clear and update adapter
                    suggestedFoodList.clear()
                    suggestedFoodList.addAll(topFoodItems)
                    suggestedFoodAdapter.notifyDataSetChanged()

                    // Debug log
                    Log.d("HomeFragment", "Suggested foods size: ${suggestedFoodList.size}")
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Log.e("HomeFragment", "Database error: ${error.message}")
                }
            })
    }

    private fun getCityFromLocation(location: Location): String? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val rawCity = addresses[0].locality ?: return null
                normalizeCityName(rawCity)
            } else null
        } catch (e: Exception) {
            Log.e("Location", "Failed to get city: ${e.message}")
            null
        }
    }

    private fun normalizeCityName(city: String): String {
        val normalized = java.text.Normalizer.normalize(city, java.text.Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        return normalized.lowercase().replace(" ", "-")
    }

    // 🆕 Scrape menus from Tazz for the detected city
    private fun fetchMenusFromTazz(city: String) {
        val normalizedCity = city
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ț", "t")

        val url = "https://tazz.ro/$normalizedCity/restaurante"

        val restaurantsRef = FirebaseDatabase.getInstance().reference.child("Restaurants")

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val restaurantsList = mutableListOf<Restaurant>()
                for (restaurantSnapshot in snapshot.children) {
                    Log.d("RestaurantData", "Processing restaurant: ${restaurantSnapshot.key}")
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    if (restaurant != null) {
                        restaurantsList.add(restaurant)
                    }
                }

                Thread {
                    try {
                        val doc = Jsoup.connect(url).get()
                        val restaurantLinks =
                            doc.select("a[href^=https://tazz.ro/][href*=/restaurant]")

                        for (link in restaurantLinks) {
                            val href = link.attr("href")
                            val slug = href.split("/").getOrNull(4) ?: continue

                            val matchedRestaurant = restaurantsList.find { restaurant ->
                                val formattedName = restaurant.name
                                    .lowercase()
                                    .replace("ă", "a")
                                    .replace("â", "a")
                                    .replace("î", "i")
                                    .replace("ș", "s")
                                    .replace("ț", "t")
                                    .replace(Regex("[^a-z0-9 ]"), "")
                                    .replace("\\s+".toRegex(), "-")
                                formattedName == slug
                            }

                            matchedRestaurant?.let { restaurant ->
                                val menuItems = scrapeMenuFromRestaurantPage(href)
                                if (menuItems.isNotEmpty()) {
                                    saveMenuToFirebase(restaurant, menuItems)
                                }
                            }
                        }

                    } catch (e: org.jsoup.HttpStatusException) {
                        Log.e(
                            "TazzScraper",
                            "HTTP error fetching URL. Status code: ${e.statusCode}, URL: ${e.url}"
                        )
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "HTTP error ${e.statusCode} while fetching from Tazz",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } catch (e: Exception) {
                        Log.e("TazzScraper", "General error: ${e.message}", e)
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Error fetching from Tazz: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.start()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TazzScraper", "Failed to load restaurants: ${error.message}")
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load restaurant data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun fetchRestaurantsAddressFromTazz(city: String) {
        val normalizedCity = city
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ț", "t")

        val url = "https://tazz.ro/$normalizedCity/restaurante"

        Thread {
            try {
                val doc = Jsoup.connect(url).get()
                val restaurantLinks = doc.select("a[href^=https://tazz.ro/][href*=/restaurant]")

                for (link in restaurantLinks) {
                    val href = link.attr("href")
                    val parts = href.split("/")

                    val citySlug = parts.getOrNull(3) ?: continue
                    val restaurantSlug = parts.getOrNull(4) ?: continue
                    val restaurantId = parts.getOrNull(5) ?: continue

                    val infoUrl =
                        "https://tazz.ro/$citySlug/$restaurantSlug/$restaurantId/partener/informatii"

                    try {
                        val infoDoc = Jsoup.connect(infoUrl).get()
                        val addressElement = infoDoc.selectFirst("div.address")
                        val scrapedAddress = addressElement?.text() ?: "Address not found"

                        // Match with Firebase restaurant by normalized slug
                        val matchedRestaurant = restaurantsList.find { restaurant ->
                            val formattedName = restaurant.name
                                .lowercase()
                                .replace("ă", "a")
                                .replace("â", "a")
                                .replace("î", "i")
                                .replace("ș", "s")
                                .replace("ț", "t")
                                .replace(Regex("[^a-z0-9 ]"), "")
                                .replace("\\s+".toRegex(), "-")
                            formattedName == restaurantSlug
                        }

                        matchedRestaurant?.let { restaurant ->
                            val dbRef = FirebaseDatabase.getInstance()
                                .getReference("Restaurants")
                                .child(restaurant.id)
                                .child("address")

                            dbRef.setValue(scrapedAddress)
                                .addOnSuccessListener {
                                    Log.d(
                                        "TazzAddress",
                                        "Updated address for ${restaurant.name}: $scrapedAddress"
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Log.e(
                                        "TazzAddress",
                                        "Failed to update address for ${restaurant.name}: ${e.message}"
                                    )
                                }
                        }

                    } catch (e: Exception) {
                        Log.e(
                            "TazzAddress",
                            "Error scraping info for $restaurantSlug: ${e.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("TazzAddress", "Error loading restaurant list: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error fetching addresses: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }


    private fun scrapeMenuFromRestaurantPage(url: String): List<MenuItem> {
        val menuItems = mutableListOf<MenuItem>()

        try {
            val doc = Jsoup.connect(url).get()

            // 1. Original approach: parse product cards
            val productCards = doc.select("div.restaurant-product-card")
            for (card in productCards) {
                val name = card.select("h5.title-container").text().trim()
                val description = card.select("p.description-container").text().trim()
                val price = card.select("span.price-container").text().trim()
                val imageUrl = card.select("div.image-container img").attr("src").trim()

                val menuItem = MenuItem(
                    foodName = name,
                    foodPrice = price,
                    foodDescription = description,
                    foodImage = imageUrl
                )
                menuItems.add(menuItem)
            }

            // 2. Enhanced approach: map subcategory IDs to category titles
            val categoryMap = mutableMapOf<String, String>()
            val sectionTitles = doc.select("div.js-section-title")

            for (section in sectionTitles) {
                val id = section.id() // e.g., "subcategory-5210246"
                val subcategoryId = id.removePrefix("subcategory-")
                val titleElement = section.nextElementSibling()
                if (titleElement != null && titleElement.tagName() == "h2" && titleElement.hasClass(
                        "widget-title"
                    )
                ) {
                    categoryMap[subcategoryId] = titleElement.text().trim()
                }
            }

            // 3. Enhanced product parsing: tz-product-dialog-opener
            val dialogOpeners = doc.select("tz-product-dialog-opener")

            for (element in dialogOpeners) {
                val name = element.selectFirst("h5.title-container")?.text()?.trim() ?: continue
                val description =
                    element.selectFirst("p.description-container")?.text()?.trim() ?: ""
                val price = element.selectFirst("span.price-container")?.text()?.trim()
                    ?: element.selectFirst("span.product-price.promo.zprice")?.text()?.trim()
                    ?: element.selectFirst("span.product-price.zprice")?.text()?.trim()
                    ?: ""
                val imageUrl = element.selectFirst("img.img-product")?.attr("src")?.trim() ?: ""

                val openerId = element.id() // e.g., "subcategory-5210246-product-90694695"
                val subcategoryMatch = Regex("subcategory-(\\d+)-product").find(openerId)
                val subcategoryId = subcategoryMatch?.groupValues?.get(1)
                val category = subcategoryId?.let { categoryMap[it] } ?: "Uncategorized"

                val menuItem = MenuItem(
                    foodName = name,
                    foodPrice = price,
                    foodDescription = description,
                    foodImage = imageUrl,
                    foodCategory = category
                )

                menuItems.add(menuItem)
            }

        } catch (e: Exception) {
            Log.e("scrapeMenu", "Error scraping menu: ${e.message}")
        }

        return menuItems
    }

    private fun scrapeCategoryForMenuItems(city: String) {
        val normalizedCity = city
            .lowercase()
            .replace("ă", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ș", "s")
            .replace("ț", "t")

        val url = "https://tazz.ro/$normalizedCity/restaurante"

        Thread {
            try {
                val doc = Jsoup.connect(url).get()
                val restaurantLinks = doc.select("a[href^=https://tazz.ro/][href*=/restaurant]")

                for (link in restaurantLinks) {
                    val href = link.attr("href")
                    val parts = href.split("/")
                    val restaurantSlug = parts.getOrNull(4) ?: continue
                    val partnerId = parts.getOrNull(5) ?: continue

                    val matchedRestaurant = restaurantsList.find { restaurant ->
                        val formattedName = restaurant.name
                            .lowercase()
                            .replace("ă", "a")
                            .replace("â", "a")
                            .replace("î", "i")
                            .replace("ș", "s")
                            .replace("ț", "t")
                            .replace(Regex("[^a-z0-9 ]"), "")
                            .replace("\\s+".toRegex(), "-")
                        formattedName == restaurantSlug
                    }

                    matchedRestaurant?.let { restaurant ->
                        val restaurantId = restaurant.id ?: return@let
                        val menuRef = FirebaseDatabase.getInstance()
                            .getReference("Restaurants")
                            .child(restaurantId)
                            .child("menu")

                        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) return

                                val menuUrl =
                                    "https://tazz.ro/$normalizedCity/$restaurantSlug/$partnerId/restaurant"

                                try {
                                    val restaurantDoc = Jsoup.connect(menuUrl).get()

                                    // Build category map
                                    val categoryMap = mutableMapOf<String, String>()
                                    val sections = restaurantDoc.select("section")
                                    for (section in sections) {
                                        val header = section.selectFirst("h2.widget-title")?.text()
                                            ?: continue
                                        val subcategoryId = section.attr("id")
                                        if (subcategoryId.startsWith("subcategory-")) {
                                            categoryMap[subcategoryId.removePrefix("subcategory-")] =
                                                header
                                        }
                                    }

                                    val productElements =
                                        restaurantDoc.select("tz-product-dialog-opener")

                                    for (menuItemSnapshot in snapshot.children) {
                                        val menuItem =
                                            menuItemSnapshot.getValue(MenuItem::class.java)
                                                ?: continue
                                        val itemKey = menuItemSnapshot.key ?: continue

                                        val matchingElement = productElements.find {
                                            val title =
                                                it.selectFirst("h5.title-container")?.text()?.trim()
                                            title.equals(
                                                menuItem.foodName?.trim(),
                                                ignoreCase = true
                                            )
                                        }

                                        if (matchingElement != null) {
                                            val openerId = matchingElement.attr("id")
                                            val subcategoryMatch =
                                                Regex("subcategory-(\\d+)-product").find(openerId)
                                            val subcategoryId =
                                                subcategoryMatch?.groupValues?.get(1)
                                            val category = subcategoryId?.let { categoryMap[it] }
                                                ?: "Uncategorized"

                                            // Save to Firebase
                                            menuRef.child(itemKey).child("category")
                                                .setValue(category)
                                        }
                                    }

                                } catch (e: Exception) {
                                    Log.e(
                                        "CategoryScraper",
                                        "Error parsing $menuUrl: ${e.message}",
                                        e
                                    )
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(
                                    "Firebase",
                                    "Failed to load menu for ${restaurant.name}: ${error.message}"
                                )
                            }
                        })
                    }
                }

            } catch (e: org.jsoup.HttpStatusException) {
                Log.e(
                    "TazzScraper",
                    "HTTP error fetching URL. Status code: ${e.statusCode}, URL: ${e.url}"
                )
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "HTTP error ${e.statusCode} while scraping categories",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("TazzScraper", "General error: ${e.message}", e)
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error scraping categories: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun saveMenuToFirebase(restaurant: Restaurant, menuItems: List<MenuItem>) {
        val database = FirebaseDatabase.getInstance()
        val restaurantRef = database.getReference("Restaurants").child(restaurant.id!!)
        val menuRef = restaurantRef.child("menu")

        menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Collect existing food names
                val existingNames = mutableMapOf<String, DataSnapshot>()
                for (menuSnapshot in snapshot.children) {
                    try {
                        val item = menuSnapshot.getValue(MenuItem::class.java)
                        val nameKey = item?.foodName?.trim()?.lowercase()
                        if (nameKey != null) {
                            existingNames[nameKey] = menuSnapshot
                        }
                    } catch (e: Exception) {
                        Log.w(
                            "saveMenu",
                            "Skipping invalid menu entry '${menuSnapshot.key}': ${e.message}"
                        )
                    }
                }

                for (item in menuItems) {
                    val foodNameKey = item.foodName?.trim()?.lowercase() ?: continue

                    if (existingNames.containsKey(foodNameKey)) {
                        val existingSnapshot = existingNames[foodNameKey]
                        val existingItem = existingSnapshot?.getValue(MenuItem::class.java)

                        // Update only if foodCategory is missing or empty
                        if (existingItem != null && (existingItem.foodCategory.isNullOrBlank() && !item.foodCategory.isNullOrBlank())) {
                            existingSnapshot.ref.child("foodCategory").setValue(item.foodCategory)
                            Log.d(
                                "saveMenu",
                                "Updated foodCategory for existing item: $foodNameKey"
                            )
                        } else {
                            Log.d("saveMenu", "Skipping duplicate menu item: $foodNameKey")
                        }
                        continue
                    }

                    val menuItemId = menuRef.push().key ?: continue
                    val menuItem = item.copy(
                        key = menuItemId,
                        restaurantName = restaurant.name
                    )

                    menuRef.child(menuItemId).setValue(menuItem)
                        .addOnSuccessListener {
                            Log.d(
                                "saveMenu",
                                "Menu item '${menuItem.foodName}' saved for ${restaurant.name}"
                            )
                        }
                        .addOnFailureListener {
                            Log.e(
                                "saveMenu",
                                "Failed to save menu item '${menuItem.foodName}': ${it.message}"
                            )
                        }

                    existingNames[foodNameKey] = snapshot // Track new entry
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("saveMenu", "Database error: ${error.message}")
            }
        })
    }

    private fun convertAllRestaurantAddressesToLatLong(context: Context) {
        val database = FirebaseDatabase.getInstance()
        val restaurantsRef = database.getReference("Restaurants")

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue
                    val address = restaurantSnapshot.child("address").getValue(String::class.java)
                        ?: continue

                    // Check existing coordinates (0.0 if not present)
                    val currentLat =
                        restaurantSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val currentLng =
                        restaurantSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0

                    // Skip if both coordinates are already set (and not zero)
                    if (currentLat != 0.0 && currentLng != 0.0) {
                        continue
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val geocoder = Geocoder(context)
                            val addresses = geocoder.getFromLocationName(address, 1)

                            if (!addresses.isNullOrEmpty()) {
                                val newLat = addresses[0].latitude
                                val newLng = addresses[0].longitude

                                withContext(Dispatchers.Main) {
                                    restaurantSnapshot.ref.updateChildren(
                                        mapOf(
                                            "latitude" to newLat,
                                            "longitude" to newLng
                                        )
                                    ).addOnSuccessListener {
                                        Log.d(
                                            "GeoUpdate",
                                            "Updated $restaurantId: ($newLat, $newLng)"
                                        )
                                    }.addOnFailureListener { e ->
                                        Log.e(
                                            "GeoUpdate",
                                            "Update failed for $restaurantId: ${e.message}"
                                        )
                                    }
                                }
                            } else {
                                Log.w("GeoUpdate", "No coordinates found for: $address")
                            }
                        } catch (e: Exception) {
                            Log.e("GeoUpdate", "Geocoding error for $address: ${e.message}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GeoUpdate", "Database error: ${error.message}")
            }
        })
    }

    private fun updateAdminUserIds() {
        val database = FirebaseDatabase.getInstance()
        val restaurantsRef = database.getReference("Restaurants")
        val oldAdminId = "adminUserId"
        val newAdminId = "7oXgWeDriHU0HYatdVqY66ibN372"

        // Query restaurants with the old adminUserId
        val query = restaurantsRef.orderByChild("adminUserId").equalTo(oldAdminId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue

                    // Update the adminUserId
                    restaurantSnapshot.ref.child("adminUserId").setValue(newAdminId)
                        .addOnSuccessListener {
                            Log.d("AdminUpdate", "Updated $restaurantId adminUserId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("AdminUpdate", "Failed to update $restaurantId: ${e.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminUpdate", "Database error: ${error.message}")
            }
        })
    }

    private fun removeDuplicateMenuItems() {
        val database = FirebaseDatabase.getInstance()
        val restaurantsRef = database.getReference("Restaurants")

        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (restaurantSnapshot in snapshot.children) {
                    val menuSnapshot = restaurantSnapshot.child("menu")
                    val seenNames = mutableSetOf<String>()
                    val duplicatesToDelete = mutableListOf<String>()

                    for (itemSnapshot in menuSnapshot.children) {
                        val foodName =
                            itemSnapshot.child("foodName").getValue(String::class.java)?.trim()
                                ?.lowercase()
                        val key = itemSnapshot.key

                        if (foodName != null && key != null) {
                            if (seenNames.contains(foodName)) {
                                duplicatesToDelete.add(key)
                            } else {
                                seenNames.add(foodName)
                            }
                        }
                    }

                    // Delete duplicates
                    val menuRef = restaurantSnapshot.ref.child("menu")
                    for (dupKey in duplicatesToDelete) {
                        menuRef.child(dupKey).removeValue()
                            .addOnSuccessListener {
                                Log.d("Cleanup", "Deleted duplicate menu item with key: $dupKey")
                            }
                            .addOnFailureListener {
                                Log.e(
                                    "Cleanup",
                                    "Failed to delete menu item $dupKey: ${it.message}"
                                )
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Cleanup", "Error fetching restaurants: ${error.message}")
            }
        })
    }

    // Notifications
    private fun checkForPendingOrders(callback: (List<NotificationModel>) -> Unit) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("orders")

        databaseRef.orderByChild("adminUserId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<NotificationModel>()

                    for (orderSnapshot in snapshot.children) {
                        val orderAccepted = orderSnapshot.child("orderAccepted").getValue(Boolean::class.java) ?: false
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false

                        if (orderAccepted && !orderDelivered) {
                            val orderId = orderSnapshot.key ?: continue
                            val orderTimeStr = orderSnapshot.child("orderTime").getValue(String::class.java)

                            // Get restaurant name if available
                            val restaurantName = orderSnapshot.child("restaurantName").getValue(String::class.java) ?: "Unknown"

                            // Get food names if available
                            val foodNames = orderSnapshot.child("foodNames").children.mapNotNull { it.getValue(String::class.java) }

                            // Get user location from Firebase
                            val userLocation = orderSnapshot.child("userLocation").getValue(String::class.java)

                            // Get restaurant location from Firebase
                            val restaurantLocation = orderSnapshot.child("restaurantLocation").getValue(String::class.java)

                            // Format order time
                            val formattedTime = if (orderTimeStr != null) {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val date = inputFormat.parse(orderTimeStr)
                                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                outputFormat.format(date!!)
                            } else {
                                val fallbackFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                fallbackFormat.format(Date())
                            }

                            // If user location is available, get coordinates
                            if (!userLocation.isNullOrEmpty()) {
                                DeliveryUtils.getCoordinatesFromAddress(userLocation) { userLat, userLng ->
                                    if (userLat != 0.0 && userLng != 0.0) {
                                        println("User Latitude: $userLat, User Longitude: $userLng")

                                        // Get restaurant coordinates if restaurantLocation is available
                                        if (!restaurantLocation.isNullOrEmpty()) {
                                            DeliveryUtils.getCoordinatesFromAddress(restaurantLocation) { restaurantLat, restaurantLng ->
                                                if (restaurantLat != 0.0 && restaurantLng != 0.0) {
                                                    println("Restaurant Latitude: $restaurantLat, Restaurant Longitude: $restaurantLng")

                                                    // Calculate delivery time
                                                    DeliveryUtils.calculateDeliveryTime(userLat, userLng, restaurantLat, restaurantLng) { estimatedDeliveryTime ->

                                                        // Run the UI updates on the main thread
                                                        Handler(Looper.getMainLooper()).post {
                                                            // Construct the message with estimated delivery time
                                                            val message = "Restaurant: $restaurantName\nFood: $foodNames\nTime: $formattedTime\nEstimated Delivery: $estimatedDeliveryTime"
                                                            val notification = NotificationModel(orderId, message, restaurantName, foodNames, estimatedDeliveryTime)

                                                            // Add notification to the list
                                                            notifications.add(notification)

                                                            // Call the callback when all orders are processed
                                                            callback(notifications)
                                                        }
                                                    }
                                                } else {
                                                    println("Failed to get valid restaurant coordinates")
                                                }
                                            }
                                        } else {
                                            // If restaurant location is not available, still create the notification without the delivery time
                                            Handler(Looper.getMainLooper()).post {
                                                val message = "Restaurant: $restaurantName\nFood: $foodNames\nTime: $formattedTime"
                                                val notification = NotificationModel(orderId, message, restaurantName, foodNames)
                                                notifications.add(notification)

                                                // Call the callback when all orders are processed
                                                if (notifications.size.toLong() == snapshot.childrenCount) {
                                                    callback(notifications)
                                                }
                                            }
                                        }
                                    } else {
                                        println("Failed to get valid user coordinates")
                                    }
                                }
                            } else {
                                // If user location is not available, still create the notification without the delivery time
                                Handler(Looper.getMainLooper()).post {
                                    val message = "Restaurant: $restaurantName\nFood: $foodNames\nTime: $formattedTime"
                                    val notification = NotificationModel(orderId, message, restaurantName, foodNames)
                                    notifications.add(notification)

                                    // Call the callback when all orders are processed
                                    if (notifications.size.toLong() == snapshot.childrenCount) {
                                        callback(notifications)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(emptyList()) // Handle gracefully
                }
            })
    }
    private fun showNotificationOnBell() {
        val bellIcon = view?.findViewById<ImageView>(R.id.imageView5)

        // Set a red dot or any other indicator on the bell icon
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.notification_dot)
        bellIcon?.setImageDrawable(drawable)
    }

    fun triggerChatbotClick() {
        if (::chatbotIcon.isInitialized) {
            chatbotIcon.performClick()
        }
    }
}