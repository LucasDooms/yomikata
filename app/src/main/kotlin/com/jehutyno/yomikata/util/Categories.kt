package com.jehutyno.yomikata.util

import com.jehutyno.yomikata.R
import java.security.InvalidParameterException


/**
 * Created by valentin on 04/10/2016.
 */
enum class Category(val index: Int) {
    // Reminder: Changing any of the index values affects both the database and user preferences
    HOME(-1),
    SELECTIONS(8),
    HIRAGANA(0),
    KATAKANA(1),
    KANJI(2),
    COUNTERS(9),
    JLPT_5(7),
    JLPT_4(6),
    JLPT_3(5),
    JLPT_2(4),
    JLPT_1(3)
}

fun Int.toCategory(): Category {
    return Category.values().firstOrNull { it.index == this }!!
}

fun Category.getSmallIcon(): Int {
    return when (this) {
        Category.HIRAGANA -> R.drawable.ic_hiragana
        Category.KATAKANA -> R.drawable.ic_katakana
        Category.KANJI    -> R.drawable.ic_kanji
        Category.COUNTERS -> R.drawable.ic_counters
        Category.JLPT_5   -> R.drawable.ic_jlpt5
        Category.JLPT_4   -> R.drawable.ic_jlpt4
        Category.JLPT_3   -> R.drawable.ic_jlpt3
        Category.JLPT_2   -> R.drawable.ic_jlpt2
        Category.JLPT_1   -> R.drawable.ic_jlpt1
        else              -> R.drawable.ic_kanji
    }
}

fun Category.getBigIcon(): Int {
    return when (this) {
        Category.HIRAGANA -> R.drawable.ic_hiragana_big
        Category.KATAKANA -> R.drawable.ic_katakana_big
        Category.KANJI    -> R.drawable.ic_kanji_big
        Category.COUNTERS -> R.drawable.ic_counters_big
        Category.JLPT_5   -> R.drawable.ic_jlpt5_big
        Category.JLPT_4   -> R.drawable.ic_jlpt4_big
        Category.JLPT_3   -> R.drawable.ic_jlpt3_big
        Category.JLPT_2   -> R.drawable.ic_jlpt2_big
        Category.JLPT_1   -> R.drawable.ic_jlpt1_big
        else              -> R.drawable.ic_selections_big
    }
}

fun Category.getLevel(): Int {
    return when (this) {
        Category.HOME -> throw InvalidParameterException("HOME category has no level")
        Category.SELECTIONS -> throw InvalidParameterException("SELECTIONS category has no level")
        Category.HIRAGANA -> 0
        Category.KATAKANA -> 0
        Category.KANJI -> 1
        Category.COUNTERS -> 1
        Category.JLPT_5 -> 2
        Category.JLPT_4 -> 3
        Category.JLPT_3 -> 4
        Category.JLPT_2 -> 5
        Category.JLPT_1 -> 6
    }
}

/**
 * Get level download size
 *
 * Returns the estimated size in MB of the download
 *
 * @param level
 * @return Size in MB
 */
fun getLevelDownloadSize(level: Int): Int {
    return when (level) {
        0 -> 5
        1 -> 6
        2 -> 6
        3 -> 7
        4 -> 35
        5 -> 41
        else -> 56
    }
}

fun getLevelDownloadVersion(level: Int): Int {
    return when (level) {
        0 -> 0
        1 -> 2
        2 -> 2
        3 -> 1
        4 -> 1
        5 -> 1
        6 -> 1
        else -> 0
    }
}
