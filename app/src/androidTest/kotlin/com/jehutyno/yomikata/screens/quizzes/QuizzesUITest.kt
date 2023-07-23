package com.jehutyno.yomikata.screens.quizzes

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Extras
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class QuizzesUITest {

    companion object {
        val intent = Intent(ApplicationProvider.getApplicationContext(), QuizzesActivity::class.java)
            .putExtra(Extras.EXTRA_ANIMATIONS_ENABLED, false)
    }

    @get:Rule
    val activityRule: ActivityScenarioRule<QuizzesActivity> = ActivityScenarioRule(intent)


    @Test
    fun tryout() {
        activityRule.scenario.onActivity { quizzesActivity ->
            quizzesActivity.gotoCategory(Categories.CATEGORY_HIRAGANA)
        }
        onView(withId(R.id.multiple_actions))
            .perform(click())
            .check(matches(isDisplayed()))
    }

}
