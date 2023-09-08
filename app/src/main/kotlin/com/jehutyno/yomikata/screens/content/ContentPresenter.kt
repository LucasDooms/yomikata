package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.StatAction
import com.jehutyno.yomikata.model.StatResult
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.LevelInterface
import com.jehutyno.yomikata.presenters.SelectionsInterface
import com.jehutyno.yomikata.presenters.WordCountInterface
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Category
import com.jehutyno.yomikata.util.Level
import mu.KLogging
import java.util.Calendar


/**
 * Created by valentin on 29/09/2016.
 */
class ContentPresenter(
    wordRepository: WordRepository,
    private val statsRepository: StatsRepository,
    selectionsInterface: SelectionsInterface,
    levelInterface: LevelInterface,
    wordCountInterface: WordCountInterface,
    wordInQuizInterface: WordInQuizInterface,
    quizIds : LongArray, level : Level?) : ContentContract.Presenter,
    SelectionsInterface by selectionsInterface, LevelInterface by levelInterface,
    WordCountInterface by wordCountInterface, WordInQuizInterface by wordInQuizInterface {

    companion object : KLogging()

    // define LiveData
    override val words: LiveData<List<Word>> =
        wordRepository.getWordsByLevel(quizIds, level).asLiveData().distinctUntilChanged()

    override fun start() {
        logger.info("Content presenter start")
    }

    /**
     * Launch quiz stat
     *
     * Saves to the database that a quiz of [category] was launched
     *
     * @param category Category that was launched
     */
    override suspend fun launchQuizStat(category: Category) {
        statsRepository.addStatEntry(
            StatAction.LAUNCH_QUIZ_FROM_CATEGORY,
            category.index.toLong(),
            Calendar.getInstance().timeInMillis,
            StatResult.OTHER)
    }

}
