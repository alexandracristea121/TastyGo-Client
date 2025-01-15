package com.examples.licenta_food_ordering.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.FragmentHomeBinding
import com.examples.licenta_food_ordering.Restaurant
import com.examples.licenta_food_ordering.adaptar.PopularAdapter
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var restaurantsList: MutableList<Restaurant>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Retrieve and display all restaurants
        retrieveAndDisplayAllRestaurants()

        return binding.root
    }

    private fun retrieveAndDisplayAllRestaurants() {
        // Initialize Firebase and reference to the "Restaurants" node
        database = FirebaseDatabase.getInstance()
        val restaurantsRef: DatabaseReference = database.reference.child("Restaurants")
        restaurantsList = mutableListOf()

        // Retrieve all restaurants data from Firebase
        restaurantsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Loop through each restaurant in the snapshot and add it to the list
                for (restaurantSnapshot in snapshot.children) {
                    val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                    restaurant?.let { restaurantsList.add(it) }
                }

                // If we have data, call the function to display all restaurants
                if (restaurantsList.isNotEmpty()) {
                    displayAllRestaurants()
                } else {
                    Toast.makeText(requireContext(), "No restaurants found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load restaurants: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayAllRestaurants() {
        // Log the total number of restaurants fetched
        Log.d("HomeFragment", "Total restaurants fetched: ${restaurantsList.size}")

        // Set the adapter with all the restaurants
        setRestaurantsAdapter(restaurantsList)
    }

    private fun setRestaurantsAdapter(allRestaurants: List<Restaurant>) {
        // Create the adapter with all the restaurants
        val adapter = PopularAdapter(allRestaurants, requireContext())

        // Set the layout manager and adapter for the RecyclerView
        binding.PopulerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.PopulerRecyclerView.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the image slider
        val imageList = ArrayList<SlideModel>()
        imageList.add(SlideModel(R.drawable.banner1, ScaleTypes.FIT))
        imageList.add(SlideModel(R.drawable.banner4, ScaleTypes.FIT))
        imageList.add(SlideModel(R.drawable.banner3, ScaleTypes.FIT))

        val imageSlider = binding.imageSlider
        imageSlider.setImageList(imageList, ScaleTypes.FIT)

        // Item click listener for the image slider
        imageSlider.setItemClickListener(object : ItemClickListener {
            override fun onItemSelected(position: Int) {
                val itemMessage = "Selected Image $position"
                Toast.makeText(requireContext(), itemMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }
}