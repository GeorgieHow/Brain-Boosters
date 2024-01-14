package com.example.brainboosters

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class HomePageActivity : ComponentActivity() {

    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Home page
        setContentView(R.layout.home_page)

        val userWelcome = findViewById<TextView>(R.id.userEmailTextView)
        val user = mAuth.currentUser

        if (user != null){
            userWelcome.setText("Welcome " + user.email)
        } else {
            userWelcome.setText("User does not exist.")
        }


        val logOutButton = findViewById<Button>(R.id.logOutButton)
        logOutButton.setOnClickListener{
            mAuth.signOut()
            finish()
        }

    }
}