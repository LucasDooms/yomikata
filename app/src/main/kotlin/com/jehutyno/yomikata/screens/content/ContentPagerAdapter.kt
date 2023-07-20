package com.jehutyno.yomikata.screens.content

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Category
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.QuizType
import org.kodein.di.DI


/**
 * Created by valentin on 19/12/2016.
 */
class ContentPagerAdapter(
    activity: ContentActivity, var quizzes: List<Quiz>, private val di: DI,
    private val category: Category, private val selectedTypes: ArrayList<QuizType>
    ) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return quizzes.size
    }

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        // specific to ContentQuiz
        bundle.putSerializable(Extras.EXTRA_QUIZ, quizzes[position])
        // general
        bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, longArrayOf(quizzes[position].id))
        bundle.putSerializable(Extras.EXTRA_SELECTION, quizzes[position])
        bundle.putSerializable(Extras.EXTRA_CATEGORY, category)
        bundle.putSerializable(Extras.EXTRA_QUIZ_TYPES, selectedTypes)

        val contentFragment = ContentQuizFragment(di)
        contentFragment.arguments = bundle
        return contentFragment
    }

}
