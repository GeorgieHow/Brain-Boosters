package com.example.brainboosters

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.brainboosters.model.PictureModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val picturesList = mutableListOf<PictureModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.home_fragment, container, false).apply {

        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val currentDate = LocalDate.now().format(formatter)
        val dateTextView = findViewById<TextView>(R.id.date_text_view)

        dateTextView.text = currentDate


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

        val startGenerateQuizButton = findViewById<Button>(R.id.start_generate_quiz_button)
        startGenerateQuizButton.setBackgroundColor(Color.parseColor("#38656B"))

        val familyAlbumQuizButton = findViewById<Button>(R.id.family_album_button)
        familyAlbumQuizButton.setBackgroundColor(Color.parseColor("#917C9F"))

        val quizResultsButton = findViewById<Button>(R.id.quiz_results_button)
        quizResultsButton.setBackgroundColor(Color.parseColor("#FAC898"))

        val quizImageSelection = QuizImageSelectionActivity()
        startGenerateQuizButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(quizImageSelection)
        }

        val familyAlbum = FamilyAlbumActivity()
        familyAlbumQuizButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(familyAlbum)
        }

        val previousQuizResultsFragment = PreviousQuizResultsFragmentActivity()
        quizResultsButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(previousQuizResultsFragment)
        }

        // Using coroutine to perform database operation on a background thread
        lifecycleScope.launch {
            val userDoc = withContext(Dispatchers.IO) {
                db.collection("users").document(userEmail!!).get().await()
            }

            // Check if age, dementiaType, and dementiaLevel exist and are not null
            val age = userDoc.getDouble("age") // Firestore stores numbers as Doubles
            val dementiaType = userDoc.getString("dementiaType")
            val dementiaLevel = userDoc.getString("dementiaLevel")

            if (age == null || dementiaType == null || dementiaLevel == null) {
                Snackbar.make(this@apply, "Please update your profile information if possible in the profile menu", Snackbar.LENGTH_LONG).show()
            }
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