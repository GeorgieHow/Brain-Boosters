package com.example.brainboosters

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.brainboosters.model.PictureModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.SortedMap

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

        val familyAlbumButton = findViewById<Button>(R.id.family_album_button)
        familyAlbumButton.setBackgroundColor(Color.parseColor("#917C9F"))

        val quizResultsButton = findViewById<Button>(R.id.quiz_results_button)
        quizResultsButton.setBackgroundColor(Color.parseColor("#FAC898"))

        val quizImageSelection = QuizImageSelectionActivity()
        startGenerateQuizButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(quizImageSelection)
        }

        val startPriorityQuizButton = findViewById<Button>(R.id.start_priority_quiz_button)
        startPriorityQuizButton.setOnClickListener {
            fetchAndStartPriorityQuiz()
        }

        val familyAlbum = FamilyAlbumActivity()
        familyAlbumButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(familyAlbum)
        }

        val previousQuizResultsFragment = PreviousQuizResultsFragmentActivity()
        quizResultsButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(previousQuizResultsFragment)
        }

        // Using coroutine to perform database operation on a background thread
        lifecycleScope.launch {
            val userEmail = mAuth.currentUser?.email

            if (userEmail != null) {
                val userDoc = withContext(Dispatchers.IO) {
                    db.collection("users").document(userEmail).get().await()
                }

                // Check if age, dementiaType, and dementiaLevel exist and are not null
                val age = userDoc.getDouble("age") // Firestore stores numbers as Doubles
                val dementiaType = userDoc.getString("dementiaType")
                val dementiaLevel = userDoc.getString("dementiaLevel")

                val rootView = view?.findViewById<View>(R.id.homeFragmentLayout)

                if (age == null || dementiaType == null || dementiaLevel == null) {
                    if (rootView != null) {
                        Snackbar.make(rootView, "Please complete the current process before navigating away", Snackbar.LENGTH_LONG).apply {
                            val snackbarLayout = this.view as Snackbar.SnackbarLayout
                            val params = snackbarLayout.layoutParams as ViewGroup.MarginLayoutParams
                            params.setMargins(
                                params.leftMargin,
                                params.topMargin,
                                params.rightMargin,
                                params.bottomMargin + 100 // Adjust this value based on the height of your bottom navigation bar
                            )
                            snackbarLayout.layoutParams = params
                            show()
                        }
                    }
                }
            } else {
                // Handle case where userEmail is null
                Log.e(TAG, "User email is null")
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

    private fun fetchAndStartPriorityQuiz() {
        db.collection("images")
            .whereEqualTo("uid", mAuth.currentUser?.uid)
            .orderBy("priority", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val sortedPictures = documents.mapNotNull { document ->
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageName = document.getString("name")
                    val imagePlace = document.getString("place")
                    val imagePerson = document.getString("person")
                    val imageYear = document.getLong("year")?.toInt()
                    val imageEvent = document.getString("event")
                    val imageDescription = document.getString("description")
                    val imagePriority = document.getString("priority")

                    if (imageUrl != null && imageName != null) {
                        PictureModel(imageUrl, imageName, pictureId, imagePerson,
                            imagePlace, imageEvent, imageDescription, imageYear, null, listOf(), imagePriority)
                    } else null
                }.sortedByDescending {
                    when (it.imagePriority) {
                        "High" -> 3
                        "Normal" -> 2
                        "Low" -> 1
                        else -> 0
                    }
                }.take(4)

                if (sortedPictures.size >= 4) {
                    Log.d("SortedPictures", "$sortedPictures")
                    startQuizActivity(sortedPictures)
                } else {
                    Toast.makeText(context, "Not enough images found for the quiz. Must have at least 4.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun startQuizActivity(pictures: List<PictureModel>) {
        val intent = Intent(context, QuizActivity::class.java).apply {
            putParcelableArrayListExtra("selectedPictures", ArrayList(pictures))
            putExtra("quizType", "PriorityQuiz")
        }
        startActivity(intent)
    }
}