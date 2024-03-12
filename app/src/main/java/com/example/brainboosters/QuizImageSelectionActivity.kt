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
import com.google.firebase.firestore.FirebaseFirestore

class QuizImageSelectionActivity : Fragment(){

    private lateinit var recyclerViewImages: RecyclerView
    private val picturesList = mutableListOf<PictureModel>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.quiz_image_selection, container, false)
        recyclerViewImages = view.findViewById(R.id.image_selector_recycler_view)


        recyclerViewImages.layoutManager = GridLayoutManager(requireContext(), 3)

        // Fetch pictures and set up the adapter within the completion block
        fetchPicturesFromFirestore { pictures ->
            activity?.runOnUiThread {
                recyclerViewImages.adapter = QuizPictureAdapter(requireContext(), pictures)
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<Button>(R.id.back_button)
        val homePage = HomeFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(homePage)
        }

        val startQuizButton = view.findViewById<Button>(R.id.start_quiz_button)
        startQuizButton.setOnClickListener {
            val adapter = recyclerViewImages.adapter as QuizPictureAdapter
            val selectedPictures = adapter.getSelectedPictures()
            val quizType = "GeneratedQuiz"

            if (selectedPictures.isNotEmpty()){
                val intent = Intent(context, QuizActivity::class.java).apply {
                    putParcelableArrayListExtra("selectedPictures", ArrayList(selectedPictures))
                    putExtra("quizType", quizType)
                }
                startActivity(intent)
            }
            else{
                Toast.makeText(context, "Must pick at least 1 picture for the quiz",
                    Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun fetchPicturesFromFirestore(completion: (List<PictureModel>) -> Unit) {

        db.collection("images").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val imageUrl = document.getString("imageUrl")
                    val pictureId = document.id
                    val imageName = document.getString("name")
                    val imagePlace = document.getString("place")
                    val imagePerson = document.getString("person")
                    val imageYear = document.getLong("year")?.toInt()
                    val imageEvent = document.getString("event")

                    if (imageUrl != null) {
                        imageName?.let { PictureModel(imageUrl, it, pictureId, imagePerson,
                            imagePlace, imageEvent, imageYear) }
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