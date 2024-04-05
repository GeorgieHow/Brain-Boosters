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
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PreviousQuizResultsFragmentActivity: Fragment(), PreviousQuizzesAdapter.OnQuizClickListener{

    val db = FirebaseFirestore.getInstance()
    val mAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.previous_quiz_results_fragment, container, false).apply {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<Button>(R.id.back_button)
        val homePage = HomeFragmentActivity()
        backButton.setOnClickListener {
            (activity as HomePageActivity).changeFragment(homePage)
        }

        val quizRecylerView = view.findViewById<RecyclerView>(R.id.previous_quiz_recycler_view)
        val quizzesList = mutableListOf<QuizModel>()
        val adapter = PreviousQuizzesAdapter(quizzesList, this)
        quizRecylerView.layoutManager = LinearLayoutManager(context)
        quizRecylerView.adapter = adapter

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
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.d("FirebaseFirestore", "Error getting documents: ", exception)
                }
        }
    }

    override fun onQuizClicked(quiz: QuizModel) {
        val quizItemDisplay = PreviousQuizResultsDisplayFragmentActivity()
        val bundle = Bundle().apply {
            putSerializable("quiz", quiz)
        }
        (activity as HomePageActivity).changeFragment(quizItemDisplay, bundle)
    }


}