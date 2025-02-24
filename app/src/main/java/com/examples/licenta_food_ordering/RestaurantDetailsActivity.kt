package com.examples.licenta_food_ordering

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.ActivityRestaurantDetailsBinding
import com.examples.licenta_food_ordering.Fragment.CartFragment
import com.examples.licenta_food_ordering.Fragment.HistoryFragment
import com.examples.licenta_food_ordering.Fragment.HomeFragment
import com.examples.licenta_food_ordering.Fragment.ProfileFragment
import com.examples.licenta_food_ordering.Fragment.SearchFragment
import com.examples.licenta_food_ordering.adaptar.MenuAdapter
import com.examples.licenta_food_ordering.model.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RestaurantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRestaurantDetailsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsRef: DatabaseReference
    private lateinit var restaurantId: String
    private lateinit var nameTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var backButton: ImageButton
    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var categorySpinner: Spinner

    private lateinit var menuAdapter: MenuAdapter
    private var menuItems = mutableListOf<MenuItem>()
    private var categories = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRestaurantDetailsBinding.inflate(layoutInflater)
        enableEdgeToEdge()  // Optional: for edge-to-edge layout
        setContentView(binding.root)

        // Set up Firebase reference
        database = FirebaseDatabase.getInstance()
        restaurantsRef = database.getReference("Restaurants")

        // Get the restaurant ID from the intent
        restaurantId = intent.getStringExtra("RESTAURANT_ID") ?: ""

        // Find the UI elements
        nameTextView = findViewById(R.id.restaurantName)
        addressTextView = findViewById(R.id.restaurantAddress)
        backButton = findViewById(R.id.backButton)
        menuRecyclerView = findViewById(R.id.menuRecyclerView)
        categorySpinner = findViewById(R.id.categoryFilterSpinner)

        // Set up RecyclerView for menu items
        menuRecyclerView.layoutManager = LinearLayoutManager(this)
        menuAdapter = MenuAdapter(menuItems, this)
        menuRecyclerView.adapter = menuAdapter

        // Set up the back button click listener
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Fetch restaurant details
        fetchRestaurantDetails()

        // Set up the Bottom Navigation and handle fragment switching
        setupBottomNavigation()
    }

    // Set up Bottom Navigation functionality
    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Hide the bottom navigation if you want it to disappear in the restaurant details page
        bottomNavigationView.visibility = View.VISIBLE

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    switchToFragment(HomeFragment()) // Manually switch to the HomeFragment
                    true
                }

                R.id.profileFragment -> {
                    switchToFragment(ProfileFragment()) // Switch to the ProfileFragment
                    true
                }

                R.id.searchFragment -> {
                    switchToFragment(SearchFragment()) // Switch to the SearchFragment
                    true
                }

                R.id.cartFragment -> {
                    switchToFragment(CartFragment()) // Switch to the CartFragment
                    true
                }

                R.id.historyFragment -> {
                    switchToFragment(HistoryFragment()) // Switch to the HistoryFragment
                    true
                }

                else -> false
            }
        }
    }

    // Function to switch between fragments
    private fun switchToFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView, fragment)
        transaction.addToBackStack(null)  // Optional: add fragment to back stack for back navigation
        transaction.commit()
    }

    // Fetch restaurant details from Firebase
    private fun fetchRestaurantDetails() {
        restaurantsRef.child(restaurantId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val restaurant = snapshot.getValue(Restaurant::class.java)
                if (restaurant != null) {
                    // Set restaurant details on the UI
                    nameTextView.text = restaurant.name
                    addressTextView.text = restaurant.address

                    // Store admin user ID in SharedPreferences
                    val adminUserId = restaurant.adminUserId // Assuming 'adminUserId' is a field in your Restaurant model
                    if (adminUserId != null) {
                        SharedPrefsHelper.saveAdminUserId(this, adminUserId)
                    }

                    // Fetch categories from the menu items
                    fetchCategories()

                    // Fetch the menu items
                    val menu = restaurant.menu // Map of menu items
                    if (menu.isNotEmpty()) {
                        menuItems.clear()
                        for (menuItem in menu.values) {
                            menuItems.add(menuItem)
                        }
                        menuAdapter.notifyDataSetChanged() // Notify adapter of new data
                    }
                }
            } else {
                Toast.makeText(this, "Restaurant not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error fetching restaurant details", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch categories from the menu items
    private fun fetchCategories() {
        val menuRef = restaurantsRef.child(restaurantId).child("menu")

        menuRef.get().addOnSuccessListener { snapshot ->
            val uniqueCategories = mutableSetOf<String>()
            for (childSnapshot in snapshot.children) {
                val category = childSnapshot.child("category").getValue(String::class.java)
                category?.let {
                    uniqueCategories.add(it)
                }
            }

            categories = uniqueCategories.toMutableList()
            categories.add(0, "Any Category") // Add "All" option for displaying all menu items

            setupCategoryFilter()
        }.addOnFailureListener {
            Toast.makeText(this, "Error fetching categories", Toast.LENGTH_SHORT).show()
        }
    }

    // Set up the category filter Spinner
    private fun setupCategoryFilter() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCategory = categories[position]
                if (selectedCategory == "Any Category") {
                    fetchMenuItems() // Fetch all items if "All" is selected
                } else {
                    fetchMenuItemsByCategory(selectedCategory) // Fetch items for the selected category
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }
    }

    // Fetch all menu items from Firebase
    private fun fetchMenuItems() {
        val menuRef: DatabaseReference =
            database.getReference("Restaurants").child(restaurantId).child("menu")
        menuRef.get().addOnSuccessListener { snapshot ->
            menuItems.clear()
            for (childSnapshot in snapshot.children) {
                val menuItem = childSnapshot.getValue(MenuItem::class.java)
                menuItem?.let {
                    menuItems.add(it)
                }
            }
            menuAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Error fetching menu items", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch menu items by category from Firebase
    private fun fetchMenuItemsByCategory(category: String) {
        val menuRef: DatabaseReference =
            database.getReference("Restaurants").child(restaurantId).child("menu")
        menuRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                menuItems.clear()
                for (childSnapshot in snapshot.children) {
                    val menuItem = childSnapshot.getValue(MenuItem::class.java)
                    menuItem?.let {
                        if (it.category == category) {
                            menuItems.add(it)
                        }
                    }
                }
                menuAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "No items found for category: $category", Toast.LENGTH_SHORT)
                    .show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error fetching filtered menu items", Toast.LENGTH_SHORT).show()
        }
    }
}