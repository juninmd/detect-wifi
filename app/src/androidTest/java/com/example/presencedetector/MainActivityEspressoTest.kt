package com.example.presencedetector

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testAppLaunchAndUIElements() {
        // Check if the Home Monitor switch is displayed. It's inside a CardView which is in a ScrollView.
        onView(withId(R.id.switchHomeMonitor))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // Check if the AntiTheft button is displayed.
        onView(withId(R.id.btnAntiTheft))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // Check if the Panic button is displayed. It is floating at the bottom, outside the ScrollView.
        onView(withId(R.id.btnPanic))
            .check(matches(isDisplayed()))
    }
}
