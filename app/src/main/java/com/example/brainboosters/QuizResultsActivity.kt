package com.example.brainboosters

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.brainboosters.model.PictureModel
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

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



        //Array list of photos used
        val selectedPictures: ArrayList<PictureModel> =
            intent.getParcelableArrayListExtra<PictureModel>("selectedPictures")
                ?: arrayListOf()

        uploadQuizToDB(object : QuizUploadCallback {
            override fun onQuizUploaded(quizId: String) {
                // Assuming selectedPictures contains the image IDs
                val imageIds = selectedPictures.map { it.documentId}
                addQuizImageLink(quizId, imageIds)
            }
        })
        updateImageDB()

    }

    private fun uploadQuizToDB(callback: QuizUploadCallback){

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

        db.collection("quizzes")
            .add(quizData)
            .addOnSuccessListener { documentReference ->
                Log.d("QuizResultsActivity", "DocumentSnapshot added with ID: ${documentReference.id}")
                callback.onQuizUploaded(documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.w("QuizResultsActivity", "Error adding document", e)
            }
    }

    private fun updateImageDB() {

        //MutableMaps here, use to assign these to pictures in database
        val correctAnswersCountMap: MutableMap<String, Int> = gson.fromJson(
            intent.getStringExtra("correctAnswerJson"), // Corrected key
            object : TypeToken<MutableMap<String, Int>>() {}.type
        ) ?: mutableMapOf()

        val incorrectAnswersCountMap: MutableMap<String, Int> = gson.fromJson(
            intent.getStringExtra("incorrectAnswerJson"), // Corrected key
            object : TypeToken<MutableMap<String, Int>>() {}.type
        ) ?: mutableMapOf()

        correctAnswersCountMap.forEach { (imageUrl, count) ->
            Log.d("QuizResultsTwo", "Image $imageUrl got $count correct answers.")
        }
        incorrectAnswersCountMap.forEach { (imageUrl, count) ->
            Log.d("QuizResultsTwo", "Image $imageUrl got $count incorrect answers.")
        }

        // Assume db is your Firebase Firestore instance
        correctAnswersCountMap.forEach { (documentID, correctCount) ->
            val imageRef = db.collection("images").document(documentID)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(imageRef)
                val currentCorrectCount = snapshot.getLong("correctCount") ?: 0
                transaction.update(imageRef, "correctCount", currentCorrectCount + correctCount)
            }.addOnSuccessListener {
                Log.d("QuizResultsActivity", "Transaction success: Correct count updated for $documentID")
            }.addOnFailureListener { e ->
                Log.w("QuizResultsActivity", "Transaction failure.", e)
            }
        }

        incorrectAnswersCountMap.forEach { (documentID, incorrectCount) ->
            val imageRef = db.collection("images").document(documentID)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(imageRef)
                val currentIncorrectCount = snapshot.getLong("incorrectCount") ?: 0
                transaction.update(imageRef, "incorrectCount", currentIncorrectCount + incorrectCount)
            }.addOnSuccessListener {
                Log.d("QuizResultsActivity", "Transaction success: Incorrect count updated for $documentID")
            }.addOnFailureListener { e ->
                Log.w("QuizResultsActivity", "Transaction failure.", e)
            }
        }
    }

    private fun addQuizImageLink(quizId: String, imageIds: List<String?>) {
        imageIds.forEach { imageId ->
            val link = hashMapOf("quizId" to quizId, "imageId" to imageId)
            db.collection("quizImageLinks").add(link)
                .addOnSuccessListener {
                    Log.d("QuizResultsActivity", "Link added between quiz $quizId and image $imageId")
                }
                .addOnFailureListener { e ->
                    Log.w("QuizResultsActivity", "Error adding quiz-image link", e)
                }
        }
    }

    interface QuizUploadCallback {
        fun onQuizUploaded(quizId: String)
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