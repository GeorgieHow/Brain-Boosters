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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.example.brainboosters.accessibility.AccessibleZoomImageView
import com.example.brainboosters.adapter.QuizReorderPicturesAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.util.Locale
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import java.util.Collections

/**
 * Question types defined here.
 */
enum class QuestionType{
    LONG_TERM, SHORT_TERM
}

/**
 * Question layout defined here.
 */
data class Question(
    val questionText: String,
    val options: List<String?>,
    val correctAnswer: String?,
    val pictureModel: PictureModel,
    val questionType: QuestionType,
    val questionArea: String
)

/**
 * Question attempt defined here.
 */
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

/**
 * The class which handles the actual quiz acitivty, like loading questions and evaluating answers.
 */
class QuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    var uid = FirebaseAuth.getInstance().currentUser?.uid ?: "N/A"

    // Sets up coroutines so quiz does not load before extra details have been fetched.
    private val activityScope = CoroutineScope(Job() + Dispatchers.Main)

    // Sets up pictures and questions.
    private lateinit var selectedPictures: List<PictureModel>
    private lateinit var pictureSequence: MutableList<PictureModel>
    private lateinit var questions: MutableList<Question>
    private var currentQuestionIndex = 0

    // Sets up text to speech.
    lateinit var textToSpeech: TextToSpeech
    private var isTTSInitialized = false

    // Sets up question relevant details.
    private lateinit var questionTitle: TextView
    private var questionsRight = 0
    private var questionsWrong = 0
    private var correctAnswersCountMap: MutableMap<String, Int> = mutableMapOf()
    private var incorrectAnswersCountMap: MutableMap<String, Int> = mutableMapOf()
    private var questionAttempts = mutableListOf<QuestionAttempt>()

    private val config = fakerConfig { locale = "en-GB" }
    private val faker = Faker(config)

    fun setFirebaseAuth(mockAuth: FirebaseAuth) {
        this.uid = mockAuth.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        hideSystemUI()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quiz_question_layout)

        // Creates empty list to keep track of order of photos.
        pictureSequence = mutableListOf()

        // Starts to set up quiz & initialises text to speech.
        initializeUIForLoading()
        textToSpeech = TextToSpeech(this, this)
        generateRememberNumber()
        showRememberNumberDialog()

        val quizType = intent.getStringExtra("quizType")

        // Gets pictures parsed through to activity.
        selectedPictures = intent
            .getParcelableArrayListExtra<PictureModel>("selectedPictures")
            ?: arrayListOf<PictureModel>()

        // Launches coroutine to set up questions and fetch extra data.
        activityScope.launch {
            val (questionsList, usedPictureSet) = getQuestions(selectedPictures)
            questions = questionsList.toMutableList()
            pictureSequence.addAll(usedPictureSet)

            addReorderPicturesQuestion()
            addRecallQuestionToEnd()
            loadQuestion()
            updateUIForLoadedContent()
        }

        // Creates text to speech button for speaking out questions.
        val textToSpeechButton = findViewById<Button>(R.id.text_to_speech_button)
        textToSpeechButton.setOnClickListener {
            speakOut(questionTitle.text.toString())
        }

        // Gets answer buttons.
        val answerButtons = getListOfButtons()

        // For everytime one of the buttons is clicked, evaluate the answer given.
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                val selectedAnswer = button.text.toString()
                // Pop up to confirm the answer.
                showAnswerPreviewDialog(selectedAnswer) { confirmSelection ->
                    if (confirmSelection) {
                        evaluateAnswer(selectedAnswer, button)
                    }
                }
            }
        }

        // A submit button is established for the reorder question.
        val submitOrderButton = findViewById<Button>(R.id.submit_order_button)
        submitOrderButton.setOnClickListener {
            evaluateAnswer("", submitOrderButton)
        }
    }

    /**
     * A method to hide everything, and wait for the quiz to load.
     */
    private fun initializeUIForLoading() {
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

    /**
     * A method to update the UI once all the quiz details have been fetched.
     */
    private fun updateUIForLoadedContent() {
        findViewById<ProgressBar>(R.id.loading_progress_bar).visibility = View.GONE
        findViewById<TextView>(R.id.question_title).visibility = View.VISIBLE
        findViewById<Button>(R.id.text_to_speech_button).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.picture_image_view).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_1_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_2_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_3_button).visibility = View.VISIBLE
        findViewById<Button>(R.id.answer_4_button).visibility = View.VISIBLE
    }


    /**
     * A method to fetch the questions for the quiz.
     *
     * @params selectedPictures The pictures that have been selected by the user.
     * @return A pair which contains a list of questions and the picture model they relate to.
     */
    private suspend fun getQuestions(selectedPictures: List<PictureModel>): Pair<List<Question>, Set<PictureModel>> {
        val imagesCollectionPath = "images"

        // If the user has only picked one image.
        if (selectedPictures.size == 1){

            // Fetches additional data for the other options.
            val additionalImagesPlace = fetchAdditionalImageData(imagesCollectionPath, "place", uid)
            val additionalImagesYear = fetchAdditionalImageData(imagesCollectionPath, "year", uid)
            val additionalImagesEvent = fetchAdditionalImageData(imagesCollectionPath, "event", uid)
            val additionalImagesPerson = fetchAdditionalImageData(imagesCollectionPath, "person", uid)
            val questions = mutableListOf<Question>()
            val usedPictures = mutableSetOf<PictureModel>()

            // For each picture selected.
            selectedPictures.forEach { picture ->
                // Initialize a set to ensure options are unique.
                val optionsSetPlace = mutableSetOf<String>()
                val optionsSetYear = mutableSetOf<String>()
                val optionsSetEvent = mutableSetOf<String>()

                // Add the correct answer first to ensure it's included.
                picture.imagePlace?.let { optionsSetPlace.add(it) }
                picture.imageYear?.let { optionsSetYear.add(it.toString()) }
                picture.imageEvent?.let { optionsSetEvent.add(it) }

                // Add additional places, randomly shuffled options, avoiding duplicates.
                additionalImagesPlace.shuffled().forEach { place ->
                    if (optionsSetPlace.size < 4) {
                        optionsSetPlace.add(place)
                    }
                }

                // Add additional years, randomly shuffled options, avoiding duplicates.
                additionalImagesYear.shuffled().forEach {year ->
                    if (optionsSetYear.size < 4){
                        optionsSetYear.add(year)
                    }
                }

                // Add additional events, randomly shuffled options, avoiding duplicates.
                additionalImagesEvent.shuffled().forEach {event ->
                    if (optionsSetEvent.size < 4){
                        optionsSetEvent.add(event)
                    }
                }


                // Convert the set back to a list and shuffle it to ensure random order
                val optionsListPlace = optionsSetPlace.toList().shuffled()
                val optionsListYear = optionsSetYear.toList().shuffled()
                val optionsListEvent = optionsSetEvent.toList().shuffled()

                // Add the questions to a list.
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

                // Runs if the person field is filled, as its optional.
                picture.imagePerson?.let { person ->
                    val optionsSetPerson = mutableSetOf<String>()
                    optionsSetPerson.add(person)

                    additionalImagesPerson.shuffled().forEach { person ->
                        if (optionsSetPerson.size < 4) {
                            optionsSetPerson.add(person)
                        }
                    }

                    // Convert the set back to a list and shuffle it to ensure random order
                    val optionsListWho = optionsSetPerson.toList().shuffled()

                    // Adds question on.
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

            // Return the list of questions.
            return Pair(questions, usedPictures)
        }
        //If multiple pictures are picked.
        else {
            val additionalPlaces = fetchAdditionalImageData(imagesCollectionPath, "place", uid).shuffled().distinct()
            val additionalYears = fetchAdditionalImageData(imagesCollectionPath, "year", uid).shuffled().distinct()
            val additionalPersons = fetchAdditionalImageData(imagesCollectionPath, "person", uid).shuffled().distinct()
            val additionalEvents = fetchAdditionalImageData(imagesCollectionPath, "event", uid).shuffled().distinct()

            val allQuestions = mutableListOf<Question>()
            val usedPictures = mutableSetOf<PictureModel>()
            val minimumQuestions = 5
            val usedQuestionsForPicture = mutableMapOf<String, MutableList<String>>()

            // Function to add a question with answers from database.
            fun addQuestionWithRealOptions(picture: PictureModel, category: String, correctAnswer: String, additionalOptions: List<String>) {
                val usedQuestions = usedQuestionsForPicture.getOrDefault(picture.documentId, mutableListOf())

                // If question is unused, add the question in.
                if (!usedQuestions.contains(category)) {
                    val filteredOptions = additionalOptions.filterNot { it == correctAnswer }.take(3)
                    val options = (filteredOptions + correctAnswer).shuffled()
                    // Change the text based on the type.
                    val questionText = when (category) {
                        "place" -> "Where was this taken?"
                        "year" -> "What year was this taken?"
                        "person" -> "Who is in this photo?"
                        "event" -> "What event was taking place?"
                        else -> ""
                    }

                    val questionArea = category

                    // Add the questions to the list, as well as used pictures.
                    val question = Question(questionText, options, correctAnswer, picture, QuestionType.LONG_TERM, category)
                    allQuestions.add(question)
                    usedQuestions.add(category)
                    usedQuestionsForPicture[picture.documentId ?: return] = usedQuestions
                    usedPictures.add(picture)
                }
            }

            // Shuffle pictures, and add additional options on.
            selectedPictures.shuffled().forEach { picture ->
                if (allQuestions.size >= minimumQuestions) return@forEach

                addQuestionWithRealOptions(picture, "place", picture.imagePlace ?: "", additionalPlaces)
                addQuestionWithRealOptions(picture, "year", picture.imageYear?.toString() ?: "", additionalYears)
                addQuestionWithRealOptions(picture, "event", picture.imageEvent?: "", additionalEvents)
                if (picture.imagePerson?.isNotEmpty() == true) {
                    addQuestionWithRealOptions(picture, "person", picture.imagePerson, additionalPersons)
                }

            }

            // Shuffle all the questions.
            allQuestions.shuffle()

            // Return the pair of a list of questions and the pictures linked to them.
            return Pair(if (allQuestions.size > minimumQuestions) allQuestions.take(minimumQuestions) else allQuestions, usedPictures)
        }
    }

    // Variables for the remember number questions.
    private lateinit var rememberNumber: String
    private val distractorNumbers = mutableListOf<String>()

    /**
     * A method to generate a random 2 digit number as a short term question.
     */
    private fun generateRememberNumber() {
        // Generates a 2 digit number and sets it as the one they have to remember.
        val randomNumber = (10..99).random().toString()
        rememberNumber = randomNumber

        // Generate 3 other numbers to add 3 other options for the question.
        distractorNumbers.clear()
        while (distractorNumbers.size < 3) {
            val distractor = (10..99).random().toString()

            // Make sure its unique so numbers are not the same.
            if (distractor != rememberNumber && distractor !in distractorNumbers) {
                distractorNumbers.add(distractor)
            }
        }
    }

    /**
     * Adds the recall question to the end so it can be asked at the end of the quiz.
     */
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
        questions.add(recallQuestion)
    }

    /**
     * Adds reorder question to the end of the quiz if there are multiple pictures so the
     * user can attempt to order them.
     */
    private fun addReorderPicturesQuestion() {
        if (pictureSequence.size > 1) {
            val reorderQuestion = Question(
                questionText = "Arrange the pictures in the order they were shown.",
                options =  pictureSequence.map { it.imageUrl as String? },
                correctAnswer = pictureSequence.joinToString(",") { it.imageUrl?: "default_image_url" },
                pictureModel = PictureModel.EMPTY,
                questionType = QuestionType.SHORT_TERM,
                questionArea = "reorder"
            )
            questions.add(reorderQuestion)
        }
    }

    /**
     * A method to fetch additional picture details.
     * @param imagesCollectionPath The path to the collection containing image data.
     * @param field The field within the documents that is to be matched against the uid.
     * @param uid The user ID to get their pictures.
     * @return A list of strings from the documents' specified field.
     */
    private suspend fun fetchAdditionalImageData(imagesCollectionPath: String, field: String, uid: String): List<String> {

        //Uses coroutines so nothing will run until extra details have been fetched from user.
        return suspendCoroutine { continuation ->
            FirebaseFirestore.getInstance()
                .collection(imagesCollectionPath)
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener { documents ->
                    // Fetches the extra data and returns it, changing it if its a long or a double.
                    val data = documents.mapNotNull { document ->
                        val fieldValue = document.get(field)
                        when (fieldValue) {
                            is String -> fieldValue
                            is Long -> fieldValue.toString()
                            is Double -> fieldValue.toString()
                            else -> null
                        }
                    }
                    // Resumes activity.
                    continuation.resume(data)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWith(Result.failure(exception))
                }
        }
    }

    /**
     * Loads next question into the right fiels.
     */
    private fun goToNextQuestion() {
        // Checks if quiz is over or not.
        if (currentQuestionIndex + 1 < questions.size) {

            // If not, the next question is loaded.
            currentQuestionIndex++
                loadQuestion()
        } else {
            // Maps are counted
            correctAnswersCountMap.forEach { (documentID, count) ->
                Log.d("QuizResults", "Image $documentID got $count correct answers.")
            }
            incorrectAnswersCountMap.forEach { (documentID, count) ->
                Log.d("QuizResults", "Image $documentID got $count incorrect answers.")
            }

            // Need Gson tp parse through the maps as JSONS, otherwise they cant be parsed
            val gson = Gson()
            val correctAnswerJson = gson.toJson(correctAnswersCountMap)
            val incorrectAnswerJson = gson.toJson(incorrectAnswersCountMap)
            val questionAttemptsJson = gson.toJson(questionAttempts)

            // Put all details from the quiz through to the quiz results activity and start the
            // activity.
            val intent = Intent(this, QuizResultsActivity::class.java).apply{
                putExtra("questionsRight", questionsRight)
                putExtra("questionsWrong", questionsWrong)

                putExtra("correctAnswerJson", correctAnswerJson)
                putExtra("incorrectAnswerJson", incorrectAnswerJson)

                putExtra("questionAttemptsJson", questionAttemptsJson)

                putParcelableArrayListExtra("selectedPictures", ArrayList(selectedPictures))


            }
            startActivity(intent)
            // Finish the quiz so the user cannot navigte back.
            finish()
        }
    }

    /**
     * A method to laod the question into the right boxes and change the UI based on the type.
     */
    private fun loadQuestion(){
        // Checks index still has questions left before trying.
        if (currentQuestionIndex < questions.size) {

            // Gets question details.
            val question = questions[currentQuestionIndex]
            pictureSequence.add(question.pictureModel)
            questionTitle = findViewById<TextView>(R.id.question_title)
            questionTitle.text = question.questionText

            // If the question is a reorder question, change the UI to handle this.
            if (question.questionArea == "reorder") {
                getListOfButtons().forEach { it.visibility = View.GONE }
                findViewById<Button>(R.id.submit_order_button).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.draggable_image_container).visibility = View.VISIBLE
                findViewById<ImageView>(R.id.picture_image_view).visibility = View.GONE

                // Load question in.
                showReorderUI(question.options)
            }
            else{
                // Otherwise, load the normal information for other questions.
                getListOfButtons().forEach { it.visibility = View.VISIBLE }
                findViewById<Button>(R.id.submit_order_button).visibility = View.GONE
                findViewById<RecyclerView>(R.id.draggable_image_container).visibility = View.GONE
                findViewById<ImageView>(R.id.picture_image_view).visibility = View.VISIBLE

                val pictureImageView = findViewById<AccessibleZoomImageView>(R.id.picture_image_view)

                // Load picture into quiz for user to see via Glide.
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

                // Reset button colours, so they are not the wrong colour.
                resetButtonColors()

                //Shuffle and display answers in option buttons.
                val shuffledAnswers = question.options.shuffled()
                val buttons = getListOfButtons()
                buttons.forEachIndexed { index, button ->
                    button.text = shuffledAnswers.getOrNull(index) ?: ""
                }
            }


        }
    }

    /**
     * A method to display the reorder question.
     *
     * @param imageUrls takes a list of image urls to populate the recyler view.
     */
    private fun showReorderUI(imageUrls: List<String?>) {

        // Sets up the recycler view.
        val recyclerView = findViewById<RecyclerView>(R.id.draggable_image_container)
        recyclerView.visibility = View.VISIBLE
        val safeImageUrls = imageUrls.filterNotNull().toMutableList()
        val reorderAdapter = QuizReorderPicturesAdapter(safeImageUrls,
            this)
        recyclerView.hasFixedSize()
        recyclerView.adapter = reorderAdapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Uses ItemTouchHelper so user can drag photos across into the right order.
        val touchHelperCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.START or ItemTouchHelper.END, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val sourcePosition = viewHolder.adapterPosition
                val targetPosition = target.adapterPosition

                // Switches the photos positions if moved.
                Collections.swap(safeImageUrls, sourcePosition, targetPosition)
                reorderAdapter.notifyItemMoved(sourcePosition, targetPosition)

                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            }
        }
        val itemTouchHelper = ItemTouchHelper(touchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * A method to rest the buttons colours.
     */
    private fun resetButtonColors() {
        val buttons = getListOfButtons()

        // Loops through all buttons and changes their background.
        buttons.forEach { button ->
            val defaultQuizButton = ContextCompat.getDrawable(this, R.drawable.quiz_answer_outline_default)
            val textColor = ContextCompat.getColor(this, R.color.inverseSurfaceColor)
            button.background = defaultQuizButton
            button.setTextColor(textColor)
        }

    }

    /**
     * A method to display an AlertDialog so a user an confirm their answer.
     *
     * @param answer The answer they have picked.
     * @param onConfirmSelection The button in which they click on the alert.
     */
    private fun showAnswerPreviewDialog(answer: String, onConfirmSelection: (Boolean) -> Unit) {
        // Inflates the popup layout, so it can be seen.
        val dialogView = LayoutInflater.from(this).inflate(R.layout.confirm_answer_popup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Gets the elemets from the layout.
        val answerText = dialogView.findViewById<TextView>(R.id.preview_answer_text)
        val selectAnswerButton = dialogView.findViewById<Button>(R.id.select_answer_button)
        val goBackButton = dialogView.findViewById<Button>(R.id.go_back_button)

        // Sets the answer and reads it out with TTS so user can hear it.
        answerText.text = answer
        speakOut(answer)

        // Confirm answer button.
        selectAnswerButton.setOnClickListener {
            onConfirmSelection(true)
            dialog.dismiss()
        }

        // Cancel answer button.
        goBackButton.setOnClickListener {
            onConfirmSelection(false)
            dialog.dismiss()
        }

        // Displays alert dialog to the user.
        dialog.show()
    }

    /**
     * A method to show the number they must remember at the start of the quiz through a pop up.
     */
    private fun showRememberNumberDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.remember_number_popup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Sets it up so the popup displays the number they need to remember.
        val numberToRememberText = dialogView.findViewById<TextView>(R.id.number_to_remember_text)
        val nextButton = dialogView.findViewById<Button>(R.id.next_button)
        numberToRememberText.text = "Remember this number:\n$rememberNumber"
        numberToRememberText.setTextColor(ContextCompat.getColor(this, R.color.white))

        // Use TTS to read out the number
        speakOut("Remember this number: $rememberNumber")

        // Dismisses dialog on clicking next.
        nextButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    /**
     * A method to evaluate the answer given.
     *
     * @param selectedAnswer the answer the user has selected.
     * @param button the button the user has pressed.
     */
    private fun evaluateAnswer(selectedAnswer: String, button: Button) {

        // Disables buttons so user cant press after answering.
        disableAnswerButtons()

        // Gets the current question asked.
        val currentQuestion = questions[currentQuestionIndex]

        // If not a reorder question, it logs the attempt and checks if the answer is correct.
        if(currentQuestion.questionArea != "reorder" ){
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


            // Gets all buttons.
            val answerButtons = getListOfButtons()
            if (selectedAnswer == currentQuestion.correctAnswer) {
                // Changes button green if the answer is right and increments questions right.
                button.setBackgroundResource(R.drawable.quiz_answer_outline_correct)
                val color = ContextCompat.getColor(this, R.color.surfaceColor)
                button.setTextColor(color)
                questionsRight++

                // Increment map as well
                val documentId = currentQuestion.pictureModel.documentId ?: "unknown"
                val currentCount = correctAnswersCountMap[documentId] ?: 0
                correctAnswersCountMap[documentId] = currentCount + 1

            } else {
                // Changes button red if the answer is wrong and increments questions wrong.
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

                // Increment map as well
                val documentId = currentQuestion.pictureModel.documentId ?: "unknown"
                val currentCount = incorrectAnswersCountMap[documentId] ?: 0
                incorrectAnswersCountMap[documentId] = currentCount + 1
            }

            // Enables next button so user can go to next question.
            enableNextButton()
        }
        else{
            // Get the current order of items from the adapter.
            val adapter = findViewById<RecyclerView>(R.id.draggable_image_container).adapter as QuizReorderPicturesAdapter
            val currentOrder = adapter.getItems().joinToString(",")
            val isCorrect = currentOrder == currentQuestion.correctAnswer

            // Records their attempt at the question.
            val attempt = QuestionAttempt(
                uid = uid,
                quizId = "",
                questionText = currentQuestion.questionText,
                correctAnswer = currentQuestion.correctAnswer,
                userAnswer = currentOrder,
                questionType = currentQuestion.questionType,
                questionArea = currentQuestion.questionArea,
                isCorrect = isCorrect
            )
            questionAttempts.add(attempt)

            val confirmOrderButton = findViewById<Button>(R.id.submit_order_button)
            if (isCorrect) {
                // Increments right if the order is right
                confirmOrderButton.setBackgroundResource(R.drawable.quiz_answer_outline_correct)
                questionsRight++
            } else {
                // Increments wrong if the order is wrong.
                confirmOrderButton.setBackgroundResource(R.drawable.quiz_answer_outline_wrong)
                questionsWrong++
                showCorrectOrder(currentQuestion.correctAnswer?.split(",") ?: listOf())
            }

            // Update the correct and incorrect answers count maps
            val documentId = currentQuestion.pictureModel.documentId ?: "unknown"
            if (isCorrect) {
                val currentCount = correctAnswersCountMap[documentId] ?: 0
                correctAnswersCountMap[documentId] = currentCount + 1
            } else {
                val currentCount = incorrectAnswersCountMap[documentId] ?: 0
                incorrectAnswersCountMap[documentId] = currentCount + 1
            }

            // Enable next button.
            enableNextButton()
        }

    }

    /**
     * A method to display the correct order of the pictures.
     *
     * @param correctOrder Parses through the correct order.
     */
    private fun showCorrectOrder(correctOrder: List<String>) {
        val recyclerView = findViewById<RecyclerView>(R.id.draggable_image_container)
        val adapter = recyclerView.adapter as QuizReorderPicturesAdapter

        // Updates recycler view to show correct order.
        adapter.updateItems(correctOrder)
    }

    /**
     * A method with gets all buttons.
     *
     * @return returns all answer buttons.
     */
    private fun getListOfButtons(): List<Button> {
        return listOf(
            findViewById<Button>(R.id.answer_1_button),
            findViewById<Button>(R.id.answer_2_button),
            findViewById<Button>(R.id.answer_3_button),
            findViewById<Button>(R.id.answer_4_button)
        )
    }

    /**
     * A method to re-enable answer buttons.
     */
    private fun enableAnswerButtons() {
        val buttons = getListOfButtons()
        buttons.forEach { it.isEnabled = true }
    }

    /**
     * A method to disable answer buttons.
     */
    private fun disableAnswerButtons() {
        val buttons = getListOfButtons()
        buttons.forEach { it.isEnabled = false }
    }

    /**
     * A method to enable the next button, to navigate to the next question.
     */
    private fun enableNextButton() {
        findViewById<Button>(R.id.next_button).visibility = View.VISIBLE
    }

    /**
     * A method to disable the next button, so user cant naviagte away too soon.
     */
    private fun hideNextButton() {
        findViewById<Button>(R.id.next_button).visibility = View.GONE
    }

    /**
     * A method to add functionality for when the next button is clicked.
     */
    fun onNextButtonClick(view: View) {
        hideNextButton()
        enableAnswerButtons()
        goToNextQuestion()
    }

    //Getter for questions right and wrong.
    fun getQuestionsRight(): Int = questionsRight
    fun getQuestionsWrong(): Int = questionsWrong


    /**
     * A method to speak out text using TTS.
     *
     * @param text The text it needs to speak.
     */
    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * A method to initialise TTS.
     *
     * @param status takes the status of TTS.
     */
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
            }
        } else {
        }
    }

    /**
     * A method to destroy TTS after is has been used and the activity is over.
     */
    override fun onDestroy() {
        // Shut down TTS
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
        activityScope.cancel()
    }


    /**
     * A method to hide the systems UI.
     */
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
