package com.jehutyno.yomikata.screens.content

import android.os.Bundle
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.getSerializableHelper
import org.kodein.di.DI


class ContentQuizFragment(di: DI): ContentFragment(di) {

    private lateinit var quiz: Quiz

    override val level = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().also { args ->
            quiz = args.getSerializableHelper(Extras.EXTRA_QUIZ, Quiz::class.java)!!
        }
    }

    override fun getQuizTitle(): String {
        return quiz.getName().split("%")[0]
    }
}
