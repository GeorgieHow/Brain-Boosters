package com.example.brainboosters.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.R
import com.example.brainboosters.model.QuizModel

class PreviousQuizzesAdapter(private val quizzes: List<QuizModel>, private val listener: OnQuizClickListener) : RecyclerView.Adapter<PreviousQuizzesAdapter.QuizViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.previous_quiz_item, parent, false)
        return QuizViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val quiz = quizzes[position]
        holder.bind(quiz, listener)
    }

    override fun getItemCount(): Int = quizzes.size

    class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizDateText: TextView = itemView.findViewById(R.id.quiz_date_text)
        private val quizNotesText: TextView = itemView.findViewById(R.id.quiz_notes_text)

        fun bind(quiz: QuizModel, listener: OnQuizClickListener) {
            quizDateText.text = quiz.date
            quizNotesText.text = quiz.notes
            itemView.setOnClickListener {
                listener.onQuizClicked(quiz)
            }
        }
    }
    interface OnQuizClickListener {
        fun onQuizClicked(quiz: QuizModel)
    }
}