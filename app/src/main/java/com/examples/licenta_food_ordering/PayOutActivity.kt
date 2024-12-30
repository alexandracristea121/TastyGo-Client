package com.examples.licenta_food_ordering

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.ActivityPayOutBinding
import com.examples.licenta_food_ordering.model.CartItems
import com.examples.licenta_food_ordering.model.OrderDetails
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PayOutActivity : AppCompatActivity() {
    lateinit var binding : ActivityPayOutBinding

    private lateinit var auth:FirebaseAuth
    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String
    private lateinit var totalAmount: String
    private lateinit var foodItemName: ArrayList<String>
    private lateinit var foodItemPrice: ArrayList<String>
    private lateinit var foodItemImage: ArrayList<String>
    private lateinit var foodItemDescription: ArrayList<String>
    private lateinit var foodItemIngredient: ArrayList<String>
    private lateinit var foodItemQuantities: ArrayList<Int>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize Fiebase and User details
        auth=FirebaseAuth.getInstance()
        databaseReference=FirebaseDatabase.getInstance().getReference()
        //set user data
        setUserData()

        //get user details from Firebase
        val intent=intent
        foodItemName=intent.getStringArrayListExtra("FoodItemName") as ArrayList<String>
        foodItemPrice=intent.getStringArrayListExtra("FoodItemPrice") as ArrayList<String>
        foodItemImage=intent.getStringArrayListExtra("FoodItemImage") as ArrayList<String>
        foodItemDescription=intent.getStringArrayListExtra("FoodItemDescription") as ArrayList<String>
        foodItemIngredient=intent.getStringArrayListExtra("FoodItemIngredient") as ArrayList<String>
        foodItemQuantities=intent.getIntegerArrayListExtra("FoodItemQuantities") as ArrayList<Int>

        totalAmount=calculateTotalAmount().toString() +"$"
        //binding.totalAmount.isEnabled=false
        binding.totalAmount.setText(totalAmount)
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.placeMyOrder.setOnClickListener {
            //get data from textview
            name=binding.name.text.toString().trim()
            address=binding.address.text.toString().trim()
            phone=binding.phone.text.toString().trim()
            if(name.isBlank() && address.isBlank() && phone.isBlank()){
                Toast.makeText(this, "Please enter all the details", Toast.LENGTH_SHORT).show()
            }else{
                placeOrder()
            }

        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun placeOrder() {
        userId=auth.currentUser?.uid?:""
        val time=System.currentTimeMillis()
        val itemPushKey=databaseReference.child("OrderDetails").push().key
        val orderDetails=OrderDetails(userId, name, foodItemName, foodItemPrice, foodItemImage, foodItemQuantities, address, totalAmount, phone, time, itemPushKey, false, false)
        val orderReference=databaseReference.child("OrderDetails").child(itemPushKey!!)
        orderReference.setValue(orderDetails).addOnSuccessListener {
            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "Test")
            removeItemFromCart()
            addOrderToHistory(orderDetails)
        }
            .addOnFailureListener {
                Toast.makeText(this, "failed to order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("user").child(userId).child("BuyHistory").child(orderDetails.itemPushkey!!).setValue(orderDetails).addOnSuccessListener {

        }
    }

    private fun removeItemFromCart() {
        val cartItemsReference=databaseReference.child("user").child(userId).child("CartItems")
        cartItemsReference.removeValue()
    }

    private fun calculateTotalAmount(): Int {
        var totalAmount=0
        for( i in 0 until foodItemPrice.size){
            var price=foodItemPrice[i]
            val lastChar=price.last()
            val priceIntVale=if(lastChar =='$'){
                price.dropLast(1).toInt()
            }else{
                price.toInt()
            }
            var quantity=foodItemQuantities[i]
            totalAmount+=priceIntVale * quantity
        }
        return totalAmount
    }

    private fun setUserData() {
        val user=auth.currentUser
        if(user!=null){
            val userId=user.uid
            val userReference = databaseReference.child("user").child(userId)
            userReference.addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
                        val names=snapshot.child("name").getValue(String::class.java)?:""
                        val addresses=snapshot.child("address").getValue(String::class.java)?:""
                        val phones=snapshot.child("phone").getValue(String::class.java)?:""
                        binding.apply {
                            name.setText(names)
                            address.setText(addresses)
                            phone.setText(phones)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }
}