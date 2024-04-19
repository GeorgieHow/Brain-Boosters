package com.example.brainboosters

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class HomeFragmentActivityTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FirebaseApp.initializeApp(context)
        MockitoAnnotations.initMocks(this)
        ActivityScenario.launch(HomePageActivity::class.java)
    }
    @Test
    fun testStartGenerateQuizButton() {
        onView(withId(R.id.start_generate_quiz_button)).perform(click())
        // Assume QuizImageSelectionActivity opens a specific Fragment or Activity
        onView(withId(R.id.quizImageSelectionFragmentLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testFamilyAlbumButton() {
        onView(withId(R.id.family_album_button)).perform(click())
        // Assuming FamilyAlbumActivity opens up
        onView(withId(R.id.familyAlbumFragmentLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testQuizResultsButton() {
        onView(withId(R.id.quiz_results_button)).perform(click())
        // Assuming it opens PreviousQuizResultsFragment
        onView(withId(R.id.previousQuizResultsFragmentLayout)).check(matches(isDisplayed()))
    }

    @Test
    fun testStartPriorityQuizButton_OpensQuiz() {
        onView(withId(R.id.start_priority_quiz_button)).perform(click())
        // Assume it starts quiz directly or opens a selection screen
        onView(withText("Priority Quiz Started")).check(matches(isDisplayed()))
    }

    @Test
    fun testLogOutButton() {
        onView(withId(R.id.log_out_button)).perform(click())
        onView(withId(R.id.welcome_page_layout)).check(matches(isDisplayed()))
    }
}
