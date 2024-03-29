package com.example.brainboosters

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpPageActivity : AppCompatActivity() {

    private var mAuth = FirebaseAuth.getInstance()
    var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        val rootLayout = findViewById<ConstraintLayout>(R.id.signup_page_layout)
        val animDrawable = rootLayout.background as AnimationDrawable

        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()

        val signUpButton = findViewById<Button>(R.id.sign_up_button)

        signUpButton.setBackgroundColor(Color.parseColor("#4DFFFFFF"))

        signUpButton.setOnClickListener{
                view ->

            val emailEntered = findViewById<EditText>(R.id.email_text)
            val passwordEntered = findViewById<EditText>(R.id.password_text)

            val passwordRetyped = findViewById<EditText>(R.id.password_retype_text)

            val emailString = emailEntered.text.toString()
            val passwordString = passwordEntered.text.toString()

            val passwordRetypedString = passwordRetyped.text.toString()

            if(emailString.isNotEmpty() && passwordString.isNotEmpty()){
                if(passwordString == passwordRetypedString){

                    mAuth.createUserWithEmailAndPassword(emailString, passwordString)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Account Created",
                                    Toast.LENGTH_SHORT).show()

                                val user = task.result?.user
                                val userid = user?.uid

                                val usersCollection = FirebaseFirestore.getInstance()
                                    .collection("users")
                                val userDocument = usersCollection.document(userid!!)

                                val fullName = findViewById<EditText>(R.id.full_name_text).text
                                    .toString()

                                val userDetails = hashMapOf(
                                    "email" to emailString,
                                    "fullName" to fullName,
                                )

                                userDocument.set(userDetails)
                                    .addOnSuccessListener {
                                        Log.e("Firestore", "Successful ;P")
                                    }
                                    .addOnFailureListener { e ->
                                        // Handle errors
                                        Log.e("Firestore", "Error writing document", e)
                                    }
                            }
                            else{
                                Toast.makeText(this, "Account Unsuccessful ",
                                    Toast.LENGTH_SHORT).show()
                            }

                        }
                }
            }
            else{
                Toast.makeText(this, "Don't leave fields empty", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        val backButton = findViewById<Button>(R.id.back_button)

        backButton.setBackgroundColor(Color.parseColor("#4DFFFFFF"))

        backButton.setOnClickListener{
            finish()
        }
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}