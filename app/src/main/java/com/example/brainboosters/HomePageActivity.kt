package com.example.brainboosters

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class HomePageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Home page
        setContentView(R.layout.home_page)

        val userWelcome = findViewById<TextView>(R.id.userEmailTextView)
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null){
            userWelcome.setText(user.email)
        } else {
            userWelcome.setText("User does not exist.")
        }

    }
}