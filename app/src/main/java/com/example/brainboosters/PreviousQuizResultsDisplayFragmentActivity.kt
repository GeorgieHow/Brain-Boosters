package com.example.brainboosters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.model.PictureModel
import com.example.brainboosters.model.QuizModel
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A Fragment that displays the specific details of a quiz.
 */
class PreviousQuizResultsDisplayFragmentActivity: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.previous_quiz_results_display_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val quizDateTextView = view.findViewById<TextView>(R.id.quiz_date_text_view)
        val quizNotesTextView = view.findViewById<TextView>(R.id.quiz_notes_text_view)
        val quizMoodTextView = view.findViewById<TextView>(R.id.quiz_mood_text_view)
        val quizQuestionsRightTextView = view.findViewById<TextView>(R.id.questions_right_text_view)
        val quizQuestionsWrongTextView = view.findViewById<TextView>(R.id.questions_wrong_text_view)

        var documentId = ""

        // Gets the data from the quiz parsed through and sets it to the right text view.
        val quiz = arguments?.getSerializable("quiz") as? QuizModel
        if (quiz != null) {
            val date = quiz.date
            quizDateTextView.text = date

            val notes = quiz.notes
            quizNotesTextView.text = notes

            val mood = quiz.mood
            quizMoodTextView.text = mood

            val questionsRight = quiz.questionsRight.toString()
            quizQuestionsRightTextView.text = questionsRight

            val questionsWrong = quiz.questionsWrong.toString()
            quizQuestionsWrongTextView.text = questionsWrong

            documentId = quiz.documentId.toString()
        }

        // Fetches images associated with the quiz to put in the recyler view.
        fetchImageIdsForQuiz(documentId)

        // Sets up back button so user can navigate back to the previous quizzes fragment.
        val backButton = view.findViewById<Button>(R.id.back_button)
        val previousQuizResultsPage = PreviousQuizResultsFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(previousQuizResultsPage)
        }
    }

    /**
     * A method to fetch all the images for the recycler view.
     *
     * @param quizId A string that contains the ID of the quiz.
     */
    private fun fetchImageIdsForQuiz(quizId: String) {

        // Gets database and creates list to put images in.
        val db = FirebaseFirestore.getInstance()
        val imageIds = mutableListOf<String>()

        // Uses the junction table in database to find images.
        db.collection("quizImageLinks")
            .whereEqualTo("quizId", quizId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Adds image id to the list.
                    val id = document.getString("imageId")
                    id?.let { imageIds.add(it) }
                }

                // Uses the ids to fetch more data.
                fetchImagesDetails(imageIds)
            }
    }

    /**
     * A method which looks through the list of picture ids to get their details.
     *
     * @param imageIds A list with all image ids stored as a string.
     */
    private fun fetchImagesDetails(imageIds: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val imagesList = mutableListOf<PictureModel>()

        // Loops through each id, and finds it in images.
        imageIds.forEach { id ->
            db.collection("images")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    // Gets the url and runs the recycler view method to set it up.
                    val imageUrl = document.getString("imageUrl") ?: return@addOnSuccessListener
                    imagesList.add(PictureModel(documentId = id, imageUrl = imageUrl))
                    if (imagesList.size == imageIds.size) {
                        setupRecyclerView(imagesList)
                    }
                }
        }
    }

    /**
     * A method which sets up the recycler view with the picture model parsed through.
     *
     * @param imagesList A list of PictureModels which have all the right image details to show.
     */
    private fun setupRecyclerView(imagesList: List<PictureModel>) {

        // Sets up recylcer view with the right adapter and parses list through to display.
        val recyclerView = view?.findViewById<RecyclerView>(R.id.pictures_used_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView?.adapter = ImagesAdapter(requireContext(), imagesList)
    }
}