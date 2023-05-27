package com.jehutyno.yomikata.dao

import androidx.room.*
import com.jehutyno.yomikata.repository.database.RoomQuiz
import com.jehutyno.yomikata.repository.database.RoomQuizWord
import com.jehutyno.yomikata.util.inBatches
import kotlinx.coroutines.flow.Flow


@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz WHERE category = :category")
    fun getQuizzesOfCategory(category: Int): Flow<List<RoomQuiz>>

    @Query("SELECT * FROM quiz WHERE _id = :quizId LIMIT 1")
    suspend fun getQuizById(quizId: Long): RoomQuiz?

    @Query("SELECT * FROM quiz")
    suspend fun getAllQuizzes(): List<RoomQuiz>

    @Insert
    suspend fun addQuiz(quiz: RoomQuiz): Long

    @Delete
    suspend fun deleteQuiz(quiz: RoomQuiz)

    @Query("DELETE FROM quiz")
    suspend fun deleteAllQuiz()

    @Query("UPDATE quiz SET name_en = :quizName, " +
                           "name_fr = :quizName " +
           "WHERE _id = :quizId")
    suspend fun updateQuizName(quizId: Long, quizName: String)

    @Query("UPDATE quiz SET isSelected = :isSelected " +
           "WHERE _id = :quizId")
    suspend fun updateQuizSelected(quizId: Long, isSelected: Boolean)

    @Query("SELECT * FROM quiz_word")
    suspend fun getAllQuizWords(): List<RoomQuizWord>

    @Insert
    suspend fun addQuizWord(quiz_word: RoomQuizWord)

    @Delete
    suspend fun deleteWordFromQuiz(quiz_word: RoomQuizWord)

    @Query("SELECT EXISTS ( " +
            "SELECT * FROM quiz_word " +
            "WHERE word_id = :wordId AND quiz_id = :quizId " +
            ")")
    suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean

    @Transaction
    suspend fun addQuizWords(quiz_words: List<RoomQuizWord>) {
        quiz_words.forEach { quiz_word ->
            if (!isWordInQuiz(quiz_word.word_id, quiz_word.quiz_id))
                addQuizWord(RoomQuizWord(quiz_word.quiz_id, quiz_word.word_id))
        }
    }

    @Query("DELETE FROM quiz_word WHERE quiz_id = :quizId AND word_id IN (:wordIds)")
    suspend fun deleteWordsFromQuizUnSafe(wordIds: LongArray, quizId: Long)

    @Transaction
    suspend fun deleteWordsFromQuiz(wordIds: LongArray, quizId: Long) {
        wordIds.inBatches { smallerWordIds ->
            deleteWordsFromQuizUnSafe(smallerWordIds, quizId)
        }
    }

    @Query("SELECT COUNT(*) FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.level = :level")
    fun countWordsForLevel(quizIds: LongArray, level: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds)")
    fun countWordsForQuizzes(quizIds: LongArray): Flow<Int>
}
