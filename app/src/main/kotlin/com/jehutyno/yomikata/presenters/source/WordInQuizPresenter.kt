package com.jehutyno.yomikata.presenters.source

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.WordRepository
import kotlinx.coroutines.flow.Flow


class WordInQuizPresenter(private val wordRepository: WordRepository): WordInQuizInterface {
    override suspend fun getWordById(id: Long): Word {
        return wordRepository.getWordById(id)
    }

    override fun getWordsByIds(wordIds: LongArray): Flow<List<Word>> {
        return wordRepository.getWordsByIds(wordIds)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun incrementWordFailCount(wordId: Long) {
        wordRepository.incrementFail(wordId)
    }

    override suspend fun incrementWordSuccessCount(wordId: Long) {
        wordRepository.incrementSuccess(wordId)
    }
}
