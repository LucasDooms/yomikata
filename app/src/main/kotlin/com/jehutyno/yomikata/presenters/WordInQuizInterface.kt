package com.jehutyno.yomikata.presenters

import com.jehutyno.yomikata.model.Word
import kotlinx.coroutines.flow.Flow


interface WordInQuizInterface {
    suspend fun getWordById(id: Long): Word
    fun getWordsByIds(wordIds: LongArray): Flow<List<Word>>
    suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
    suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
    suspend fun incrementWordFailCount(wordId: Long)
    suspend fun incrementWordSuccessCount(wordId: Long)
}
