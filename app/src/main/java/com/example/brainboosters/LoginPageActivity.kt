package com.example.brainboosters

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

/**
 * LoginPageActivity is where the user can login with an account they already own. The details
 * are verified with Firebase to check they are correct.
 */
class LoginPageActivity : AppCompatActivity() {

    //Used to call Firebase Authentication
    private var mAuth = FirebaseAuth.getInstance()

    /**
     * Called on creation of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        // Creates the animation for the background of this screen.
        val rootLayout = findViewById<ConstraintLayout>(R.id.login_page_layout)
        val animDrawable = rootLayout.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()

        val saveLoginCheckBox = findViewById<CheckBox>(R.id.saveLoginCheckBox)
        val rootView = findViewById<View>(android.R.id.content)

        // Sets up enter button for when it's clicked.
        val enterButton = findViewById<Button>(R.id.enterButton)
        enterButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparentWhiteButton))
        enterButton.setOnClickListener{

            // Gets the text entered in the edit texts
            val emailEntered = findViewById<EditText>(R.id.emailText)
            val passwordEntered = findViewById<EditText>(R.id.passwordText)
            val isEmailEmpty = emailEntered.text.toString()
            val isPasswordEmpty = passwordEntered.text.toString()


            // Checks fields are not empty first
            if (isEmailEmpty.isNotEmpty() && isPasswordEmpty.isNotEmpty()) {

                // Calls Firebase authentication to check entered details match a user in the system
                mAuth.signInWithEmailAndPassword(emailEntered.text.toString(), passwordEntered.text.toString())
                    .addOnCompleteListener(this) { task ->

                        // If task is successful, it checks they are verified.
                        if (task.isSuccessful) {
                            val user = mAuth.currentUser
                            if (user != null && user.isEmailVerified) {

                                // Shows a message and navigates to the home page.
                                Snackbar.make(rootView, "Logging in to App.",
                                    Snackbar.LENGTH_LONG).show()
                                val intent = Intent(this, HomePageActivity::class.java)
                                startActivity(intent)

                            } else {

                                // Shows a message to show account is unverified and signs them out
                                // to make sure user is still not technically signed in.
                                Snackbar.make(rootView,
                                    "Please verify your email before attempting to log in.",
                                    Snackbar.LENGTH_LONG).show()
                                mAuth.signOut()

                            }
                        } else {
                            // Shows a message to let user know an account with those details
                            // does not exist.
                            Snackbar.make(rootView,
                                "Account does not exist.", Snackbar.LENGTH_LONG).show()

                        }
                    }
            } else {
                // Lets users know they cant leave fields empty when trying to sign in.
                Snackbar.make(rootView,
                    "Make sure all fields are filled in when attempting to login.",
                    Snackbar.LENGTH_LONG).show()
            }

            // Establishes a users preference
            val sharedPreferences = getSharedPreferences("com.example.brainboosters", MODE_PRIVATE)

            // If checked, all users sign in details are saved.
            if (saveLoginCheckBox.isChecked) {
                sharedPreferences.edit().apply {
                    putString("email", emailEntered.text.toString())
                    putString("password", passwordEntered.text.toString())
                    putBoolean("saveLogin", true)
                    apply()
                }
            }
            // Otherwise, details will be cleared each time they log in/sign out/restart app.
            else {
                sharedPreferences.edit().apply {
                    remove("email")
                    remove("password")
                    putBoolean("saveLogin", false)
                    apply()
                }
            }
        }

        // Sets up back button to take user back to welcome page.
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparentWhiteButton))
        backButton.setOnClickListener{
            // Finishes current activity to take them back.
            finish()
        }

        // Uses these to clear sharedPreferences if needed.
        val sharedPreferences = getSharedPreferences("com.example.brainboosters", MODE_PRIVATE)
        val email = sharedPreferences.getString("email", "")
        val password = sharedPreferences.getString("password", "")
        val saveLogin = sharedPreferences.getBoolean("saveLogin", false)

        findViewById<EditText>(R.id.emailText).setText(email)
        findViewById<EditText>(R.id.passwordText).setText(password)
        saveLoginCheckBox.isChecked = saveLogin
    }
}



