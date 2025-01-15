package com.examples.licenta_food_ordering

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.ActivityPayOutBinding
import com.examples.licenta_food_ordering.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PayOutActivity : AppCompatActivity() {

    lateinit var binding: ActivityPayOutBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String
    private lateinit var totalAmount: String
    private lateinit var foodItemName: ArrayList<String>
    private lateinit var foodItemPrice: ArrayList<String>
    private lateinit var foodItemImage: ArrayList<String>
    private lateinit var foodItemQuantities: ArrayList<Int>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String

    private val stripe: Stripe by lazy {
        Stripe(applicationContext, "pk_test_51O5AEKErOmyaHsbbQATChHY8CoF7Gb7HHcm6k5765EfnSgTnCpsSLs5CRuEvZ0VG2wrbwOqgETTB7K7pITVsDq5c00xTuRRn6y") // Your Stripe publishable key
    }

    private lateinit var adminUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminUserId = SharedPrefsHelper.getAdminUserId(this) ?: "Unknown Admin ID"

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference()

        PaymentConfiguration.init(applicationContext, "pk_test_51O5AEKErOmyaHsbbQATChHY8CoF7Gb7HHcm6k5765EfnSgTnCpsSLs5CRuEvZ0VG2wrbwOqgETTB7K7pITVsDq5c00xTuRRn6y")

        setUserData()

        val intent = intent
        foodItemName = intent.getStringArrayListExtra("FoodItemName") as ArrayList<String>
        foodItemPrice = intent.getStringArrayListExtra("FoodItemPrice") as ArrayList<String>
        foodItemImage = intent.getStringArrayListExtra("FoodItemImage") as ArrayList<String>
        foodItemQuantities = intent.getIntegerArrayListExtra("FoodItemQuantities") as ArrayList<Int>

        totalAmount = calculateTotalAmount().toString() + " $"
        binding.totalAmount.setText(totalAmount)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.placeMyOrder.setOnClickListener {
            name = binding.name.text.toString().trim()
            address = binding.address.text.toString().trim()
            phone = binding.phone.text.toString().trim()

            if (name.isBlank() || address.isBlank() || phone.isBlank()) {
                Toast.makeText(this, "Please enter all the details", Toast.LENGTH_SHORT).show()
            } else {
                val amount = calculateTotalAmountInCents()
                createPaymentIntent(amount)
            }
        }
    }

    private fun createPaymentIntent(amount: Long) {
        val paymentRequest = PaymentRequest(amount)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.createPaymentIntent(paymentRequest)
                if (response.isSuccessful) {
                    val clientSecret = response.body()?.clientSecret
                    if (clientSecret != null) {
                        confirmPayment(clientSecret)
                    } else {
                        Toast.makeText(this@PayOutActivity, "Failed to get client secret", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PayOutActivity, "Failed to create payment intent", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PayOutActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmPayment(clientSecret: String) {
        val cardParams = PaymentMethodCreateParams.createCard(
            CardParams("4242424242424242", 12, 25, "123")
        )

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            cardParams,
            clientSecret
        )

        stripe.confirmPayment(this, confirmParams)
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

    private fun calculateTotalAmountInCents(): Long {
        var totalAmount = 0L
        for (i in 0 until foodItemPrice.size) {
            var price = foodItemPrice[i]
            val lastChar = price.last()

            val priceIntValue = when {
                price.contains("$") -> price.dropLast(1).toDouble()
                price.contains("€") -> price.dropLast(1).toDouble()
                else -> price.toDouble()
            }

            val priceInCents = (priceIntValue * 100).toLong()

            val quantity = foodItemQuantities[i]

            totalAmount += priceInCents * quantity
        }
        return totalAmount
    }

    private fun setUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userReference = databaseReference.child("user").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val names = snapshot.child("name").getValue(String::class.java) ?: ""
                        val addresses = snapshot.child("address").getValue(String::class.java) ?: ""
                        val phones = snapshot.child("phone").getValue(String::class.java) ?: ""
                        binding.apply {
                            name.setText(names)
                            address.setText(addresses)
                            phone.setText(phones)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                result.intent.status?.let { status ->
                    when (status) {
                        StripeIntent.Status.Succeeded -> {
                            Toast.makeText(this@PayOutActivity, "Payment successful", Toast.LENGTH_SHORT).show()
                            placeOrder()
                        }
                        StripeIntent.Status.RequiresPaymentMethod -> {
                            Toast.makeText(this@PayOutActivity, "Payment failed", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this@PayOutActivity, "Payment status unknown", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Toast.makeText(this@PayOutActivity, "Payment status is null", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onError(e: Exception) {
                Toast.makeText(this@PayOutActivity, "Payment failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun placeOrder() {
        userId = auth.currentUser?.uid ?: ""
        val time = System.currentTimeMillis()
        val itemPushKey = databaseReference.child("OrderDetails").push().key
        val orderDetails = OrderDetails(
            userId,
            name,
            foodItemName,
            foodItemPrice,
            foodItemImage,
            foodItemQuantities,
            address,
            totalAmount,
            phone,
            time,
            itemPushKey,
            false,
            false,
            adminUserId
        )
        val orderReference = databaseReference.child("OrderDetails").child(itemPushKey!!)
        orderReference.setValue(orderDetails).addOnSuccessListener {
            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "Test")
            removeItemFromCart()
            addOrderToHistory(orderDetails)
        }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to place order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeItemFromCart() {
        val cartItemsReference = databaseReference.child("user").child(userId).child("CartItems")
        cartItemsReference.removeValue()
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("user").child(userId).child("BuyHistory")
            .child(orderDetails.itemPushkey!!).setValue(orderDetails)
            .addOnSuccessListener {}
    }
}