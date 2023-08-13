package com.jehutyno.yomikata.presenters.source

import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.presenters.WordInQuizInterface
import com.jehutyno.yomikata.repository.WordRepository


class WordInQuizPresenter(private val wordRepository: WordRepository): WordInQuizInterface {
    override suspend fun getWordById(id: Long): Word {
        return wordRepository.getWordById(id)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long) : Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>) : ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun updateWordCheck(id: Long, check: Boolean) {
        wordRepository.updateWordSelected(id, check)
    }

    override suspend fun updateWordsCheck(ids: LongArray, check: Boolean) {
        wordRepository.updateWordsSelected(ids, check)
    }
}
