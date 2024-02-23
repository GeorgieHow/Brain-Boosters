package com.example.brainboosters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.example.brainboosters.model.PictureModel
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.brainboosters.accessibility.AccessibleZoomImageView
import com.google.gson.Gson
import java.util.Locale
import kotlinx.coroutines.*
import javax.sql.DataSource

//Questions up here
data class Question(
    val questionText: String,
    val options: List<String?>,
    val correctAnswer: String?,
    val pictureModel: PictureModel
)

class QuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var selectedPictures: List<PictureModel>

    private lateinit var questions: List<Question>
    private var currentQuestionIndex = 0

    private lateinit var textToSpeech: TextToSpeech
    private var isTTSInitialized = false

    private lateinit var questionTitle: TextView

    private var questionsRight = 0
    private var questionsWrong = 0

    private var correctAnswersCountMap: MutableMap<String, Int> = mutableMapOf()
    private var incorrectAnswersCountMap: MutableMap<String, Int> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {

        //For making it full screen
        hideSystemUI()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_question_layout)

        initializeUIForLoading()

        textToSpeech = TextToSpeech(this, this)

        selectedPictures = intent
            .getParcelableArrayListExtra<PictureModel>("selectedPictures")
            ?: arrayListOf<PictureModel>()
        questions = getQuestions(selectedPictures)
        loadQuestion()

        val textToSpeechButton = findViewById<Button>(R.id.text_to_speech_button)
        textToSpeechButton.setOnClickListener {
            speakOut(questionTitle.text.toString())
        }

        val answerButtons = getListOfButtons()

        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val selectedAnswer = button.text.toString()
                showAnswerPreviewDialog(selectedAnswer) { confirmSelection ->
                    if (confirmSelection) {
                        evaluateAnswer(selectedAnswer, button)
                    }
                }
            }
        }
    }

    private fun initializeUIForLoading() {
        // Set the ProgressBar to visible
        findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.VISIBLE
        // Hide quiz content, e.g., buttons, text views, etc.
        // Example:
        findViewById<TextView>(R.id.question_title).visibility = View.GONE
        findViewById<Button>(R.id.text_to_speech_button).visibility = View.GONE
        findViewById<ImageView>(R.id.picture_image_view).visibility = View.GONE
        findViewById<Button>(R.id.answer_1_button).visibility = View.GONE
        findViewById<Button>(R.id.answer_2_button).visibility = View.GONE
        findViewById<Button>(R.id.answer_3_button).visibility = View.GONE
        findViewById<Button>(R.id.answer_4_button).visibility = View.GONE
        // Repeat for other quiz elements...
    }

    private fun getQuestions(selectedPictures: List<PictureModel>): List<Question> {

        //If only one picture is picked
        if (selectedPictures.size == 1){
            val questions = mutableListOf(
                Question("Where was this taken?", listOf("Option 1", "Option 2",
                    "Option 3", selectedPictures[0].imagePlace), selectedPictures[0].imagePlace,
                    selectedPictures[0]),
                Question("What year was this taken?", listOf("Option 1", "Option 2",
                    "Option 3", selectedPictures[0].imageYear.toString()),
                    selectedPictures[0].imageYear.toString(),
                    selectedPictures[0]),
                Question("What event was taking place?", listOf("Option 1", "Option 2",
                    "Option 3", selectedPictures[0].imageEvent.toString()),
                    selectedPictures[0].imageEvent.toString(),
                    selectedPictures[0])
            )

            // Add the question about the person if 'imagePerson' is not empty or null
            if (!selectedPictures[0].imagePerson.isNullOrEmpty()) {
                questions.add(
                    Question("Who is in this photo?", listOf("Option 1", "Option 2",
                        "Option 3", selectedPictures[0].imagePerson), selectedPictures[0].imagePerson,
                        selectedPictures[0])
                )
            }

            // Return the list of questions
            return questions
        }
        //If multiple pictures ate picked
        else {
            val allQuestions = mutableListOf<Question>()
            val minimumQuestions = 5
            var questionCount = 0

            while (questionCount < minimumQuestions) {
                selectedPictures.forEach { picture ->
                    if(questionCount < minimumQuestions) {
                        val questionsForPicture = mutableListOf(
                            Question("Where was this taken?",
                                listOf("Option 1", "Option 2", "Option 3", picture.imagePlace),
                                picture.imagePlace, picture),
                            Question("What year was this taken?",
                                listOf("Option 1", "Option 2", "Option 3",
                                    picture.imageYear.toString()), picture.imageYear.toString(),
                                picture)
                        )
                        // Only add the question about the person if 'imagePerson' is not empty
                        if (picture.imagePerson?.isNotEmpty() == true) {
                            questionsForPicture.add(
                                Question("Who is in this photo?",
                                    listOf("Option 1", "Option 2", "Option 3", picture.imagePerson),
                                    picture.imagePerson, picture)
                            )
                        }
                        // Randomly select a question from the list for each picture
                        allQuestions.add(questionsForPicture.random())
                        questionCount++
                    }
                }
            }

            // Shuffle the list to ensure randomness in question order
            allQuestions.shuffle()

            // If you end up with more questions than needed due to the loop, trim the list to the desired size
            return if (allQuestions.size > minimumQuestions) allQuestions.take(minimumQuestions) else allQuestions
        }
    }

    private fun goToNextQuestion() {
        // Check if more questions are available
        if (currentQuestionIndex + 1 < questions.size) {
            currentQuestionIndex++ // Increment the question index
            GlobalScope.launch(Dispatchers.Main) {
                delay(5000L)
                loadQuestion() // Load the next question
            }
        } else {
            // Handle the case where there are no more questions (e.g., show results or restart the quiz)
            Toast.makeText(this, "You've reached the end of the quiz! You got $questionsRight right and $questionsWrong wrong", Toast.LENGTH_SHORT).show()
            correctAnswersCountMap.forEach { (documentID, count) ->
                Log.d("QuizResults", "Image $documentID got $count correct answers.")
            }
            incorrectAnswersCountMap.forEach { (documentID, count) ->
                Log.d("QuizResults", "Image $documentID got $count incorrect answers.")
            }


            //relevant to results page, what I am parsing through
            val gson = Gson()

            val correctAnswerJson = gson.toJson(correctAnswersCountMap)
            val incorrectAnswerJson = gson.toJson(incorrectAnswersCountMap)

            val intent = Intent(this, QuizResultsActivity::class.java).apply{
                putExtra("questionsRight", questionsRight)
                putExtra("questionsWrong", questionsWrong)

                putExtra("correctAnswerJson", correctAnswerJson)
                putExtra("incorrectAnswerJson", incorrectAnswerJson)

                putParcelableArrayListExtra("selectedPictures", ArrayList(selectedPictures))

            }
            startActivity(intent)
            finish()
        }
    }


    private fun loadQuestion(){
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]

            questionTitle = findViewById<TextView>(R.id.question_title)
            questionTitle.text = question.questionText

            //Question Picture
            val pictureImageView = findViewById<AccessibleZoomImageView>(R.id.picture_image_view)


            Glide.with(this)
                .load(question.pictureModel.imageUrl)
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // This is called when the image is ready. Perform your actions here.
                        pictureImageView.post {
                            if (pictureImageView is AccessibleZoomImageView) {
                                pictureImageView.resetZoom()
                            }
                        }
                        return false
                    }
                })
                .into(pictureImageView)

            resetButtonColors()


            val shuffledAnswers = question.options.shuffled()

            //Answers
            findViewById<Button>(R.id.answer_1_button).text = shuffledAnswers[0]
            findViewById<Button>(R.id.answer_2_button).text = shuffledAnswers[1]
            findViewById<Button>(R.id.answer_3_button).text = shuffledAnswers[2]
            findViewById<Button>(R.id.answer_4_button).text = shuffledAnswers[3]


        }
    }

    private fun resetButtonColors() {
        val buttons = getListOfButtons()

        buttons.forEach { button ->
            val defaultQuizButton = ContextCompat.getDrawable(this, R.drawable.quiz_answer_outline_default)
            val textColor = ContextCompat.getColor(this, R.color.inverseSurfaceColor)
            button.background = defaultQuizButton
            button.setTextColor(textColor)
        }

    }

    private fun showAnswerPreviewDialog(answer: String, onConfirmSelection: (Boolean) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.confirm_answer_popup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dialog from getting dismissed by back press or outside touch
            .create()

        val answerText = dialogView.findViewById<TextView>(R.id.preview_answer_text)
        val selectAnswerButton = dialogView.findViewById<Button>(R.id.select_answer_button)
        val goBackButton = dialogView.findViewById<Button>(R.id.go_back_button)

        answerText.text = answer

        // Use TTS to read out the answer
        speakOut(answer)

        selectAnswerButton.setOnClickListener {
            onConfirmSelection(true)
            dialog.dismiss()
        }

        goBackButton.setOnClickListener {
            onConfirmSelection(false)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun evaluateAnswer(selectedAnswer: String, button: Button) {
        val currentQuestion = questions[currentQuestionIndex]

        val answerButtons = getListOfButtons()

        if (selectedAnswer == currentQuestion.correctAnswer) {
            //Changes button green
            button.setBackgroundResource(R.drawable.quiz_answer_outline_correct)
            val color = ContextCompat.getColor(this, R.color.surfaceColor)
            button.setTextColor(color)

            questionsRight++

            // Increment the correct answer count for the current PictureModel
            val currentCount = correctAnswersCountMap[currentQuestion.pictureModel.documentId] ?: 0
            correctAnswersCountMap[currentQuestion.pictureModel.documentId!!] = currentCount + 1

        } else {
            //Changes button red
            button.setBackgroundResource(R.drawable.quiz_answer_outline_wrong)
            val color = ContextCompat.getColor(this, R.color.surfaceColor)
            button.setTextColor(color)

            //Changes button with right answer green
            answerButtons.find { it.text == currentQuestion.correctAnswer }?.let { button ->
                button.setBackgroundResource(R.drawable.quiz_answer_outline_correct)
                val color = ContextCompat.getColor(this, R.color.surfaceColor)
                button.setTextColor(color)
            }

            questionsWrong++

            val currentCount = incorrectAnswersCountMap[currentQuestion.pictureModel.documentId] ?: 0
            incorrectAnswersCountMap[currentQuestion.pictureModel.documentId!!] = currentCount + 1
        }

        goToNextQuestion()
    }

    private fun getListOfButtons(): List<Button> {
        return listOf(
            findViewById<Button>(R.id.answer_1_button),
            findViewById<Button>(R.id.answer_2_button),
            findViewById<Button>(R.id.answer_3_button),
            findViewById<Button>(R.id.answer_4_button)
        )
    }


    //TEXT TO SPEECH METHODS BELOW
    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for the TextToSpeech engine
            val result = textToSpeech.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.LENGTH_LONG
            } else {
                // Speak out a message as soon as the TTS engine is initialized successfully
                speakOut("Text to speech enabled.")
                isTTSInitialized = true
                //speakOut(questionTitle.text.toString())
                Log.d("Working", "Boy what the hell boy")
            }
        } else {
            // Initialization failed, handle the error
            // This might be a good place to log the error or inform the user that TTS is not available.
            Log.d("Not working", "wont even intialise BRUH")
        }
    }

    //Destorys Text to Speech after its been used
    override fun onDestroy() {
        // Shut down TTS
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }


    //Method for hiding system UI
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}
