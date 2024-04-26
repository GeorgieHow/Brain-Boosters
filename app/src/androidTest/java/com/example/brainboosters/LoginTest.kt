package com.example.brainboosters
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*

@RunWith(AndroidJUnit4::class)
class LoginTest {

    @get:Rule
    var intentsTestRule = IntentsTestRule(LoginPageActivity::class.java)

    @Test
    fun testUserLoginInputs() {
        onView(withId(R.id.emailText)).perform(typeText("testuserfordiss@gmail.com"), closeSoftKeyboard())
        onView(withId(R.id.passwordText)).perform(typeText("testpassword"), closeSoftKeyboard())
        onView(withId(R.id.enterButton)).perform(click())
        Intents.intended(hasComponent(HomePageActivity::class.java.name))
    }

    @Test
    fun testFieldsAreNotEmpty() {

        onView(withId(R.id.enterButton)).perform(click())
        onView(withText("Make sure all fields are filled in when attempting to login."))
            .check(matches(isDisplayed()))
    }


}