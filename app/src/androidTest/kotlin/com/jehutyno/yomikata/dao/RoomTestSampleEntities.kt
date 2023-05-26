package com.jehutyno.yomikata.dao

import com.jehutyno.yomikata.repository.database.RoomKanjiSolo
import com.jehutyno.yomikata.repository.database.RoomKanjiSoloRadical
import com.jehutyno.yomikata.repository.database.RoomQuiz
import com.jehutyno.yomikata.repository.database.RoomQuizWord
import com.jehutyno.yomikata.repository.database.RoomRadicals
import com.jehutyno.yomikata.repository.database.RoomSentences
import com.jehutyno.yomikata.repository.database.RoomStatEntry
import com.jehutyno.yomikata.repository.database.RoomWords
import kotlinx.coroutines.runBlocking


/**
 * Samples of Room entities for testing
 */


val sampleRoomKanjiSolo = listOf (
    RoomKanjiSolo("", 0, "", "", "", "", ""),
    RoomKanjiSolo("七", 2, "seven", "sept", "なな、なの、ななつ","シチ", "一"),
    RoomKanjiSolo("症", 10, "symptoms, illness", "symptômes, maladie", "", "ショウ", "⽧")
)

val sampleRoomRadicals = listOf (
    RoomRadicals("", 0, "", "", ""),
    RoomRadicals("⼄", 1, "", "the second", "le second"),
    RoomRadicals("⽼", 6, "おいかんむり", "old, old-age", "vieux, ancien")
)

val sampleRoomKanjiSoloRadical = listOf (
    RoomKanjiSoloRadical("", 0, "", "", "", "", "",
        0, "", "", ""),
    RoomKanjiSoloRadical("侮", 8, "insult, despise, scorn", "insulter, mépriser, dédain",
        "あなど、あなどる", "ブ", "⺅", 2, "にんべん",
        "person", "personne")
)

val sampleRoomQuiz = listOf (
    RoomQuiz(0, "", "", 0, false),
    RoomQuiz(0, "Kanji: Verbs%行く 入る 会う...", "Kanji: Verbes%行く 入る 会う...", 2, false),
    RoomQuiz(0, "N4 - Vocabulary Part2%注意 火事 星...", "N4 - Vocabulaire Partie2%注意 火事 星...", 6, true)
)

val sampleRoomQuizWords = listOf (
    RoomQuizWord(1, 2),
    RoomQuizWord(4, 6)
)

val sampleRoomWords = listOf (
    RoomWords(0, "", "", "", "", 0, 0,
          0, 0, 0, 0, 0, null),
    RoomWords(0, "金", "metal; Friday", "métal; vendredi", "きん",
             0, 0, 0, 0, -1, 0,
       2,null)
)

val sampleRoomSentences = listOf (
    RoomSentences(0, "", "", "", 0),
    RoomSentences(0, "{彼;かれ}が{試験;しけん}に{受;う}かるかどうか{は;わ}{五;ご}{分;ぶ}{五;ご}{分;ぶ}だ。",
                    "It's 50/50 whether he passes the test or not.",
                    "Les chances qu'il réussisse l'examen ou pas sont de 50/50.", 4)
)

val sampleStatEntries = listOf (
    RoomStatEntry(0, 0, 0, 0, 0),
    RoomStatEntry(0, 2, 757, 1680209804328, 2),
    RoomStatEntry(0, 1, 753, 1680455079568, 0)
)

fun getRandomRoomWord(wordId: Long): RoomWords {
    return RoomWords(wordId, "", "", "", "", 0, 0,
        0, 0, 0, 0, 0, null)
}

fun getRandomRoomQuiz(quizId: Long): RoomQuiz {
    return RoomQuiz(quizId, "", "", 0, false)
}


class CoupledQuizWords(private val quizDao: QuizDao, private val wordDao: WordDao) {

    private val sampleRoomQuiz = listOf (
        RoomQuiz(1, "", "", 0, false),
        RoomQuiz(2, "Kanji: Verbs%行く 入る 会う...", "Kanji: Verbes%行く 入る 会う...", 2, false),
        RoomQuiz(3, "N4 - Vocabulary Part2%注意 火事 星...", "N4 - Vocabulaire Partie2%注意 火事 星...", 6, true)
    )

    private val sampleRoomWords = listOf (
        RoomWords(1, "", "", "", "", 0, 0,
            0, 0, 0, 0, 0, null),
        RoomWords(2, "金", "metal; Friday", "métal; vendredi", "きん",
            0, 0, 0, 0, -1, 0,
            2, null)
    )
    private val sampleRoomQuizWords = listOf (
        RoomQuizWord(1, 1),
        RoomQuizWord(3, 1),
        RoomQuizWord(3, 2)
    )

    fun addAllToDatabase() = runBlocking {
        for (sample in sampleRoomQuiz) {
            quizDao.addQuiz(sample)
        }
        for (sample in sampleRoomWords) {
            wordDao.addWord(sample)
        }
        for (sample in sampleRoomQuizWords) {
            quizDao.addQuizWord(sample)
        }
    }

    fun getWordIds(): List<Long> {
        return sampleRoomWords.map { it._id }
    }

    fun countWordsForQuizzes(quizIds: LongArray): Int {
        var count = 0
        for (id in getWordIds()) {
            for (roomQuizWord in sampleRoomQuizWords) {
                if (id != roomQuizWord.word_id) {
                    continue
                }
                if (quizIds.contains(roomQuizWord.quiz_id)) {
                    count++
                }
            }
        }
        return count
    }

    fun getWords(quizId: Long): List<RoomWords> {
        val wordIds = mutableListOf<Long>()
        for (roomQuizWord in sampleRoomQuizWords) {
            if (roomQuizWord.quiz_id == quizId)
                wordIds.add(roomQuizWord.word_id)
        }
        val words = mutableListOf<RoomWords>()
        for (word in sampleRoomWords) {
            if (word._id in wordIds)
                words.add(word)
        }
        return words
    }

    fun getWords(quizIds: LongArray): List<RoomWords> {
        val words = mutableListOf<RoomWords>()
        for (quizId in quizIds) {
            words += getWords(quizId)
        }
        return words
    }

    fun getWordsByLevels(quizIds: LongArray, levels: IntArray): List<RoomWords> {
        val words = getWords(quizIds)
        return words.filter { levels.contains(it.level) }
    }

    fun getWordsByRepetition(quizIds: LongArray, repetition: Int, limit: Int): List<RoomWords> {
        val words = getWords(quizIds)
        return words.filter { it.repetition == repetition }.sortedBy { it._id }.take(limit)
    }

    fun getWordIdsWithRepetitionStrictlyGreaterThan(quizIds: LongArray, repetition: Int): LongArray {
        val words = getWords(quizIds)
        return words.filter { it.repetition > repetition }.map { it._id }.toLongArray()
    }

}
