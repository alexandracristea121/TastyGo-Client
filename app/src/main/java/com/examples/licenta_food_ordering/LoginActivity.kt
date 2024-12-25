package com.examples.licenta_food_ordering

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.licenta_food_ordering.R
import com.example.licenta_food_ordering.databinding.ActivityLoginBinding
import com.examples.licenta_food_ordering.model.UserModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database

class LoginActivity : AppCompatActivity() {
    private var userName: String?=null
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val googleSignInOptions=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        auth= Firebase.auth
        database=Firebase.database.reference
        googleSignInClient=GoogleSignIn.getClient(this, googleSignInOptions)

        binding.loginButton.setOnClickListener{


            email=binding.emailAddress.text.toString().trim()
            password=binding.password.text.toString().trim()
            if(email.isBlank() || password.isBlank()){
                Toast.makeText(this, "Please enter all the details", Toast.LENGTH_SHORT).show()
            }else{
                createUser()
                Toast.makeText(this, "Login successfull", Toast.LENGTH_SHORT).show()
            }
        }
        binding.donthavebutton.setOnClickListener{
            val intent = Intent(this, SignActivity::class.java)
            startActivity(intent)
        }

        binding.googleButton.setOnClickListener {
            val signInIntent=googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val launcher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
        if(result.resultCode== Activity.RESULT_OK){
            val task=GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if(task.isSuccessful){
                val account: GoogleSignInAccount?=task.result
                val credential= GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { task->
                    if(task.isSuccessful){
                        Toast.makeText(this, "Sign in successfull", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }else{
                        Toast.makeText(this, "Sign in field", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }else{
            Toast.makeText(this, "Sign in field", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUser() {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task->
            if(task.isSuccessful){
                val user=auth.currentUser
                updateUi(user)
            }else{
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task->
                    if(task.isSuccessful){
                        saveUserData()
                        val user=auth.currentUser
                        updateUi(user)
                    }else{
                        Toast.makeText(this, "Sign in field", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
    }

    private fun saveUserData() {
        email=binding.emailAddress.text.toString().trim()
        password=binding.password.text.toString().trim()
        val user = UserModel(userName, email, password)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        database.child("user").child(userId).setValue(user)
    }

    override fun onStart() {
        super.onStart()
        val currentUser=auth.currentUser
        if(currentUser!=null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updateUi(user: FirebaseUser?) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}