package com.example.brainboosters

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class QuizResultsActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_result_page)

        val messageTextView = findViewById<TextView>(R.id.well_done_text)
        messageTextView.text = getRandMessage()

        val questionsRight = intent.getIntExtra("questionsRight", 0)
        val questionsWrong = intent.getIntExtra("questionsWrong", 0)

        val questionsRightTextView = findViewById<TextView>(R.id.no_right_text)
        val questionsWrongTextView = findViewById<TextView>(R.id.no_wrong_text)

        questionsRightTextView.text = questionsRight.toString()
        questionsWrongTextView.text = questionsWrong.toString()

    }

    private fun getRandMessage(): String{
        val messages = listOf(
            "Great job!",
            "Well done!",
            "Fantastic!"
        )

        return messages.random()
    }
}