package com.examples.licenta_food_ordering.presentation.activity

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.AiChatActivityBinding
import com.examples.licenta_food_ordering.model.chat.Message
import com.examples.licenta_food_ordering.adapter.ChatAdapter
import com.examples.licenta_food_ordering.utils.GeminiApiHelper
import com.examples.licenta_food_ordering.model.order.OrderDetails
import com.examples.licenta_food_ordering.service.courier.FirebaseCourierService
import com.examples.licenta_food_ordering.utils.DeliveryUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class AIChatbotActivity : AppCompatActivity() {

    private lateinit var binding: AiChatActivityBinding
    private val messageList = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter
    private var sugestiiCurente: List<String>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsRef: DatabaseReference
    private lateinit var firebaseCourierService: FirebaseCourierService

    // Store the user location
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.white)
        window.navigationBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = AiChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.chatToolbar)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        restaurantsRef = database.getReference("Restaurants")

        // Setup RecyclerView
        setupRecyclerView()

        // Handle sending messages
        binding.sendButton.setOnClickListener {
            val userMessage = binding.messageEditText.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessageToChat(userMessage, Message.Type.USER)
                binding.messageEditText.text?.clear()
                sendToGemini(userMessage)
            }
        }

        // Fetch user's current location
        getUserLocation()
    }

    private fun setupRecyclerView() {
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(messageList)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLat = location.latitude
                    userLng = location.longitude
                }
            }
    }

    private fun addMessageToChat(message: String, type: Message.Type) {
        messageList.add(Message(message, type))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun sendToGemini(userMessage: String) {
        binding.progressBar.visibility = View.VISIBLE

        val alegere = userMessage.toIntOrNull()
        if (alegere != null && sugestiiCurente != null && alegere in 1..sugestiiCurente!!.size) {
            handleOrderSelection(alegere)
            return
        }

        handleFoodSearch(userMessage)
    }

    private fun handleOrderSelection(choice: Int) {
        val selectie = sugestiiCurente!![choice - 1]
        sugestiiCurente = null

        val regex = Regex(
            "(.+) - (\\d+(?:\\.\\d{1,2})?) RON de la restaurantul: (.+)",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(selectie) ?: run {
            binding.progressBar.visibility = View.GONE
            addMessageToChat("Eroare la procesarea selecției", Message.Type.BOT)
            return
        }

        val foodName = match.groups[1]?.value?.trim() ?: ""
        val foodPrice = match.groups[2]?.value?.toDoubleOrNull() ?: 0.0
        val restaurantName = match.groups[3]?.value?.trim() ?: ""
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userUid = sharedPref.getString("userId", null)

        val userRef = FirebaseDatabase.getInstance().getReference("user").child(userUid!!)
        userRef.get().addOnSuccessListener { userSnapshot ->
            val userName =
                userSnapshot.child("name").getValue(String::class.java) ?: "Nume necunoscut"
            val userPhone =
                userSnapshot.child("phone").getValue(String::class.java) ?: "Număr necunoscut"
            val userLocation =
                userSnapshot.child("address").getValue(String::class.java) ?: "Adresă necunoscută"

            val dbRef = FirebaseDatabase.getInstance().getReference("Restaurants")
            dbRef.orderByChild("name").equalTo(restaurantName).get()
                .addOnSuccessListener { restaurantSnapshot ->
                    val restaurant = restaurantSnapshot.children.firstOrNull() ?: run {
                        binding.progressBar.visibility = View.GONE
                        addMessageToChat("Restaurantul nu a fost găsit", Message.Type.BOT)
                        return@addOnSuccessListener
                    }

                    processRestaurantData(
                        restaurant = restaurant,
                        userUid = userUid,
                        userName = userName,
                        userPhone = userPhone,
                        userLocation = userLocation,
                        foodName = foodName,
                        foodPrice = foodPrice,
                        selectie = selectie
                    )
                }
        }
    }

    private fun processRestaurantData(
        restaurant: DataSnapshot,
        userUid: String,
        userName: String,
        userPhone: String,
        userLocation: String,
        foodName: String,
        foodPrice: Double,
        selectie: String
    ) {
        val restaurantLocation =
            restaurant.child("address").getValue(String::class.java) ?: "Locație necunoscută"
        val adminUserId =
            restaurant.child("adminUserId").getValue(String::class.java) ?: "ID Admin necunoscut"
        val orderTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Geocoder(this@AIChatbotActivity, Locale("ro", "RO"))
                } else {
                    Geocoder(this@AIChatbotActivity)
                }

                val userAddress = geocoder.getFromLocationName(userLocation, 1)
                val userLat = userAddress?.firstOrNull()?.latitude?.toString() ?: "0.0"
                val userLng = userAddress?.firstOrNull()?.longitude?.toString() ?: "0.0"

                withContext(Dispatchers.Main) {
                    val order = OrderDetails(
                        userId = userUid,
                        name = userName,
                        foodItemName = arrayListOf(foodName),
                        foodItemPrice = arrayListOf(foodPrice.toString()),
                        foodItemImage = arrayListOf(""),
                        foodItemQuantities = arrayListOf(1),
                        foodItemDescriptions = arrayListOf(""),
                        foodItemIngredients = arrayListOf(""),
                        userLocation = userLocation,
                        userLocationLat = userLat,
                        userLocationLng = userLng,
                        restaurantLocation = restaurantLocation,
                        totalAmount = foodPrice.toString(),
                        phone = userPhone,
                        orderTime = orderTime,
                        itemPushKey = null,
                        orderAccepted = true,
                        paymentReceived = false,
                        adminUserId = adminUserId,
                        restaurantName = restaurant.child("name").getValue(String::class.java) ?: ""
                    )

                    saveOrderToFirebase(order)
                    binding.progressBar.visibility = View.GONE
                    addMessageToChat(
                        "Ai ales:\n$selectie\nComanda ta a fost plasată cu succes!",
                        Message.Type.BOT
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    addMessageToChat("Eroare la obținerea coordonatelor locației", Message.Type.BOT)
                }
            }
        }
    }

    private fun handleFoodSearch(userMessage: String) {
        val directRegex = Regex("(.+?) cu valoarea de maxim (\\d+) ron", RegexOption.IGNORE_CASE)
        val directMatch = directRegex.find(userMessage)

        val produs = directMatch?.groups?.get(1)?.value?.trim()
        val pretMaxim = directMatch?.groups?.get(2)?.value?.toDoubleOrNull()

        if (!produs.isNullOrEmpty() && pretMaxim != null) {
            searchByFoodAndPrice(produs, pretMaxim)
        } else {
            GeminiApiHelper.getGeminiResponse(userMessage) { botReply ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    addMessageToChat(botReply, Message.Type.BOT)
                }

                val geminiMatch = directRegex.find(botReply)
                val extractedProdus = geminiMatch?.groups?.get(1)?.value?.trim()
                val extractedPret = geminiMatch?.groups?.get(2)?.value?.toDoubleOrNull()

                if (!extractedProdus.isNullOrEmpty() && extractedPret != null) {
                    runOnUiThread {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    searchByFoodAndPrice(extractedProdus, extractedPret)
                }
            }
        }
    }

    private fun saveOrderToFirebase(order: OrderDetails) {
        FirebaseDatabase.getInstance().getReference("orders").push().setValue(order)
            .addOnSuccessListener {
                "✅ Comanda a fost salvată și trimisă cu succes!"
                Message.Type.BOT
            }
            .addOnFailureListener {
                "❌ A apărut o eroare la trimiterea comenzii. Te rugăm să încerci din nou."
                Message.Type.BOT
            }
    }

    private fun getAddressLatLng(address: String, callback: (Double, Double) -> Unit) {
        val apiKey = "AIzaSyBgUEkRTXHL_1Z7zK8bPFoy9QswouPd27A" // Your Google API Key

        // Create the URL for the Geocoding API
        val encodedAddress = URLEncoder.encode(address, "UTF-8")
        val url =
            "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=$apiKey"

        // Make the HTTP request
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure here
                callback(0.0, 0.0) // Return a default value if the request fails
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    val jsonObject = JSONObject(jsonResponse)
                    val results = jsonObject.getJSONArray("results")

                    if (results.length() > 0) {
                        val firstResult = results.getJSONObject(0)
                        val geometry = firstResult.getJSONObject("geometry")
                        val location = geometry.getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        // Return the latitude and longitude through the callback
                        callback(lat, lng)
                    } else {
                        callback(0.0, 0.0) // Return default values if no results found
                    }
                } else {
                    callback(0.0, 0.0) // Return default values if the response is unsuccessful
                }
            }
        })
    }

    private fun searchByFoodAndPrice(foodName: String, maxPrice: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference("user")

            dbRef.child(userId).get().addOnSuccessListener { snapshot ->
                val address = snapshot.child("address").getValue(String::class.java) ?: ""

                if (address.isNotEmpty()) {
                    getAddressLatLng(address) { userLat, userLng ->
                        val restaurantRef =
                            FirebaseDatabase.getInstance().getReference("Restaurants")

                        restaurantRef.get().addOnSuccessListener { restaurantSnapshot ->
                            val suggestions = mutableListOf<String>()
                            val deferredList = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

                            lifecycleScope.launch(Dispatchers.IO) {
                                for (restaurant in restaurantSnapshot.children) {
                                    val restaurantName =
                                        restaurant.child("name").getValue(String::class.java)
                                            ?: "Restaurant necunoscut"
                                    val menuItems = restaurant.child("menu").children
                                    val restaurantLat =
                                        restaurant.child("latitude").getValue(Double::class.java)
                                            ?: 0.0
                                    val restaurantLng =
                                        restaurant.child("longitude").getValue(Double::class.java)
                                            ?: 0.0

                                    val deferred = async {
                                        suspendCancellableCoroutine { cont ->
                                            DeliveryUtils.calculateDeliveryTime(
                                                userLat,
                                                userLng,
                                                restaurantLat,
                                                restaurantLng,
                                            ) { deliveryTime ->
                                                for (item in menuItems) {
                                                    val numeProdus = item.child("foodName")
                                                        .getValue(String::class.java)?.lowercase()
                                                        ?: ""
                                                    val rawPrice = item.child("foodPrice")
                                                        .getValue(String::class.java)
                                                        ?.lowercase()
                                                        ?.replace("[^0-9,.]".toRegex(), "") ?: ""
                                                    val pretProdus =
                                                        rawPrice.replace(",", ".").toDoubleOrNull()
                                                            ?: Double.MAX_VALUE

                                                    if (numeProdus.contains(foodName.lowercase()) && pretProdus <= maxPrice && suggestions.size < 3) {
                                                        suggestions.add("$numeProdus - $pretProdus RON de la restaurantul: $restaurantName. Timp estimativ de livrare: $deliveryTime")
                                                    }
                                                }
                                                cont.resume(Unit) {}
                                            }
                                        }
                                    }
                                    deferredList.add(deferred)
                                }

                                deferredList.forEach { it.await() }

                                val response = if (suggestions.isNotEmpty()) {
                                    val formattedList =
                                        suggestions.mapIndexed { index, s -> "${index + 1}. $s" }
                                            .joinToString("\n")
                                    "🍽 Iată câteva sugestii pentru **\"$foodName\"** sub **$maxPrice RON**:\n\n$formattedList\n\n✋ Răspunde cu *1*, *2* sau *3* pentru a comanda."
                                } else {
                                    "😞 Din păcate, nu am găsit produse **\"$foodName\"** sub **$maxPrice RON** în acest moment."
                                }

                                withContext(Dispatchers.Main) {
                                    binding.progressBar.visibility = View.GONE
                                    addMessageToChat(response, Message.Type.BOT)
                                    sugestiiCurente = suggestions // Store for later selection
                                }
                            }
                        }
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    addMessageToChat("Nu am găsit adresa utilizatorului.", Message.Type.BOT)
                }
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                addMessageToChat(
                    "A apărut o eroare la obținerea datelor din tabelul utilizator.",
                    Message.Type.BOT
                )
            }
        } else {
            binding.progressBar.visibility = View.GONE
            addMessageToChat("Nu ești autentificat. Te rugăm să te autentifici.", Message.Type.BOT)
        }
    }

    // Define a response class for DistanceMatrix API
    data class DistanceMatrixResponse(
        val rows: List<Row>
    )

    data class Row(
        val elements: List<Element>
    )

    data class Element(
        val duration: Duration
    )

    data class Duration(
        val text: String
    )
}