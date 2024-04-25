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

/**
 * An activity to show quiz results after the completion of a quiz.
 */
class QuizResultsActivity : AppCompatActivity(){

    // Intialises variables for getting and uploading data.
    private val gson = Gson()
    private var mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var quizId: String? = null
    private var questionsRight: Int = 0
    private var questionsWrong: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_result_page)

        // Displays well done message.
        val messageTextView = findViewById<TextView>(R.id.well_done_text)
        messageTextView.text = getRandMessage()

        // Gets the no. of questions right and wrong from previous activity.
        questionsRight = intent.getIntExtra("questionsRight", 0)
        questionsWrong = intent.getIntExtra("questionsWrong", 0)

        // Gets all question attempts.
        var questionAttemptsJson = intent.getStringExtra("questionAttemptsJson")?: ""
        val type = object : TypeToken<List<QuestionAttempt>>() {}.type
        val questionAttempts: List<QuestionAttempt> = gson.fromJson(questionAttemptsJson, type)

        // Displays to screen.
        val questionsRightTextView = findViewById<TextView>(R.id.no_right_text)
        val questionsWrongTextView = findViewById<TextView>(R.id.no_wrong_text)
        questionsRightTextView.text = questionsRight.toString()
        questionsWrongTextView.text = questionsWrong.toString()

        // Array list of photos used to store alongside quiz.
        val selectedPictures: ArrayList<PictureModel> =
            intent.getParcelableArrayListExtra<PictureModel>("selectedPictures")
                ?: arrayListOf()

        // Uses callback so Id for quiz can be gotten after uploaded.
        uploadQuizToDB(object : QuizUploadCallback {
            override fun onQuizUploaded(quizId: String) {

                // Upload and link question attempts.
                val attemptsWithQuizId = questionAttempts.map { it.copy(quizId = quizId) }
                uploadQuestionAttempts(attemptsWithQuizId)

                // Upload images and link to quiz.
                val imageIds = selectedPictures.map { it.documentId}
                addQuizImageLink(quizId, imageIds)
            }
        })
        updateImageDB()

        // Set up recycler view and load used images in to it.
        val imagesRecyclerView = findViewById<RecyclerView>(R.id.pictures_used_recycler_view)
        imagesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = ImagesAdapter(this, selectedPictures)
        imagesRecyclerView.adapter = adapter

        val endQuizButton = findViewById<Button>(R.id.end_quiz_button)
        val notesEditText = findViewById<EditText>(R.id.photo_notes_edit_text)
        val moodToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.mood_toggle_group)

        // On clicking end quiz button, the details entered are uploaded alongside the quiz.
        endQuizButton.setOnClickListener {
            lifecycleScope.launch {
                val selectedMoodButtonId = moodToggleGroup.checkedButtonId
                val selectedMood = findViewById<MaterialButton>(selectedMoodButtonId).text.toString()
                val notes = notesEditText.text.toString()
                updateQuizDetails(selectedMood, notes)
            }
        }

    }

    /**
     * Uploads quiz to database.
     *
     * @param callback used to get the ID right after the quiz has been uploaded.
     */
    private fun uploadQuizToDB(callback: QuizUploadCallback){

        // Gets user id.
        val uid = mAuth.currentUser?.uid
        if (uid == null){
            Log.e("QuizResultsActivity", "No user logged in")
        }

        // Gets other details like date and questions right/wrong to create hash map.
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDate)
        val quizData = hashMapOf(
            "uid" to uid,
            "date" to formattedDate,
            "questionsRight" to questionsRight,
            "questionsWrong" to questionsWrong
        )

        // Create new quiz record.
        db.collection("quizzes")
            .add(quizData)
            .addOnSuccessListener { documentReference ->
                Log.d("QuizResultsActivity", "DocumentSnapshot added with ID: ${documentReference.id}")

                // Use the ID on callback so it can be used for other records.
                this.quizId = documentReference.id
                callback.onQuizUploaded(documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.w("QuizResultsActivity", "Error adding document", e)
            }
    }

    /**
     * Update the image aspect of the database to show how many times the image has been gotten right or wrong.
     */
    private fun updateImageDB() {

        // Mutable maps used to map picture to number right and wrong.
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

        // Map updates the image and add onto the count of how many they have gotten right
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

        // Map updates the image and add onto the count of how many they have gotten wrong
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

    /**
     * Used to link quiz to the image.
     *
     * @param quizId the id of the quiz.
     * @param imageIds the id of the images.
     */
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

    /**
     * A method to update quiz details with the details the user has entered.
     *
     * @param selectedMood the selected mood.
     * @param notes the notes along with the quiz.
     */
    private suspend fun updateQuizDetails(selectedMood: String, notes: String) {
        quizId?.let { id ->
            val updateMap = hashMapOf(
                "mood" to selectedMood,
                "notes" to notes,
            )

            // Attempts to uplaod the hashmap to the quiz with the right quiz id.
            try {
                db.collection("quizzes").document(id)
                    .update(updateMap as Map<String, Any>). await()
                Log.d("QuizResultsActivity", "Quiz details updated successfully with current date and time.")

                // Inside QuizResultsActivity, before finishing the activity
                val sharedPref = this.getSharedPreferences("AppPreferences", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("LastFragment", "HomeFragment")
                    apply()
                }

                // Once uploading has worked, the user is returned to the home menu and the results
                // activity is finished.
                val returnIntent = Intent()
                returnIntent.putExtra("fromQuizResults", true)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()

            } catch (e: Exception) {
                Log.w("QuizResultsActivity", "Error updating quiz details", e)
            }
        } ?: Log.e("QuizResultsActivity", "Quiz ID is null. Cannot update quiz details.")
    }

    /**
     * A method to upload the question attempts from the user.
     *
     * @param questionAttempts a list of the question attempts from the quiz activity.
     */
    private fun uploadQuestionAttempts(questionAttempts: List<QuestionAttempt>) {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        // Goes through each attempt and adds it to the attempts collection.
        questionAttempts.forEach { attempt ->
            val docRef = db.collection("questions").document()
            batch.set(docRef, attempt)
        }

        // Sends off the questions in one batch.
        batch.commit().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Firestore", "Question attempts uploaded successfully")
            } else {
                Log.e("Firestore", "Error uploading question attempts", task.exception)
            }
        }
    }

    /**
     * Used to get the quiz id right after upload.
     */
    interface QuizUploadCallback {
        fun onQuizUploaded(quizId: String)
    }

    /**
     * Gets a random congratulations message for the user.
     */
    private fun getRandMessage(): String{
        val messages = listOf(
            "Great job!",
            "Well done!",
            "Fantastic!"
        )

        return messages.random()
    }
}