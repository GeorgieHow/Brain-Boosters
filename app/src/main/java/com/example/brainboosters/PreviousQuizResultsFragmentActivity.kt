package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.adapter.PreviousQuizzesAdapter
import com.example.brainboosters.model.QuizModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


/**
 * A Fragment that displays a list of previous quiz results for the current user.
 */
class PreviousQuizResultsFragmentActivity: Fragment(), PreviousQuizzesAdapter.OnQuizClickListener{

    // Gets database and authentication to get user and their associated records.
    val db = FirebaseFirestore.getInstance()
    val mAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.previous_quiz_results_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Creates back button for user to navigate back to home page.
        val backButton = view.findViewById<Button>(R.id.back_button)
        val homePage = HomeFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(homePage)
        }

        //  Sets up recycler view to show all previous quizzes to user.
        val quizRecylerView = view.findViewById<RecyclerView>(R.id.previous_quiz_recycler_view)
        val quizzesList = mutableListOf<QuizModel>()
        val adapter = PreviousQuizzesAdapter(quizzesList, this)
        quizRecylerView.layoutManager = LinearLayoutManager(context)
        quizRecylerView.adapter = adapter

        // Queries the database sp it can find the quizzes associated to user and adds to list.
        val userId = mAuth.currentUser?.uid
        userId?.let {
            db.collection("quizzes")
                .whereEqualTo("uid", it)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val quiz = document.toObject(QuizModel::class.java).apply{
                            documentId = document.id
                        }
                        quizzesList.add(quiz)
                    }

                    // Notifies adapater that dataset has changed so quizzes will load.
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.d("FirebaseFirestore", "Error getting documents: ", exception)
                }
        }
    }

    /**
     * Handles clicking, so when a quiz is clicked the user can be taken to show more details.
     */
    override fun onQuizClicked(quiz: QuizModel) {
        val quizItemDisplay = PreviousQuizResultsDisplayFragmentActivity()
        val bundle = Bundle().apply {
            // Parses quiz details through.
            putSerializable("quiz", quiz)
        }

        // Changes fragment.
        (activity as HomePageActivity).changeFragment(quizItemDisplay, bundle)
    }
}