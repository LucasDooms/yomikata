package com.jehutyno.yomikata.screens.quizzes

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.screens.home.HomeFragment
import com.jehutyno.yomikata.util.Category
import com.jehutyno.yomikata.util.Extras
import org.kodein.di.DI


/**
 * Created by valentin on 19/12/2016.
 */
class QuizzesPagerAdapter(activity: QuizzesActivity, private val di: DI) : FragmentStateAdapter(activity) {

    val categories = Category.values()

    override fun getItemCount(): Int {
        return categories.size
    }

    override fun createFragment(position: Int): Fragment {
        return if (categories[position] == Category.HOME) {
            HomeFragment(di)
        } else {
            val bundle = Bundle()
            bundle.putSerializable(Extras.EXTRA_CATEGORY, categories[position])
            val quizzesFragment = QuizzesFragment(di)
            quizzesFragment.arguments = bundle
            quizzesFragment
        }
    }

    companion object {
        fun positionFromCategory(selectedCategory: Category): Int {
            return when (selectedCategory) {
                Category.HOME -> 0
                Category.SELECTIONS -> 1
                Category.HIRAGANA -> 2
                Category.KATAKANA -> 3
                Category.KANJI -> 4
                Category.COUNTERS -> 5
                Category.JLPT_5 -> 6
                Category.JLPT_4 -> 7
                Category.JLPT_3 -> 8
                Category.JLPT_2 -> 9
                Category.JLPT_1 -> 10
            }
        }
    }


    fun getMenuItemFromPosition(position: Int): Int {
        return when (position) {
            0 -> R.id.home
            1 -> R.id.your_selections_item
            2 -> R.id.hiragana_item
            3 -> R.id.katakana_item
            4 -> R.id.kanji_item
            5 -> R.id.counters_item
            6 -> R.id.jlpt5_item
            7 -> R.id.jlpt4_item
            8 -> R.id.jlpt3_item
            9 -> R.id.jlpt2_item
            else -> R.id.jlpt1_item
        }
    }

}
