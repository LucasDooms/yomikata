package com.jehutyno.yomikata.screens.quizzes

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Extras
import org.hamcrest.Matchers.*
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
    fun floatActionButton() {
        // starts in home screen -> no button
        onView(withId(R.id.multiple_actions))
            .check(matches(not(isDisplayed())))
        // go to a category with button
        activityRule.scenario.onActivity { quizzesActivity ->
            quizzesActivity.gotoCategory(Categories.CATEGORY_HIRAGANA)
        }
        // check visibility
        onView(withId(R.id.multiple_actions))
            .check(matches(isDisplayed()))
    }

    @Test
    fun correctTextTitle() {
        // starts in home screen
        onView(withId(R.id.text_title))
            .check(matches(withText(R.string.home_title)))
        // go to different category
        activityRule.scenario.onActivity { quizzesActivity ->
            quizzesActivity.gotoCategory(Categories.CATEGORY_KANJI)
        }
        // check if text has changed correctly
        onView(withId(R.id.text_title))
            .check(matches(withText(R.string.drawer_kanji_beginner)))
    }

    @Test
    fun navViewChangeCategory() {
        // open nav view
        onView(allOf(withParent(withId(R.id.toolbar)), isFocusable()))
            .perform(click())
        // click on a category
        onView(withId(R.id.jlpt5_item))
            .perform(click())

        activityRule.scenario.onActivity { quizzesActivity ->
            // find resumed, throw error if none found
            val resumedFragment = quizzesActivity.supportFragmentManager
                .fragments.first { it.isResumed }
            val correctFragment = quizzesActivity.supportFragmentManager
                .findFragmentByTag("f${quizzesActivity.quizzesAdapter.positionFromCategory(Categories.CATEGORY_JLPT_5)}")
            assert(resumedFragment.equals(correctFragment))
        }

    }

}
