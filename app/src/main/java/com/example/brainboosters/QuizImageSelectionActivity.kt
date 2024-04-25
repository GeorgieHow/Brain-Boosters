package com.example.brainboosters

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.QuizPictureAdapter
import com.example.brainboosters.model.PictureModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment where users can select their pictures for the quiz before starting/
 */
class QuizImageSelectionActivity : Fragment(){

    // Sets up recycler view and gets database and authentication.
    private lateinit var recyclerViewImages: RecyclerView
    private val picturesList = mutableListOf<PictureModel>()
    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.quiz_image_selection, container, false)
        // Sets up recycler view.
        recyclerViewImages = view.findViewById(R.id.image_selector_recycler_view)
        recyclerViewImages.layoutManager = GridLayoutManager(requireContext(), 3)
        fetchPicturesFromFirestore { pictures ->
            activity?.runOnUiThread {
                recyclerViewImages.adapter = QuizPictureAdapter(requireContext(), pictures)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Creates back button so user can navigate back to main menu.
        val backButton = view.findViewById<Button>(R.id.back_button)
        val homePage = HomeFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(homePage)
        }

        // Creates start quiz button so the user can start the quiz.
        val startQuizButton = view.findViewById<Button>(R.id.start_quiz_button)
        startQuizButton.setOnClickListener {

            // Gets the adapter for the recycler view and the pictures the user has selected.
            val adapter = recyclerViewImages.adapter as QuizPictureAdapter
            val selectedPictures = adapter.getSelectedPictures()
            val quizType = "GeneratedQuiz"

            // Start quiz if the selected pictures is not empty.
            if (selectedPictures.isNotEmpty()){
                val intent = Intent(context, QuizActivity::class.java).apply {
                    putParcelableArrayListExtra("selectedPictures", ArrayList(selectedPictures))
                    putExtra("quizType", quizType)
                }
                startActivity(intent)

                val homePage = HomeFragmentActivity()
                (activity as? HomePageActivity)?.changeFragment(homePage)

            }
            else{
                // Displays if no pictures have been picked.
                Toast.makeText(context, "Must pick at least 1 picture for the quiz",
                    Toast.LENGTH_SHORT).show()
            }

        }
    }

    /**
     * A method to fetch pictures from firestore to display in recycler view.
     */
    private fun fetchPicturesFromFirestore(completion: (List<PictureModel>) -> Unit) {

        // Looks through the images collection for the users images.
        db.collection("images")
            .whereEqualTo("uid", mAuth.currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                // Gets all the pictures details
                for (document in documents) {
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageName = document.getString("name")
                    val imagePlace = document.getString("place")
                    val imagePerson = document.getString("person")
                    val imageYear = document.getLong("year")?.toInt()
                    val imageEvent = document.getString("event")
                    val imageDescription = document.getString("description")

                    // Creates picture model with it.
                    if (imageUrl != null) {
                        imageName?.let { PictureModel(imageUrl, it, pictureId, imagePerson,
                            imagePlace, imageEvent, imageDescription, imageYear) }
                            ?.let { picturesList.add(it) }
                    }
                }
                completion(picturesList)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

}