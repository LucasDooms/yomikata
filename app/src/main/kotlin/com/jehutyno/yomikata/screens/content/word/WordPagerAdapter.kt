package com.jehutyno.yomikata.screens.content.word

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.QuizType


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordPagerAdapter(
    fragment: Fragment,
    var quizType: QuizType?, private val callback: Callback
) : FragmentStateAdapter(fragment) {

    private val words: ArrayList<Triple<MutableLiveData<Word>, List<KanjiSoloRadical?>, Sentence>> = arrayListOf()

    val count get() = words.count()

    fun updateWord(position: Int, newWord: Word) {
        words[position].first.value = newWord
    }

    fun replaceData(list: List<Triple<Word, List<KanjiSoloRadical?>, Sentence>>) {
        words.clear()
        words.addAll(
            list.map {
                Triple(
                    MutableLiveData(it.first), it.second, it.third
                )
            }
        )
        notifyDataSetChanged()
    }


    interface Callback {
        fun onSelectionClick(view: View, word: Word)
        fun onReportClick(wordKanjiSentence: Triple<Word, List<KanjiSoloRadical?>, Sentence>)
        fun onWordTTSClick(word: Word)
        fun onSentenceTTSClick(sentence: Sentence)
        fun onLevelUp(word: Word, position: Int)
        fun onLevelDown(word: Word, position: Int)
        fun onCloseClick()
    }

    interface InternalCallback: Callback {
        fun onLevelUp(word: Word)
        fun onLevelDown(word: Word)
    }

    override fun getItemCount(): Int {
        return words.size
    }

    override fun createFragment(position: Int): Fragment {
        Log.e("FRAGMENT ADAPTER", "$position CREATED")
        class ExtendedCallback: InternalCallback, Callback by callback {
            override fun onLevelUp(word: Word) {
                callback.onLevelUp(word, position)
            }
            override fun onLevelDown(word: Word) {
                callback.onLevelDown(word, position)
            }
        }
        return WordFragment(words[position], quizType, ExtendedCallback())
    }

}
