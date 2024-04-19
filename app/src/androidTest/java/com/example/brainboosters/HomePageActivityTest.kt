import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import com.example.brainboosters.HomePageActivity
import com.example.brainboosters.R
import com.google.firebase.FirebaseApp
import org.hamcrest.core.AllOf.allOf
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class HomePageActivityTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseApp.initializeApp(context)
        MockitoAnnotations.initMocks(this)
        ActivityScenario.launch(HomePageActivity::class.java)
    }

    @Test
    fun testHomeNavigation() {
        onView(withId(R.id.nav_home)).perform(click())
        onView(withId(R.id.homeFragmentLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testGalleryNavigation() {
        onView(withId(R.id.nav_gallery)).perform(click())
        onView(withId(R.id.galleryFragmentLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testStatisticsNavigation() {
        onView(withId(R.id.nav_statistics)).perform(click())
        onView(withId(R.id.statisticsFragmentLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testProfileNavigation() {
        onView(withId(R.id.nav_profile)).perform(click())
        onView(withId(R.id.profileFragmentLayout)).check(matches(isDisplayed()))
    }
}