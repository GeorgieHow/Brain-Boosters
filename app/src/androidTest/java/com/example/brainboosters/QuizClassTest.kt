import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.brainboosters.QuizActivity
import com.example.brainboosters.R
import com.example.brainboosters.model.PictureModel
import com.google.firebase.Timestamp
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.mockito.Mockito
import java.util.Date



@RunWith(AndroidJUnit4::class)
class QuizActivityTest {

    private lateinit var quizActivity: QuizActivity
    private lateinit var generateRememberNumberMethod: Method
    private lateinit var scenario: ActivityScenario<QuizActivity>
    val mockAuth = Mockito.mock(FirebaseAuth::class.java)
    val mockUser = Mockito.mock(FirebaseUser::class.java)

    val url = " "

    private fun createMockPictureModel(): ArrayList<PictureModel> {
        return arrayListOf(
            PictureModel(
                imageUrl = "url",
                imageName = "Image 1",
                documentId = "doc1",
                imagePerson = "Person 1",
                imagePlace = "Place 1",
                imageEvent = "Event 1",
                imageDescription = "Description 1",
                imageYear = 2021,
                timestamp = Timestamp(Date()),
                tags = listOf("tag1", "tag2"),
                imagePriority = "High"
            ),
            PictureModel(
                imageUrl = "url",
                imageName = "Image 2",
                documentId = "doc2",
                imagePerson = "Person 2",
                imagePlace = "Place 2",
                imageEvent = "Event 2",
                imageDescription = "Description 2",
                imageYear = 2022,
                timestamp = Timestamp(Date()),
                tags = listOf("tag3", "tag4"),
                imagePriority = "Medium"
            )
        )
    }

    @Before
    fun setUp() {
        Mockito.`when`(mockAuth.currentUser).thenReturn(mockUser)
        Mockito.`when`(mockUser.uid).thenReturn("m7tuoCTng1UtIbDh54p7DVFQfvL2")

        val mockPictures = createMockPictureModel()

        val startIntent = Intent(ApplicationProvider.getApplicationContext(), QuizActivity::class.java).apply{
            putParcelableArrayListExtra("selectedPictures", mockPictures)
        }
        scenario = ActivityScenario.launch(startIntent)
        scenario.onActivity { activity ->
            activity.setFirebaseAuth(mockAuth)
            quizActivity = activity
            generateRememberNumberMethod = QuizActivity::class.java.getDeclaredMethod("generateRememberNumber")
            generateRememberNumberMethod.isAccessible = true
        }
    }

    @Test
    fun testGenerateNumberSize() {
        generateRememberNumberMethod.invoke(quizActivity)
        val rememberNumberField = QuizActivity::class.java.getDeclaredField("rememberNumber")
        rememberNumberField.isAccessible = true
        val number = rememberNumberField.get(quizActivity) as String

        val distractorNumbersField = QuizActivity::class.java.getDeclaredField("distractorNumbers")
        distractorNumbersField.isAccessible = true
        val distractorNumbers = distractorNumbersField.get(quizActivity) as MutableList<String>

        assertEquals(4, (distractorNumbers + number).distinct().size)
    }

    @Test
    fun testGenerateRememberNumberWithinRange() {
        generateRememberNumberMethod.invoke(quizActivity)
        val rememberNumberField = QuizActivity::class.java.getDeclaredField("rememberNumber")
        rememberNumberField.isAccessible = true
        val number = rememberNumberField.get(quizActivity) as String

        val distractorNumbersField = QuizActivity::class.java.getDeclaredField("distractorNumbers")
        distractorNumbersField.isAccessible = true

        assertTrue(number.toInt() in 10..99)
    }

    @Test
    fun testGenerateDistractorNumberSize() {
        generateRememberNumberMethod.invoke(quizActivity)
        val rememberNumberField = QuizActivity::class.java.getDeclaredField("rememberNumber")
        rememberNumberField.isAccessible = true

        val distractorNumbersField = QuizActivity::class.java.getDeclaredField("distractorNumbers")
        distractorNumbersField.isAccessible = true
        val distractorNumbers = distractorNumbersField.get(quizActivity) as MutableList<String>

        assertEquals(3, distractorNumbers.size)
    }

    @Test
    fun testAnswerButtonBehavior() {
        Thread.sleep(5000)
        onView(withId(R.id.next_button)).perform(click())
        onView(withId(R.id.answer_1_button)).check(matches(isEnabled())).perform(click())
        onView(withId(R.id.select_answer_button)).perform(click())
        onView(withId(R.id.answer_1_button)).perform(click())
        onView(withId(R.id.answer_1_button)).check(matches(isNotEnabled()))
    }

    @Test
    fun testEvaluateAnswerCorrectAnswer() {
        Thread.sleep(5000)
        onView(withId(R.id.next_button)).perform(click())
        onView(withId(R.id.answer_1_button)).perform(click())
        onView(withId(R.id.select_answer_button)).perform(click())
        scenario.onActivity { activity ->
            assertEquals(1, activity.getQuestionsRight())
            assertEquals(0, activity.getQuestionsWrong())
        }
    }

    @Test
    fun testEvaluateAnswerIncorrectAnswer() {
        Thread.sleep(5000)
        onView(withId(R.id.next_button)).perform(click())
        onView(withId(R.id.answer_2_button)).perform(click())
        onView(withId(R.id.select_answer_button)).perform(click())
        scenario.onActivity { activity ->
            assertEquals(1, activity.getQuestionsWrong())
            assertEquals(0, activity.getQuestionsRight())
        }
    }

    @Test
    fun testNextButtonBehavior() {
        Thread.sleep(5000)
        onView(withId(R.id.next_button)).perform(click())
        onView(withId(R.id.answer_1_button)).check(matches(isEnabled())).perform(click())
        onView(withId(R.id.select_answer_button)).perform(click())
        onView(withId(R.id.answer_1_button)).perform(click())
        onView(withId(R.id.next_button)).perform(click())
    }



}
