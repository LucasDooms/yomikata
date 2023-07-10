package com.jehutyno.yomikata.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.jehutyno.yomikata.repository.database.RoomQuizWord
import com.jehutyno.yomikata.repository.database.RoomWords
import com.jehutyno.yomikata.util.inBatches
import com.jehutyno.yomikata.util.inBatchesWithFlowReturnL
import com.jehutyno.yomikata.util.inBatchesWithReturn
import kotlinx.coroutines.flow.Flow


@Dao
interface WordDao {
    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<RoomWords>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id = :quizId")
    fun getWords(quizId: Long): Flow<List<RoomWords>>

    @Query("SELECT words.* FROM words JOIN quiz_word " +
            "ON quiz_word.word_id = words._id " +
            "AND quiz_word.quiz_id IN (:quizIds)")
    fun getWordsUnsafe(quizIds: LongArray): Flow<List<RoomWords>>

    fun getWords(quizIds: LongArray): Flow<List<RoomWords>> {
        return quizIds.inBatchesWithFlowReturnL { smallerQuizIds ->
            getWordsUnsafe(smallerQuizIds)
        }
    }

    @Query("SELECT words.* FROM words JOIN quiz_word " +
           "ON quiz_word.word_id = words._id " +
           "AND quiz_word.quiz_id IN (:quizIds) " +
           "AND words.level IN (:levels)")
    fun getWordsByLevelsUnsafe(quizIds: LongArray, levels: IntArray): Flow<List<RoomWords>>

    fun getWordsByLevels(quizIds: LongArray, levels: IntArray): Flow<List<RoomWords>> {
        return quizIds.inBatchesWithFlowReturnL { smallerQuizIds ->
            getWordsByLevelsUnsafe(smallerQuizIds, levels)
        }
    }

    @Query("SELECT words.* FROM words " +
            "WHERE _id IN (:wordIds) " +
            "AND words.repetition = :repetition ORDER BY words._id LIMIT :limit")
    suspend fun getWordsByRepetitionUnsafe(wordIds: LongArray, repetition: Int, limit: Int): List<RoomWords>

    @Transaction
    suspend fun getWordsByRepetition(wordIds: LongArray, repetition: Int, limit: Int): List<RoomWords> {
        return wordIds.inBatchesWithReturn { smallerWordIds ->
            getWordsByRepetitionUnsafe(smallerWordIds, repetition, limit)
        }
    }

    @Query("SELECT words.* FROM words " +
            "WHERE _id IN (:wordIds) " +
            "AND words.repetition >= :minRepetition ORDER BY words.repetition, words._id LIMIT :limit")
    suspend fun getWordsByMinRepetitionUnsafe(wordIds: LongArray, minRepetition: Int, limit: Int): List<RoomWords>

    @Transaction
    suspend fun getWordsByMinRepetition(wordIds: LongArray, minRepetition: Int, limit: Int): List<RoomWords> {
        return wordIds.inBatchesWithReturn { smallerWordIds ->
            getWordsByMinRepetitionUnsafe(smallerWordIds, minRepetition, limit)
        }
    }

    @Query("SELECT words._id FROM words " +
            "WHERE _id IN (:wordIds) " +
            "AND words.repetition > :repetition")
    suspend fun getWordIdsWithRepetitionStrictlyGreaterThanUnsafe(wordIds: LongArray, repetition: Int): LongArray

    @Transaction
    suspend fun getWordIdsWithRepetitionStrictlyGreaterThan(wordIds: LongArray, repetition: Int): LongArray {
        return wordIds.inBatchesWithReturn { smallerWordIds ->
            getWordIdsWithRepetitionStrictlyGreaterThanUnsafe(smallerWordIds, repetition).toList()
        }.toLongArray()
    }

    @Query("UPDATE words SET repetition = repetition - 1 WHERE _id IN (:wordIds)")
    suspend fun decreaseWordRepetitionByOneUnsafe(wordIds: LongArray)

    @Transaction
    suspend fun decreaseWordRepetitionByOne(wordIds: LongArray) {
        wordIds.inBatches { smallerWordIds ->
            decreaseWordRepetitionByOneUnsafe(smallerWordIds)
        }
    }

    @Transaction
    suspend fun decreaseWordsRepetition(wordIds: LongArray) {
        val idList = getWordIdsWithRepetitionStrictlyGreaterThan(wordIds, 0)
        decreaseWordRepetitionByOne(idList)
    }

    @Query("SELECT words._id FROM words JOIN quiz_word " +
            "ON quiz_word.word_id = words._id " +    // select all quiz_words of the correct word id
            "AND quiz_word.quiz_id =  " +
            "( " +
            "SELECT quiz_id FROM quiz_word " +          // such that the quiz_id matches
            "WHERE quiz_word.word_id = :wordId " +      // that of the word with id=wordId
            "AND ( " +                              // category 8 = custom selections
            " (SELECT category FROM quiz WHERE _id = quiz_id LIMIT 1) != 8 " +
            ") " +
            ") " +
            "AND LENGTH(words.japanese) = :wordSize " +
            "AND words._id != :wordId")
    suspend fun getWordsOfSizeRelatedTo(wordId: Long, wordSize: Int): List<Long>

    @RawQuery
    suspend fun getRandomWords(rawQuery: SupportSQLiteQuery): List<RoomWords>

    @Query("SELECT * FROM words " +
           "WHERE reading LIKE '%' || (:searchString) || '%' " +
           "OR reading LIKE '%' || (:hiraganaString) || '%' " +
           "OR japanese LIKE '%' || (:searchString) || '%' " +
           "OR japanese LIKE '%' || (:hiraganaString) || '%' " +
           "OR english LIKE '%' || (:searchString) || '%' " +
           "OR french LIKE '%' || (:searchString) || '%'")
    fun searchWords(searchString: String, hiraganaString: String): Flow<List<RoomWords>>

    @Query("SELECT EXISTS ( " +
            "SELECT * FROM quiz_word " +
            "WHERE word_id = :wordId AND quiz_id = :quizId " +
           ")")
    suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean

    @Query("SELECT * FROM words WHERE _id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: Long): RoomWords?

    @Query("SELECT * FROM words WHERE _id IN (:wordIds)")
    fun getWordsByIdsUnSafe(wordIds: LongArray): Flow<List<RoomWords>>

    fun getWordsByIds(wordIds: LongArray): Flow<List<RoomWords>> {
        return wordIds.inBatchesWithFlowReturnL { reducedWordIds ->
            getWordsByIdsUnSafe(reducedWordIds)
        }
    }

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Delete
    suspend fun deleteWord(word: RoomWords)

    @Update
    suspend fun updateWord(word: RoomWords)

    @Query("UPDATE words SET points = :points WHERE _id = :wordId")
    suspend fun updateWordPoints(wordId: Long, points: Int)

    @Query("UPDATE words SET level = :level " +
           "WHERE _id = :wordId")
    suspend fun updateWordLevel(wordId: Long, level: Int)

    @Query("UPDATE words SET repetition = :repetition WHERE _id = :wordId")
    suspend fun updateWordRepetition(wordId: Long, repetition: Int)

    @Insert
    suspend fun addQuizWord(quizWord: RoomQuizWord): Long

    @Insert
    suspend fun addWord(word: RoomWords): Long

    @Query("UPDATE words SET count_fail = count_fail + 1 WHERE _id = :wordId")
    suspend fun incrementFail(wordId: Long)

    @Query("UPDATE words SET count_success = count_success + 1 WHERE _id = :wordId")
    suspend fun incrementSuccess(wordId: Long)
}
