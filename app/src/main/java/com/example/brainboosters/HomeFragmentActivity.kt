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

/**
 * The home fragment, which shows the main menu and allows the user to navigate around to
 * different parts of the app.
 */
class HomeFragmentActivity : Fragment() {

    // Gets database and authentication to get users details.
    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val picturesList = mutableListOf<PictureModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.home_fragment, container, false).apply {

        // Formats current date for user.
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val currentDate = LocalDate.now().format(formatter)
        val dateTextView = findViewById<TextView>(R.id.date_text_view)
        dateTextView.text = currentDate


        // Attempts to get users name to display.
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

        // Sets up menu buttons with right colours.
        val startGenerateQuizButton = findViewById<Button>(R.id.start_generate_quiz_button)
        startGenerateQuizButton.setBackgroundColor(Color.parseColor("#38656B"))

        val familyAlbumButton = findViewById<Button>(R.id.family_album_button)
        familyAlbumButton.setBackgroundColor(Color.parseColor("#917C9F"))

        val quizResultsButton = findViewById<Button>(R.id.quiz_results_button)
        quizResultsButton.setBackgroundColor(Color.parseColor("#944547"))

        // Sets up navigation to start quiz.
        val quizImageSelection = QuizImageSelectionActivity()
        startGenerateQuizButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(quizImageSelection)
        }

        // Sets up navigation to start a priority quiz.
        val startPriorityQuizButton = findViewById<Button>(R.id.start_priority_quiz_button)
        startPriorityQuizButton.setOnClickListener {
            fetchAndStartPriorityQuiz()
        }

        // Sets up navigation to go to family album fragment.
        val familyAlbum = FamilyAlbumActivity()
        familyAlbumButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(familyAlbum)
        }

        // Sets up navigation to view previous quizzes.
        val previousQuizResultsFragment = PreviousQuizResultsFragmentActivity()
        quizResultsButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(previousQuizResultsFragment)
        }

        // Using coroutine to fetch additional user details in the background.
        lifecycleScope.launch {
            val userEmail = mAuth.currentUser?.email

            if (userEmail != null) {
                val userDoc = withContext(Dispatchers.IO) {
                    db.collection("users").document(userEmail).get().await()
                }

                // Check if age, dementiaType, and dementiaLevel exist and are not null
                val age = userDoc.getDouble("age")
                val dementiaType = userDoc.getString("dementiaType")
                val dementiaLevel = userDoc.getString("dementiaLevel")

                val rootView = view?.findViewById<View>(R.id.homeFragmentLayout)

                // Shows a message telling the user to update their profile details if they are null.
                if (age == null || dementiaType == null || dementiaLevel == null) {
                    if (rootView != null) {
                        Snackbar.make(rootView, "Please complete the current process before navigating away", Snackbar.LENGTH_LONG).apply {
                            val snackbarLayout = this.view as Snackbar.SnackbarLayout
                            val params = snackbarLayout.layoutParams as ViewGroup.MarginLayoutParams
                            params.setMargins(
                                params.leftMargin,
                                params.topMargin,
                                params.rightMargin,
                                params.bottomMargin + 100
                            )
                            snackbarLayout.layoutParams = params
                            show()
                        }
                    }
                }
            } else {
            }
        }


        // Sets up Log-Out button, to take the user back to the welcome page.
        val logOutButton = findViewById<Button>(R.id.log_out_button)
        logOutButton.setOnClickListener{
            mAuth.signOut()
            val intent = Intent(context, WelcomePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

    }

    /**
     * Fetches images from the database in the order of their priority, so the priority quiz can
     * be started from this fragment.
     */
    private fun fetchAndStartPriorityQuiz() {

        // Queries database for images relating to user by priority.
        db.collection("images")
            .whereEqualTo("uid", mAuth.currentUser?.uid)
            .orderBy("priority", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val sortedPictures = documents.mapNotNull { document ->
                    // Gets details for the picture
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageName = document.getString("name")
                    val imagePlace = document.getString("place")
                    val imagePerson = document.getString("person")
                    val imageYear = document.getLong("year")?.toInt()
                    val imageEvent = document.getString("event")
                    val imageDescription = document.getString("description")
                    val imagePriority = document.getString("priority")

                    // Creates a picture model with these details
                    if (imageUrl != null && imageName != null) {
                        PictureModel(imageUrl, imageName, pictureId, imagePerson,
                            imagePlace, imageEvent, imageDescription, imageYear, null, listOf(), imagePriority)
                    } else null
                }.sortedByDescending {
                    // Takes top 4 priority photos to create the quiz.
                    when (it.imagePriority) {
                        // Creates an index so the priorities can be sorted right, with "High"
                        // at the top.
                        "High" -> 3
                        "Normal" -> 2
                        "Low" -> 1
                        else -> 0
                    }
                }.take(4)

                // Starts quiz if it can find 4 pictures.
                if (sortedPictures.size >= 4) {
                    startQuizActivity(sortedPictures)
                }
                // Lets user know otherwise
                else {
                    Toast.makeText(context, "Not enough images found for the quiz. Must have at least 4.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Starts the QuizActivity with the priority pictures and specifies the type of quiz.
     *
     * @param pictures A list of pictures for the quiz.
     */
    private fun startQuizActivity(pictures: List<PictureModel>) {
        // Creates intent with QuizActivity and parses the pictures through as an array list.
        val intent = Intent(context, QuizActivity::class.java).apply {
            putParcelableArrayListExtra("selectedPictures", ArrayList(pictures))
            putExtra("quizType", "PriorityQuiz")
        }

        // Starts the activity.
        startActivity(intent)
    }
}