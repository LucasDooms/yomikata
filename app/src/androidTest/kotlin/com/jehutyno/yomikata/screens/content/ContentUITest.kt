package com.jehutyno.yomikata.screens.content

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
import com.jehutyno.yomikata.util.QuizType
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class ContentUITest {

    companion object {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ContentActivity::class.java)
            .putExtra(Extras.EXTRA_CATEGORY, Categories.CATEGORY_HIRAGANA)
            .putExtra(Extras.EXTRA_QUIZ_POSITION, 0)
            .putExtra(Extras.EXTRA_QUIZ_TYPES, arrayListOf(QuizType.TYPE_EN_JAP))
            // EXTRA_LEVEL is null
    }

    @get:Rule
    val activityRule: ActivityScenarioRule<ContentActivity> = ActivityScenarioRule(intent)

    @Test
    fun floatActionButton() {
        onView(withId(R.id.multiple_actions))
            .perform(click())
            .check(matches(isDisplayed()))
    }

    @Test
    fun enterSelectModeThroughToolbar() {
        onView(withId(R.id.select_mode))
            .perform(click())
        // check that checkboxes are shown
        onView(allOf(withId(R.id.word_check), withParent(
            withParent(withChild(allOf(withId(R.id.kanji_word), withText("あ"))))
        )))
            .check(matches(isDisplayed()))
            .check(matches(isClickable()))
            .check(matches(isNotChecked()))
    }

}
