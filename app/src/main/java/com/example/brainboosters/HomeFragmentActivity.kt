package com.example.brainboosters

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.home_fragment, container, false).apply {


        //Welcome Message set up for the user
        val fullNameTextView = findViewById<TextView>(R.id.user_fullname)

        val userEmail = mAuth.currentUser?.email
        val usersCollection = FirebaseFirestore.getInstance().collection("users")
        var email = ""
        var fullName = ""

        usersCollection.whereEqualTo("email", userEmail).get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {

                val document = documents.documents[0]

                email = document.getString("email").toString()
                fullName = document.getString("fullName").toString()

                if(fullName != null){
                    fullNameTextView.text = fullName
                }
            }
        }

        val startQuizButton = findViewById<Button>(R.id.start_quiz_button)
        val quizImageSelection = QuizImageSelectionActivity()
        startQuizButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(quizImageSelection)
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