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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/**
 * Created by jehutyno on 08/10/2016.
 */
class WordPagerAdapter(
    fragment: Fragment, private val coroutineScope: CoroutineScope,
    var quizType: QuizType?, private val callback: Callback, private val presenter: WordContract.Presenter
) : FragmentStateAdapter(fragment) {

    private val words: ArrayList<Triple<MutableLiveData<Word>,
            MutableLiveData<List<KanjiSoloRadical>?>, MutableLiveData<Sentence?>>> = arrayListOf()

    val count get() = words.count()

    fun updateWord(position: Int, newWord: Word) {
        words[position].first.value = newWord
    }

    fun replaceData(list: List<Word>) {
        words.clear()
        words.addAll(
            list.map {
                Triple(
                    MutableLiveData(it), MutableLiveData(null), MutableLiveData(null)
                )
            }
        )
        @Suppress("notifyDataSetChanged")
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
        // load kanjisolo and sentence if not loaded yet
        if (words[position].second.value == null || words[position].third.value == null) {
            coroutineScope.launch {
                words[position].second.value = presenter.getKanjiSoloList(words[position].first.value!!)
                words[position].third.value = presenter.getSentence(words[position].first.value!!)
            }
        }
        return WordFragment(words[position], quizType, ExtendedCallback())
    }

}
