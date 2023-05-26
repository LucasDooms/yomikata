package com.jehutyno.yomikata.screens.search

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface


/**
 * Created by valentin on 13/10/2016.
 */
interface SearchResultContract {

    interface View: BaseView<Presenter> {
        fun displayResults(words: List<Word>)
        fun displayNoResults()
    }

    interface Presenter: BasePresenter, SelectionsInterface, WordInQuizInterface {
        val words : LiveData<List<Word>>
        fun updateSearchString(newSearchString: String)
        suspend fun updateWordCheck(id: Long, check: Boolean)
        suspend fun updateWordsCheck(ids: LongArray, check: Boolean)
    }

}
