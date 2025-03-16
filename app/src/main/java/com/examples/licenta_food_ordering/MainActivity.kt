package com.examples.licenta_food_ordering

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment)
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment)
                }
                R.id.searchFragment -> {
                    navController.navigate(R.id.searchFragment)
                }
                R.id.cartFragment -> {
                    navController.navigate(R.id.cartFragment)
                }
                R.id.historyFragment -> {
                    navController.navigate(R.id.historyFragment)
                }
            }
            true
        }

        // Set default fragment when the app is first launched
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.homeFragment
        }

        // Handle click for bottom sheet
        binding.imageView5.setOnClickListener {
            val bottomSheetDialog = Notification_Bottom_Fragment()
            bottomSheetDialog.show(supportFragmentManager, "Test")
        }

        // Handle window insets to prevent UI overlapping
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(0, 0, 0, 0)
            bottomNavigationView.setPadding(0, 0, 0, 0)

            insets
        }
    }

    // Handle returning from RestaurantDetailsActivity (optional)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.homeFragment) // Navigate back to home or the appropriate fragment
        }
    }

    // Handle back button press if needed
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        super.onBackPressed()
    }
}