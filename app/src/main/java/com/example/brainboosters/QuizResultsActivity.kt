package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.brainboosters.model.PictureModel
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class QuizResultsActivity : AppCompatActivity(){

    //For mutable maps ;P
    private val gson = Gson()

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var questionsRight: Int = 0
    private var questionsWrong: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_result_page)

        //Well done message
        val messageTextView = findViewById<TextView>(R.id.well_done_text)
        messageTextView.text = getRandMessage()

        //Amount of questions gotten right or wrong
        questionsRight = intent.getIntExtra("questionsRight", 0)
        questionsWrong = intent.getIntExtra("questionsWrong", 0)

        val questionsRightTextView = findViewById<TextView>(R.id.no_right_text)
        val questionsWrongTextView = findViewById<TextView>(R.id.no_wrong_text)

        questionsRightTextView.text = questionsRight.toString()
        questionsWrongTextView.text = questionsWrong.toString()

        //MutableMaps here, use to assign these to pictures in database
        val correctAnswersMap  = object : TypeToken<MutableMap<String, Int>>() {}.type
        val incorrectAnswersMap  = object : TypeToken<MutableMap<String, Int>>() {}.type

        val correctAnswersCountMap: MutableMap<String, Int> = gson.fromJson(
            intent.getStringExtra("correctAnswersMap"), correctAnswersMap
        ) ?: mutableMapOf()

        val incorrectAnswersCountMap: MutableMap<String, Int> = gson.fromJson(
            intent.getStringExtra("incorrectAnswersMap"), incorrectAnswersMap
        ) ?: mutableMapOf()


        //Array list of photos used
        val selectedPictures: ArrayList<PictureModel> =
            intent.getParcelableArrayListExtra<PictureModel>("selectedPictures")
                ?: arrayListOf()

        uploadQuizToDB()

    }

    private fun uploadQuizToDB(){

        val uid = mAuth.currentUser?.uid

        if (uid == null){
            Log.e("QuizResultsActivity", "No user logged in")
        }

        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)

        val quizData = hashMapOf(
            "uid" to uid,
            "date" to formattedDate,
            "questionsRight" to questionsRight,
            "questionsWrong" to questionsWrong
        )

        db.collection("quiz")
            .add(quizData)
            .addOnSuccessListener { documentReference ->
                Log.d("QuizResultsActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("QuizResultsActivity", "Error adding document", e)
            }

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