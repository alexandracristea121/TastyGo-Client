package com.examples.licenta_food_ordering.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.util.Patterns
import com.example.licenta_food_ordering.databinding.FragmentProfileBinding
import com.examples.licenta_food_ordering.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.examples.licenta_food_ordering.LoginActivity

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false)

        saveUserData()

        binding.apply {
            name.isEnabled = false
            email.isEnabled = false
            address.isEnabled = false
            phone.isEnabled = false

            editButton.setOnClickListener {
                name.isEnabled = !name.isEnabled
                email.isEnabled = !email.isEnabled
                address.isEnabled = !address.isEnabled
                phone.isEnabled = !phone.isEnabled

                if (name.isEnabled) {
                    Toast.makeText(requireContext(), "Editing Profile", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Profile Edit Disabled", Toast.LENGTH_SHORT).show()
                }
            }

            saveInfoButton.setOnClickListener {
                val name = binding.name.text.toString()
                val email = binding.email.text.toString()
                val address = binding.address.text.toString()
                val phone = binding.phone.text.toString()

                if (validateInput(name, email, address, phone)) {
                    updateUserData(name, email, address, phone)
                }
            }

            logoutButton.setOnClickListener {
                logOut()
            }
        }

        return binding.root
    }

    private fun logOut() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private fun updateUserData(name: String, email: String, address: String, phone: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)
            val userData = hashMapOf(
                "name" to name,
                "address" to address,
                "email" to email,
                "phone" to phone
            )
            userReference.setValue(userData).addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Profile update failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userProfile = snapshot.getValue(UserModel::class.java)
                        if (userProfile != null) {
                            binding.name.setText(userProfile.name)
                            binding.email.setText(userProfile.email)

                            val address = userProfile.address ?: "Address not provided"
                            binding.address.setText(address)

                            val phone = userProfile.phone ?: "Phone number not provided"
                            binding.phone.setText(phone)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun validateInput(name: String, email: String, address: String, phone: String): Boolean {
        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isBlank()) {
            Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (phone.isBlank()) {
            Toast.makeText(requireContext(), "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!phone.matches("^[0-9]{10,}$".toRegex())) {
            Toast.makeText(requireContext(), "Phone number must contain at least 10 digits", Toast.LENGTH_SHORT).show()
            return false
        }

        if (address.isBlank()) {
            Toast.makeText(requireContext(), "Address cannot be empty", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}