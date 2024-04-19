package com.example.brainboosters

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

/**
 * WelcomePageActivity is the class the app itself loads up on, where users can either log-in to
 * their accounts, or sign up and create a new one.
 */
class WelcomePageActivity : AppCompatActivity() {

    /**
     * Called on creation of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_page)

        // Creates the animation for the background of this screen.
        val rootLayout = findViewById<ConstraintLayout>(R.id.welcome_page_layout)
        val animDrawable = rootLayout.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()

        // Sets up login button to transfer user to login page.
        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparentWhiteButton))
        loginButton.setOnClickListener{

            //Navigate away to the Login Page if clicked
            val intent = Intent(this, LoginPageActivity::class.java)
            startActivity(intent)
        }

        // Sets up sign up button to transfer user to sign up page.
        val signupButton = findViewById<Button>(R.id.sign_up_button)
        signupButton.setBackgroundColor(ContextCompat.getColor(this, R.color.transparentWhiteButton))
        signupButton.setOnClickListener{

            //Navigate away to the Sign Up Page if clicked
            val intent = Intent(this, SignUpPageActivity::class.java)
            startActivity(intent)
        }
    }

}