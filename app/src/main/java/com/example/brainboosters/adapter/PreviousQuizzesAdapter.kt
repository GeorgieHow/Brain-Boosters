package com.example.brainboosters.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.R
import com.example.brainboosters.model.QuizModel

/**
 * Adapter for displaying a list of previous quizzes in a RecyclerView.
 * Each quiz is shown with its date and notes.
 *
 * @param quizzes List of QuizModel, the dataset for this adapter.
 * @param listener Listener for handling quiz item clicks.
 */
class PreviousQuizzesAdapter(private val quizzes: List<QuizModel>,
                             private val listener: OnQuizClickListener) :
    RecyclerView.Adapter<PreviousQuizzesAdapter.QuizViewHolder>() {

    /**
     * Creates ViewHolder instances for each quiz item in the list.
     * Inflates the layout from XML.
     *
     * @param parent The container for the new view.
     * @param viewType The type of view, not used here as all items are the same.
     * @return A new QuizViewHolder for displaying quiz information.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.previous_quiz_item, parent, false)
        return QuizViewHolder(view)
    }

    /**
     * Binds the quiz data to each ViewHolder. This method sets up the quiz details in the ViewHolder's views
     * and defines the click behavior for each quiz item.
     *
     * @param holder The ViewHolder which should be updated to represent the quiz data.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val quiz = quizzes[position]
        holder.bind(quiz, listener)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The size of the quizzes list.
     */
    override fun getItemCount(): Int = quizzes.size

    /**
     * ViewHolder class for quiz items. Holds the views for the date and notes of the quiz.
     */
    class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizDateText: TextView = itemView.findViewById(R.id.quiz_date_text)
        private val quizNotesText: TextView = itemView.findViewById(R.id.quiz_notes_text)

        /**
         * Binds the quiz data to the TextViews and sets an onClickListener that notifies a listener when
         * the quiz item is clicked.
         *
         * @param quiz The QuizModel data for the current item.
         * @param listener The listener that handles quiz item clicks.
         */
        fun bind(quiz: QuizModel, listener: OnQuizClickListener) {
            quizDateText.text = quiz.date
            quizNotesText.text = quiz.notes
            itemView.setOnClickListener {
                listener.onQuizClicked(quiz)
            }
        }
    }

    /**
     * Interface for handling clicks on quiz items.
     */
    interface OnQuizClickListener {
        fun onQuizClicked(quiz: QuizModel)
    }
}