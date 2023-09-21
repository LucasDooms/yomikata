package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.LevelInterface
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.util.Category


/**
 * Created by valentin on 27/09/2016.
 */
interface ContentContract {

    interface View {
        fun displayWords(words: List<Word>)
        fun displayStats()
    }

    interface Presenter: BasePresenter, SelectionsInterface, LevelInterface,
                         WordCountInterface, WordInQuizInterface {
        val words: LiveData<List<Word>>
        suspend fun launchQuizStat(category: Category)
    }

}
