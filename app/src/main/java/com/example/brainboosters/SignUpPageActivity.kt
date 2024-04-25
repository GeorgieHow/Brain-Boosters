package com.example.brainboosters

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * SignUpPageActivity is where users can create accounts. Firebase Authentication is used for the
 * logging in aspect for later on and Firebase Firestore is used to store the users extra details.
 */
class SignUpPageActivity : AppCompatActivity() {

    // Creates variables to call upon Authentication and the Database
    private var mAuth = FirebaseAuth.getInstance()
    var db = Firebase.firestore

    /**
     * Called on creation of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        // Creates the animation for the background of this screen.
        val rootLayout = findViewById<ConstraintLayout>(R.id.signup_page_layout)
        val animDrawable = rootLayout.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()

        val rootView = findViewById<View>(android.R.id.content)

        // Sets up sign up button for when it's clicked.
        val signUpButton = findViewById<Button>(R.id.sign_up_button)
        signUpButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparentWhiteButton))
        signUpButton.setOnClickListener{

            // Gets all details entered
            val emailEntered = findViewById<EditText>(R.id.email_text)
            val passwordEntered = findViewById<EditText>(R.id.password_text)
            val passwordRetyped = findViewById<EditText>(R.id.password_retype_text)
            val emailString = emailEntered.text.toString()
            val passwordString = passwordEntered.text.toString()
            val passwordRetypedString = passwordRetyped.text.toString()

            val fullNameEntered = findViewById<EditText>(R.id.full_name_text)
            val fullNameString = fullNameEntered.text.toString()

            // Checks if all details are empty
            if(emailString.isNotEmpty() && passwordString.isNotEmpty()
                && fullNameString.isNotEmpty() ){

                // Checks that both password fields are the same
                if(passwordString == passwordRetypedString){

                    // Creates the user in Firebase Authentication with the email and password.
                    mAuth.createUserWithEmailAndPassword(emailString, passwordString)
                        .addOnCompleteListener(this) { task -> if (task.isSuccessful) {
                                Snackbar.make(rootView,
                                    "Account created. Please verify your email before attempting to log in."
                                    , Snackbar.LENGTH_LONG).show()

                                // Gets the current user logged in.
                                val user = task.result?.user
                                val userid = user?.uid

                                // Attempts to send the user the verification email.
                                user?.sendEmailVerification()?.addOnSuccessListener {
                                    Snackbar.make(rootView,
                                        "Verification Email Sent."
                                        , Snackbar.LENGTH_LONG).show()

                                }?.addOnFailureListener { e ->
                                    Snackbar.make(rootView,
                                        "Unable to send Verification Email."
                                        , Snackbar.LENGTH_LONG).show()
                                }

                                // Gets the user collection and creates a document with the
                                // users uid from Authentication.
                                val usersCollection = FirebaseFirestore.getInstance()
                                    .collection("users")
                                val userDocument = usersCollection.document(userid!!)

                                // Gets the full name entered, and maps email and name to the
                                // users details.
                                val userDetails = hashMapOf(
                                    "email" to emailString,
                                    "fullName" to fullNameString,
                                )

                                // The user collection gets updated with these details.
                                userDocument.set(userDetails)
                                    .addOnSuccessListener {
                                        Log.e("Firestore", "Successful creation.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Error writing document", e)
                                    }
                            }
                            else{
                            Snackbar.make(rootView, "Account creation unsuccessful due to:" +
                                    " ${task.exception?.message}", Snackbar.LENGTH_LONG).apply {
                                (view.findViewById(com.google.android.material.R.id.snackbar_text)
                                        as TextView).maxLines = 3
                            }.show()
                            }
                        }
                    }
                }
            // Shows error if user leaves any fields empty.
            else{
                Snackbar.make(rootView,
                    "Cant create account. Don't leave fields empty."
                    , Snackbar.LENGTH_LONG).show()
            }
        }

        // Sets up back button to take user back to welcome page.
        val backButton = findViewById<Button>(R.id.back_button)
        backButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparentWhiteButton))
        backButton.setOnClickListener{
            // Finishes current activity to take them back.
            finish()
        }
    }
}