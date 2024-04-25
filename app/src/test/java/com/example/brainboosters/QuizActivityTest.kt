import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.example.brainboosters.QuizActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.OLDEST_SDK])
class QuizActivityTest {

    private lateinit var activity: QuizActivity
    private lateinit var controller: ActivityController<QuizActivity>
    private lateinit var generateRememberNumberMethod: Method
    private lateinit var firestore: FirebaseFirestore

    @Before
    fun setUp() {
        if (FirebaseApp.getApps(RuntimeEnvironment.application).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId("brainboosters-29e43")
                .setApplicationId("1:197186655768:android:8ff822234dd5bb92abd56d")
                .setApiKey("AIzaSyAPygbjqUvx2axDHBmdioTL-0wWXhx3CQc")
                .build()
            FirebaseApp.initializeApp(RuntimeEnvironment.application, options)
        }

        firestore = Mockito.mock(FirebaseFirestore::class.java)
        Mockito.`when`(FirebaseFirestore.getInstance()).thenReturn(firestore)

        controller = Robolectric.buildActivity(QuizActivity::class.java)
        activity = controller.create().get()
        generateRememberNumberMethod = QuizActivity::class.java.getDeclaredMethod("generateRememberNumber")
        generateRememberNumberMethod.isAccessible = true


    }
    @Test
    fun testGenerateRememberNumber() {
        generateRememberNumberMethod.invoke(activity)
        val rememberNumber = activity::class.java.getDeclaredField("rememberNumber")
        rememberNumber.isAccessible = true
        val number = rememberNumber.get(activity) as String

        val distractorNumbersField = activity::class.java.getDeclaredField("distractorNumbers")
        distractorNumbersField.isAccessible = true
        val distractorNumbers = distractorNumbersField.get(activity) as MutableList<String>

        assertEquals(4, (distractorNumbers + number).distinct().size)
        assertTrue(number.toInt() in 10..99)
        assertEquals(3, distractorNumbers.size)
    }

    @After
    fun tearDown() {
        // Clean up after your test
        controller.pause().stop().destroy()
    }
}
