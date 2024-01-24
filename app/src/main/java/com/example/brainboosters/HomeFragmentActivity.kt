package com.example.brainboosters

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class HomeFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.home_fragment, container, false).apply {


        //Welcome Message set up for the user
        val welcomeMessage = findViewById<TextView>(R.id.welcome_text)
        val user = mAuth.currentUser

        if (user != null) {
            welcomeMessage.text = "Welcome " + user.email
        }
        else {
            welcomeMessage.text = "User Not Found."
        }

        //Log-Out Button Functionality, Signs out user and takes them back to the welcome page
        val logOutButton = findViewById<Button>(R.id.log_out_button)
        logOutButton.setOnClickListener{
            mAuth.signOut()
            val intent = Intent(context, WelcomePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

    }

}