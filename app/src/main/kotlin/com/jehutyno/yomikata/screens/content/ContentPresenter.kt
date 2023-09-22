package com.jehutyno.yomikata.screens.content

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.Categories
import com.jehutyno.yomikata.util.Level
import mu.KLogging


/**
 * Created by valentin on 29/09/2016.
 */
class ContentPresenter(
    private val wordRepository: WordRepository,
    private val quizRepository: QuizRepository,
    contentView: ContentContract.View,
    quizIds : LongArray, level : Level?) : ContentContract.Presenter {

    companion object : KLogging()

    init {
        contentView.setPresenter(this)
    }

    // define LiveData
    override val words: LiveData<List<Word>> =
        wordRepository.getWordsByLevel(quizIds, level).asLiveData().distinctUntilChanged()
    override val selections: LiveData<List<Quiz>> =
        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS).asLiveData().distinctUntilChanged()
    override val quizCount: LiveData<Int> =
        quizRepository.countWordsForQuizzes(quizIds).asLiveData().distinctUntilChanged()
    override val lowCount: LiveData<Int> =
        quizRepository.countWordsForLevel(quizIds, Level.LOW).asLiveData().distinctUntilChanged()
    override val mediumCount: LiveData<Int> =
        quizRepository.countWordsForLevel(quizIds, Level.MEDIUM).asLiveData().distinctUntilChanged()
    override val highCount: LiveData<Int> =
        quizRepository.countWordsForLevel(quizIds, Level.HIGH).asLiveData().distinctUntilChanged()
    override val masterCount: LiveData<Int> =
        quizRepository.countWordsForLevel(quizIds, Level.MASTER).asLiveData().distinctUntilChanged()

    override fun start() {
        logger.info("Content presenter start")
    }

    override suspend fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override suspend fun updateWordsCheck(ids: LongArray, check: Boolean) {
        wordRepository.updateWordsSelected(ids, check)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override suspend fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

}
