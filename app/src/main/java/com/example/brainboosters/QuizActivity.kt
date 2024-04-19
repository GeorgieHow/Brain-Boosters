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
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.util.Locale
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig

//Questions up here
enum class QuestionType{
    LONG_TERM, SHORT_TERM
}

data class Question(
    val questionText: String,
    val options: List<String?>,
    val correctAnswer: String?,
    val pictureModel: PictureModel,
    val questionType: QuestionType,
    val questionArea: String
)

data class QuestionAttempt(
    val uid: String,
    val quizId: String,
    val questionText: String,
    val correctAnswer: String?,
    val userAnswer: String?,
    val questionType: QuestionType,
    val questionArea: String,
    val isCorrect: Boolean
)

class QuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "N/A"

    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var selectedPictures: List<PictureModel>

    private lateinit var questions: MutableList<Question>
    private var currentQuestionIndex = 0

    private lateinit var textToSpeech: TextToSpeech
    private var isTTSInitialized = false

    private lateinit var questionTitle: TextView

    private var questionsRight = 0
    private var questionsWrong = 0

    private var correctAnswersCountMap: MutableMap<String, Int> = mutableMapOf()
    private var incorrectAnswersCountMap: MutableMap<String, Int> = mutableMapOf()

    private var questionAttempts = mutableListOf<QuestionAttempt>()

    private val config = fakerConfig { locale = "en-GB" }
    private val faker = Faker(config)

    override fun onCreate(savedInstanceState: Bundle?) {


        //For making it full screen
        hideSystemUI()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_question_layout)


        Log.wtf("QuizDebugging", "Started Quiz.")

        initializeUIForLoading()

        textToSpeech = TextToSpeech(this, this)

        generateRememberNumber()
        showRememberNumberDialog()

        val quizType = intent.getStringExtra("quizType")

        selectedPictures = intent
            .getParcelableArrayListExtra<PictureModel>("selectedPictures")
            ?: arrayListOf<PictureModel>()


        activityScope.launch {
            questions = getQuestions(selectedPictures).toMutableList()
            addRecallQuestionToEnd()
            loadQuestion()
            updateUIForLoadedContent()
        }

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

        hideNextButton()

        findViewById<TextView>(R.id.question_title).visibility = View.GONE
        findViewById<Button>(R.id.text_to_speech_button).visibility = View.GONE
        findViewById<ImageView>(R.id.picture_image_view).visibility = View.GONE
        findViewById<Button>(R.id.answer_1_button).visibility = View.GONE
        findViewById<Button>(R.id.answer_2_button).visibility = View.GONE
        findViewById<Button>(R.id.answer_3_button).visibility = View.GONE
        findViewById<Button>(R.id.answer_4_button).visibility = View.GONE
    }

    private fun updateUIForLoadedContent() {
        // Hide the ProgressBar
        findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.GONE

        // Show quiz content
        findViewById<TextView>(R.id.question_title).visibility = View.VISIBLE
        findViewById<Button>(R.id.text_to_speech_button).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.picture_image_view).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_1_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_2_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_3_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_4_button).visibility = View.VISIBLE
    }

    private fun generateFakePlaces(count: Int): List<String> {
        return List(count) { faker.address.city()}
    }

    private fun generateFakeYears(count: Int): List<String> {
        val currentYear = java.time.Year.now().value
        return List(count) { (1900..currentYear).random().toString() }
    }

    private fun generateFakeEvents(count: Int): List<String> {
        val events = listOf("Festival", "Conference", "Exhibition", "Marathon")
        return List(count) { "${faker.job.field().capitalize()} ${events.random()}" }
    }

    private fun generateFakePersons(count: Int): List<String> {
        return List(count) { faker.name.name() }
    }

    private suspend fun getQuestions(selectedPictures: List<PictureModel>): List<Question> {
        val imagesCollectionPath = "images"

        //If only one picture is picked
        if (selectedPictures.size == 1){
            // Fetch additional images from Firestore to use as options
            val additionalImagesPlace = fetchAdditionalImageData(imagesCollectionPath, "place")
            val additionalImagesYear = fetchAdditionalImageData(imagesCollectionPath, "year")
            val additionalImagesEvent = fetchAdditionalImageData(imagesCollectionPath, "event")
            val additionalImagesPerson = fetchAdditionalImageData(imagesCollectionPath, "person")

            val questions = mutableListOf<Question>()
            selectedPictures.forEach { picture ->
                // Initialize a set to ensure options are unique
                val optionsSetPlace = mutableSetOf<String>()
                val optionsSetYear = mutableSetOf<String>()
                val optionsSetEvent = mutableSetOf<String>()

                // Add the correct answer first to ensure it's included
                picture.imagePlace?.let { optionsSetPlace.add(it) }
                picture.imageYear?.let { optionsSetYear.add(it.toString()) }
                picture.imageEvent?.let { optionsSetEvent.add(it) }

                Log.wtf("QuizDebugging", "Correct place added: ${optionsSetPlace.joinToString()}")

                // Add additional, randomly shuffled options, avoiding duplicates
                additionalImagesPlace.shuffled().forEach { place ->
                    if (optionsSetPlace.size < 4) { // Assuming you need 4 options
                        optionsSetPlace.add(place)
                    }
                }

                additionalImagesYear.shuffled().forEach {year ->
                    if (optionsSetYear.size < 4){
                        optionsSetYear.add(year)
                    }
                }

                additionalImagesEvent.shuffled().forEach {event ->
                    if (optionsSetEvent.size < 4){
                        optionsSetEvent.add(event)
                    }
                }

                Log.wtf("QuizDebugging", "Final places set: ${optionsSetPlace.joinToString()}")

                // Convert the set back to a list and shuffle it to ensure random order
                val optionsListPlace = optionsSetPlace.toList().shuffled()
                val optionsListYear = optionsSetYear.toList().shuffled()
                val optionsListEvent = optionsSetEvent.toList().shuffled()

                Log.wtf("QuizDebugging", "Options list for place (shuffled): ${optionsListPlace.joinToString()}")


                questions.add(
                    Question(
                        questionText = "Where was this taken?",
                        options = optionsListPlace,
                        correctAnswer = picture.imagePlace,
                        pictureModel = picture,
                        questionType = QuestionType.LONG_TERM,
                        questionArea = "place"
                    )
                )

                questions.add(
                    Question(
                        questionText = "What year was this taken?",
                        options = optionsListYear,
                        correctAnswer = picture.imageYear.toString(),
                        pictureModel = picture,
                        questionType = QuestionType.LONG_TERM,
                        questionArea = "year"
                    )
                )

                questions.add(
                    Question(
                        questionText = "What event was taking place?",
                        options = optionsListEvent,
                        correctAnswer = picture.imageYear.toString().toString(),
                        pictureModel = picture,
                        questionType = QuestionType.LONG_TERM,
                        questionArea = "event"
                    )
                )

                picture.imagePerson?.let { person ->
                    val optionsSetPerson = mutableSetOf<String>()
                    optionsSetPerson.add(person) // Add the correct answer

                    // Add additional, randomly shuffled options, avoiding duplicates
                    additionalImagesPerson.shuffled().forEach { person ->
                        if (optionsSetPerson.size < 4) { // Assuming you need 4 options
                            optionsSetPerson.add(person)
                        }
                    }
                    

                    // Convert the set back to a list and shuffle it to ensure random order
                    val optionsListWho = optionsSetPerson.toList().shuffled()

                    questions.add(
                        Question(
                            questionText = "Who is in this photo?",
                            options = optionsListWho,
                            correctAnswer = person,
                            pictureModel = picture,
                            questionType = QuestionType.LONG_TERM,
                            questionArea = "who"
                        )
                    )
                }
            }

            // Return the list of questions
            return questions
        }
        //If multiple pictures are picked
        else {
            val additionalPlaces = fetchAdditionalImageData(imagesCollectionPath, "place").shuffled().distinct()
            val additionalYears = fetchAdditionalImageData(imagesCollectionPath, "year").shuffled().distinct()
            val additionalPersons = fetchAdditionalImageData(imagesCollectionPath, "person").shuffled().distinct()
            val additionalEvents = fetchAdditionalImageData(imagesCollectionPath, "event").shuffled().distinct()

            val allQuestions = mutableListOf<Question>()
            val minimumQuestions = 5
            val usedQuestionsForPicture = mutableMapOf<String, MutableList<String>>() // Track used questions for each picture

            // Function to create and add a question if it's unique for the picture
            fun addQuestionWithRealOptions(picture: PictureModel, category: String, correctAnswer: String, additionalOptions: List<String>) {
                val usedQuestions = usedQuestionsForPicture.getOrDefault(picture.documentId, mutableListOf())
                if (!usedQuestions.contains(category)) {
                    val filteredOptions = additionalOptions.filterNot { it == correctAnswer }.take(3)
                    val options = (filteredOptions + correctAnswer).shuffled()
                    val questionText = when (category) {
                        "place" -> "Where was this taken?"
                        "year" -> "What year was this taken?"
                        "person" -> "Who is in this photo?"
                        "event" -> "What event was taking place?"
                        else -> ""
                    }

                    val questionArea = category

                    val question = Question(questionText, options, correctAnswer, picture, QuestionType.LONG_TERM, category)
                    allQuestions.add(question)
                    usedQuestions.add(category)
                    usedQuestionsForPicture[picture.documentId ?: return] = usedQuestions
                }
            }

            selectedPictures.shuffled().forEach { picture ->
                if (allQuestions.size >= minimumQuestions) return@forEach // Break if we have enough questions

                addQuestionWithRealOptions(picture, "place", picture.imagePlace ?: "", additionalPlaces)
                addQuestionWithRealOptions(picture, "year", picture.imageYear?.toString() ?: "", additionalYears)
                addQuestionWithRealOptions(picture, "event", picture.imageEvent?: "", additionalEvents)
                if (picture.imagePerson?.isNotEmpty() == true) {
                    addQuestionWithRealOptions(picture, "person", picture.imagePerson, additionalPersons)
                }
            }

// Shuffle the list to ensure randomness in question order
            allQuestions.shuffle()

// Trim the list to the desired size if necessary
            return if (allQuestions.size > minimumQuestions) allQuestions.take(minimumQuestions) else allQuestions
        }
    }



    private lateinit var rememberNumber: String
    private val distractorNumbers = mutableListOf<String>()

    private fun generateRememberNumber() {
        val randomNumber = (10..99).random().toString() // Generates a random 4-digit number
        rememberNumber = randomNumber

        // Generate distractor numbers
        distractorNumbers.clear()
        while (distractorNumbers.size < 3) {
            val distractor = (10..99).random().toString()
            if (distractor != rememberNumber && distractor !in distractorNumbers) {
                distractorNumbers.add(distractor)
            }
        }
    }

    private fun addRecallQuestionToEnd() {
        val options = mutableListOf(rememberNumber).apply { addAll(distractorNumbers) }.shuffled()
        val recallQuestion = Question(
            questionText = "What was the number shown at the start of the quiz?",
            options = options,
            correctAnswer = rememberNumber,
            pictureModel = PictureModel.EMPTY, // Use the placeholder for consistency
            questionType = QuestionType.SHORT_TERM,
            questionArea = "number"
        )

        // Assuming 'questions' is your list of quiz questions
        questions.add(recallQuestion) // Add this as the last question
    }


    private suspend fun fetchAdditionalImageData(imagesCollectionPath: String, field: String): List<String> {
        return suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection(imagesCollectionPath)
                .get()
                .addOnSuccessListener { documents ->
                    val data = documents.mapNotNull { document ->
                        val fieldValue = document.get(field)
                        when (fieldValue) {
                            is String -> fieldValue
                            is Long -> fieldValue.toString()
                            is Double -> fieldValue.toString()
                            else -> null
                        }
                    }
                    continuation.resume(data)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWith(Result.failure(exception))
                }
        }
    }

    private fun goToNextQuestion() {
        if (currentQuestionIndex + 1 < questions.size) {
            currentQuestionIndex++ // Increment the question index
            //GlobalScope.launch(Dispatchers.Main) {
                //delay(5000L)
                loadQuestion() // Load the next question
            //}
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
            val questionAttemptsJson = gson.toJson(questionAttempts)

            val intent = Intent(this, QuizResultsActivity::class.java).apply{
                putExtra("questionsRight", questionsRight)
                putExtra("questionsWrong", questionsWrong)

                putExtra("correctAnswerJson", correctAnswerJson)
                putExtra("incorrectAnswerJson", incorrectAnswerJson)

                putExtra("questionAttemptsJson", questionAttemptsJson)

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
            val buttons = getListOfButtons()
            buttons.forEachIndexed { index, button ->
                button.text = shuffledAnswers.getOrNull(index) ?: ""
            }
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

    private fun showRememberNumberDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.remember_number_popup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // This dialog should not be dismissible without choosing an option
            .create()

        val numberToRememberText = dialogView.findViewById<TextView>(R.id.number_to_remember_text)
        val nextButton = dialogView.findViewById<Button>(R.id.next_button)


        numberToRememberText.text = "Remember this number:\n$rememberNumber"
        numberToRememberText.setTextColor(ContextCompat.getColor(this, R.color.white))

        // Use TTS to read out the number
        speakOut("Remember this number: $rememberNumber")

        nextButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun evaluateAnswer(selectedAnswer: String, button: Button) {

        disableAnswerButtons()

        val currentQuestion = questions[currentQuestionIndex]

        val isCorrect = selectedAnswer == currentQuestion.correctAnswer
        val attempt = QuestionAttempt(
            uid = uid,
            quizId = "",
            questionText = currentQuestion.questionText,
            correctAnswer = currentQuestion.correctAnswer,
            userAnswer = selectedAnswer,
            questionType = currentQuestion.questionType,
            questionArea = currentQuestion.questionArea,
            isCorrect = isCorrect
        )
        questionAttempts.add(attempt)

        val answerButtons = getListOfButtons()

        if (selectedAnswer == currentQuestion.correctAnswer) {
            //Changes button green
            button.setBackgroundResource(R.drawable.quiz_answer_outline_correct)
            val color = ContextCompat.getColor(this, R.color.surfaceColor)
            button.setTextColor(color)

            questionsRight++

            // Increment the correct answer count for the current PictureModel
            // Safely handle nullable documentId
            val documentId = currentQuestion.pictureModel.documentId ?: "unknown"
            val currentCount = correctAnswersCountMap[documentId] ?: 0
            correctAnswersCountMap[documentId] = currentCount + 1

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

            val documentId = currentQuestion.pictureModel.documentId ?: "unknown"
            val currentCount = incorrectAnswersCountMap[documentId] ?: 0
            incorrectAnswersCountMap[documentId] = currentCount + 1
        }

        enableNextButton()
    }

    private fun getListOfButtons(): List<Button> {
        return listOf(
            findViewById<Button>(R.id.answer_1_button),
            findViewById<Button>(R.id.answer_2_button),
            findViewById<Button>(R.id.answer_3_button),
            findViewById<Button>(R.id.answer_4_button)
        )
    }

    private fun enableAnswerButtons() {
        val buttons = getListOfButtons()
        buttons.forEach { it.isEnabled = true }
    }

    private fun disableAnswerButtons() {
        val buttons = getListOfButtons()
        buttons.forEach { it.isEnabled = false }
    }

    private fun enableNextButton() {
        findViewById<Button>(R.id.next_button).visibility = View.VISIBLE
    }

    private fun hideNextButton() {
        findViewById<Button>(R.id.next_button).visibility = View.GONE
    }

    fun onNextButtonClick(view: View) {
        hideNextButton()
        enableAnswerButtons()
        goToNextQuestion()
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
        activityScope.cancel()
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
