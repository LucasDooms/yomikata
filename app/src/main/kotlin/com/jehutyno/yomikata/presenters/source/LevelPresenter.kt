package com.jehutyno.yomikata.presenters.source

import com.jehutyno.yomikata.presenters.LevelInterface
import com.jehutyno.yomikata.repository.WordRepository


class LevelPresenter(private val wordRepository: WordRepository): LevelInterface {

    override suspend fun levelUp(ids: LongArray, points: IntArray) {
        val newPoints = points.map{ com.jehutyno.yomikata.util.levelUp(it) }.toIntArray()
        wordRepository.updateWordPoints(ids, newPoints)
    }

    override suspend fun levelDown(ids: LongArray, points: IntArray) {
        val newPoints = points.map { com.jehutyno.yomikata.util.levelDown(it) }.toIntArray()
        wordRepository.updateWordPoints(ids, newPoints)
    }

}
