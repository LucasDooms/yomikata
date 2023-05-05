package com.jehutyno.yomikata.screens.quiz

import android.os.Bundle
import com.jehutyno.yomikata.BasePresenter
import com.jehutyno.yomikata.BaseView
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.QuizType


/**
 * Created by valentin on 18/10/2016.
 */
interface QuizContract {

    interface View : BaseView<Presenter> {
        fun displayWords(quizWordsPair: List<Pair<Word, QuizType>>)
        fun noWords()
        fun setHiraganaConversion(enabled: Boolean)
        fun displayQCMMode(hintText: String? = null)
        fun displayEditMode()
        fun displayQCMNormalTextViews()
        fun displayQCMFuriTextViews()
        fun displayQCMTv(tvNum: Int, option: String, color: Int)
        fun displayQCMFuri(furiNum: Int, optionFuri: String, start: Int, end: Int, color: Int)
        fun showKeyboard()
        fun hideKeyboard()
        fun animateColor(position: Int, word: Word, sentence: Sentence, quizType: QuizType, fromPoints: Int, toPoints: Int)
        fun setEditTextColor(color: Int)
        fun animateCheck(result: Boolean)
        fun reInitUI()
        fun showAlertProgressiveSessionEnd(proposeErrors: Boolean)
        fun setPagerPosition(position: Int)
        fun finishQuiz()
        fun showAlertNonProgressiveSessionEnd(proposeErrors: Boolean)
        fun showAlertQuizEnd(proposeErrors: Boolean)
        fun showAlertErrorSessionEnd(quizEnded: Boolean)
        fun clearEdit()
        fun displayEditAnswer(answer: String)
        fun displayEditDisplayAnswerButton()
        fun openAnswersScreen(answers: ArrayList<Answer>)
        fun setSentence(sentence: Sentence)
        fun reportError(word: Word, sentence: Sentence)
        fun speakWord(word: Word)
        fun launchSpeakSentence(sentence: Sentence)
    }

    interface Presenter : BasePresenter {
        suspend fun getWords(): List<Word>
        suspend fun getSelections(): List<Quiz>
        suspend fun loadWords(): List<Pair<Word, QuizType>>
        suspend fun createSelection(quizName: String): Long
        suspend fun addWordToSelection(wordId: Long, quizId: Long)
        suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean
        suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean>
        suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long)
        suspend fun updateWordLevel(wordId: Long, level: Level)
        suspend fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word>
        suspend fun updateWordPoints(wordId: Long, points: Int)
        suspend fun getNextProgressiveWords(): List<Pair<Word, QuizType>>
        suspend fun getWord(id: Long): Word?
        suspend fun initQuiz()
        suspend fun updateRepetitions(id: Long, repetition: Int)
        suspend fun decreaseAllRepetitions()
        suspend fun saveAnswerResultStat(word: Word, result: Boolean)
        suspend fun saveWordSeenStat(word: Word)
        fun setTTSSupported(ttsSupported: Int)
        fun getTTSForCurrentItem(): String
        suspend fun setIsFuriDisplayed(isFuriDisplayed: Boolean)
        suspend fun onNextWord()
        suspend fun onAnswerGiven(answer: String)
        suspend fun onLaunchErrorSession()
        fun onFinishQuiz()
        suspend fun setUpNextQuiz()
        suspend fun onLaunchNextProgressiveSession()
        suspend fun onContinueQuizAfterErrorSession()
        suspend fun onRestartQuiz()
        suspend fun onContinueAfterNonProgressiveSessionEnd()
        fun onEditActionClick()
        fun onSaveInstanceState(outState: Bundle)
        fun onRestoreInstanceState(savedInstanceState: Bundle)
        suspend fun onOptionClick(choice: Int)
        fun onDisplayAnswersClick()
        suspend fun getRandomSentence(word: Word): Sentence
        fun onSpeakSentence()
        fun onSpeakWordTTS()
        fun onReportClick(position: Int)
        fun previousAnswerWrong(): Boolean
    }

}
