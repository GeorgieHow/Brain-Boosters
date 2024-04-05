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

        fetchImageIdsForQuiz(documentId)

        val backButton = view.findViewById<Button>(R.id.back_button)
        val previousQuizResultsPage = PreviousQuizResultsFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(previousQuizResultsPage)
        }
    }

    private fun fetchImageIdsForQuiz(quizId: String) {
        val db = FirebaseFirestore.getInstance()
        val imageIds = mutableListOf<String>()

        db.collection("quizImageLinks")
            .whereEqualTo("quizId", quizId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val id = document.getString("imageId")
                    id?.let { imageIds.add(it) }
                }
                fetchImagesDetails(imageIds)
            }
    }

    private fun fetchImagesDetails(imageIds: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val imagesList = mutableListOf<PictureModel>()

        imageIds.forEach { id ->
            db.collection("images")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    val imageUrl = document.getString("imageUrl") ?: return@addOnSuccessListener
                    imagesList.add(PictureModel(documentId = id, imageUrl = imageUrl))
                    if (imagesList.size == imageIds.size) {
                        setupRecyclerView(imagesList)
                    }
                }
        }
    }

    private fun setupRecyclerView(imagesList: List<PictureModel>) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.pictures_used_recycler_view)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView?.adapter = ImagesAdapter(requireContext(), imagesList)
    }
}