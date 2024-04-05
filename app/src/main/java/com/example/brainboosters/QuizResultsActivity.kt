package com.example.brainboosters

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.brainboosters.model.PictureModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class QuizResultsActivity : AppCompatActivity(){

    //For mutable maps ;P
    private val gson = Gson()

    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var quizId: String? = null

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

        //Full Question Details Unpacked
        var questionAttemptsJson = intent.getStringExtra("questionAttemptsJson")?: ""
        val type = object : TypeToken<List<QuestionAttempt>>() {}.type
        val questionAttempts: List<QuestionAttempt> = gson.fromJson(questionAttemptsJson, type)

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
                //Change quizid on attempts to quiz id just uploaded
                val attemptsWithQuizId = questionAttempts.map { it.copy(quizId = quizId) }
                uploadQuestionAttempts(attemptsWithQuizId)

                val imageIds = selectedPictures.map { it.documentId}
                addQuizImageLink(quizId, imageIds)
            }
        })
        updateImageDB()

        val imagesRecyclerView = findViewById<RecyclerView>(R.id.pictures_used_recycler_view)
        imagesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Use the adapter with PictureModel list
        val adapter = ImagesAdapter(this, selectedPictures)
        imagesRecyclerView.adapter = adapter

        // Initialize your UI components here...
        val endQuizButton = findViewById<Button>(R.id.end_quiz_button)
        val notesEditText = findViewById<EditText>(R.id.photo_notes_edit_text)
        val moodToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.mood_toggle_group)

        endQuizButton.setOnClickListener {
            lifecycleScope.launch {
                val selectedMoodButtonId = moodToggleGroup.checkedButtonId
                val selectedMood = findViewById<MaterialButton>(selectedMoodButtonId).text.toString()
                val notes = notesEditText.text.toString()

                // Call the suspend function to update the Firestore document
                updateQuizDetails(selectedMood, notes)
            }
        }

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
                this.quizId = documentReference.id
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
    private suspend fun updateQuizDetails(selectedMood: String, notes: String) {
        quizId?.let { id ->
            val updateMap = hashMapOf(
                "mood" to selectedMood,
                "notes" to notes,
            )

            try {
                db.collection("quizzes").document(id)
                    .update(updateMap as Map<String, Any>). await()
                Log.d("QuizResultsActivity", "Quiz details updated successfully with current date and time.")

                // Inside QuizResultsActivity, before finishing the activity
                val sharedPref = this.getSharedPreferences("AppPreferences", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("LastFragment", "HomeFragment") // Use a key like "LastFragment" to remember the desired fragment
                    apply()
                }

                val returnIntent = Intent()
                returnIntent.putExtra("fromQuizResults", true)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()

            } catch (e: Exception) {
                Log.w("QuizResultsActivity", "Error updating quiz details", e)
            }
        } ?: Log.e("QuizResultsActivity", "Quiz ID is null. Cannot update quiz details.")
    }

    private fun uploadQuestionAttempts(questionAttempts: List<QuestionAttempt>) {
        val db = FirebaseFirestore.getInstance()

        // Optional: Use a batch if you want to upload all attempts atomically
        val batch = db.batch()

        questionAttempts.forEach { attempt ->
            // Create a new document reference for each attempt in the "questionAttempts" collection
            val docRef = db.collection("questions").document()

            // Convert your QuestionAttempt object into a Map or let Firestore handle the serialization
            batch.set(docRef, attempt)
        }

        // Commit the batch
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firestore", "Question attempts uploaded successfully")
            } else {
                Log.e("Firestore", "Error uploading question attempts", task.exception)
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