package com.jehutyno.yomikata.presenters

import com.jehutyno.yomikata.model.Quiz


interface SelectionsInterface {
    suspend fun getSelections(): List<Quiz>
    suspend fun createSelection(quizName: String): Long
    suspend fun addWordToSelection(wordId: Long, quizId: Long)
    suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
    suspend fun addWordsToSelection(wordIds: LongArray, quizId: Long)
    suspend fun deleteWordsFromSelection(wordIds: LongArray, quizId: Long)
}
