package com.jehutyno.yomikata.screens.content

import android.os.Bundle
import android.view.View
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.getSerializableHelper
import org.kodein.di.DI


class ContentLevelFragment(di: DI): ContentFragment(di) {

    private lateinit var quizIds: LongArray
    override lateinit var level: Level

    override fun onCreate(savedInstanceState: Bundle?) {
        requireArguments().also { args ->
            quizIds = args.getLongArray(Extras.EXTRA_QUIZ_IDS)!!
            level = args.getSerializableHelper(Extras.EXTRA_LEVEL, Level::class.java)!!
        }
        // make sure level is assigned before calling super
        super.onCreate(savedInstanceState)
    }

    override fun displayStats() {
        super.displayStats()
        // no need to update visibilities using LiveData, since it is only set once per fragment
        binding.seekLowContainer.visibility = if (level == Level.LOW) View.VISIBLE else View.GONE
        binding.seekMediumContainer.visibility = if (level == Level.MEDIUM) View.VISIBLE else View.GONE
        binding.seekHighContainer.visibility = if (level == Level.HIGH) View.VISIBLE else View.GONE
        binding.seekMasterContainer.visibility =
            if (level == Level.MASTER || level == Level.MAX) View.VISIBLE else View.GONE
    }

    override fun getQuizTitle(): String {
        return when (level) {
            Level.LOW -> getString(R.string.red_review)
            Level.MEDIUM -> getString(R.string.orange_review)
            Level.HIGH -> getString(R.string.yellow_review)
            Level.MASTER, Level.MAX -> getString(R.string.green_review)
        }
    }
}
