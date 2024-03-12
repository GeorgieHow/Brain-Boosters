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
import androidx.lifecycle.lifecycleScope
import com.example.brainboosters.model.PictureModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeFragmentActivity : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val picturesList = mutableListOf<PictureModel>()


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

        val startGenerateQuizButton = findViewById<Button>(R.id.start_generate_quiz_button)
        val quizImageSelection = QuizImageSelectionActivity()
        startGenerateQuizButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(quizImageSelection)
        }

        val startQuickQuizButton = findViewById<Button>(R.id.start_quick_quiz_button)
        startQuickQuizButton.setOnClickListener {
            lifecycleScope.launch {
                val pictures = fetchRandomPictures()
                // Now you have your MutableList<PictureModel>, pass it to QuizActivity
                val intent = Intent(context, QuizActivity::class.java).apply {
                    putExtra("selectedPictures", ArrayList(pictures)) // Assuming PictureModel is Parcelable
                }
                startActivity(intent)
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

    suspend fun fetchRandomPictures(): MutableList<PictureModel> = withContext(Dispatchers.IO) {
        val db = FirebaseFirestore.getInstance()
        try {
            val documents = db.collection("images").get().await()
            val pictures = documents.documents.mapNotNull { document ->
                try {
                    PictureModel(
                        imageUrl = document.getString("imageUrl"),
                        imageName = document.getString("name"),
                        documentId = document.id,
                        imagePerson = document.getString("person"),
                        imagePlace = document.getString("place"),
                        imageEvent = document.getString("event"),
                        imageYear = document.getLong("year")?.toInt()
                    )
                } catch (e: Exception) {
                    null // Skip any documents that don't match the expected structure
                }
            }.shuffled().take(4).toMutableList()

            pictures
        } catch (e: Exception) {
            mutableListOf() // Return an empty list in case of error
        }
    }

    interface PictureCallback {
        fun onSuccess(pictures: List<PictureModel>)
        fun onError(e: Exception)
    }


}