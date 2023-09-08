package com.jehutyno.yomikata.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.util.Category
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.getLevelFromPoints
import com.jehutyno.yomikata.util.getProgressToNextLevel
import com.jehutyno.yomikata.util.readableTranslationFormat
import com.jehutyno.yomikata.util.toCategory
import com.jehutyno.yomikata.util.toLevel
import java.io.Serializable
import java.util.*


/**
 * Created by valentin on 26/09/2016.
 */
data class Word(var id: Long, var japanese: String, var english: String, var french: String,
                var reading: String, var level: Level, var countSuccess: Int,
                var countFail: Int, var isKana: Int, var repetition: Int, var points: Int,
                var baseCategory: Category, var isSelected: Int, var sentenceId: Long?) : Parcelable, Serializable {

    constructor(source: Parcel): this(source.readLong(), source.readString()!!,
        source.readString()!!, source.readString()!!, source.readString()!!, source.readInt().toLevel(),
        source.readInt(), source.readInt(), source.readInt(), source.readInt(),
        source.readInt(), source.readInt().toCategory(), source.readInt(), source.readLong().takeIf { it != -1L } )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeString(japanese)
        dest.writeString(english)
        dest.writeString(french)
        dest.writeString(reading)
        dest.writeInt(level.level)
        dest.writeInt(countSuccess)
        dest.writeInt(countFail)
        dest.writeInt(isKana)
        dest.writeInt(repetition)
        dest.writeInt(points)
        dest.writeInt(baseCategory.index)
        dest.writeInt(isSelected)
        dest.writeLong(sentenceId ?: -1L)   // write as -1 if null
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("UNUSED")
        @JvmField val CREATOR: Parcelable.Creator<Word> = object : Parcelable.Creator<Word> {
            override fun createFromParcel(source: Parcel): Word{
                return Word(source)
            }

            override fun newArray(size: Int): Array<Word?> {
                return arrayOfNulls(size)
            }
        }
    }

    fun getTrad(): String {
        return (if (Locale.getDefault().language == "fr")
            french
        else
            english).readableTranslationFormat()
    }
}


fun getWordColor(context: Context, points: Int): Int {
    val level = getLevelFromPoints(points)
    val percentToNextLevel = (getProgressToNextLevel(points) * 100f).toInt()
    val color = when (level) {
        Level.LOW -> {
            if (percentToNextLevel < 25)
                R.color.level_low_1
            else if (percentToNextLevel < 50)
                R.color.level_low_2
            else if (percentToNextLevel < 75)
                R.color.level_low_3
            else
                R.color.level_low_4
        }
        Level.MEDIUM -> {
            if (percentToNextLevel < 25)
                R.color.level_medium_1
            else if (percentToNextLevel < 50)
                R.color.level_medium_2
            else if (percentToNextLevel < 75)
                R.color.level_medium_3
            else
                R.color.level_medium_4
        }
        Level.HIGH -> {
            if (percentToNextLevel < 25)
                R.color.level_high_1
            else if (percentToNextLevel < 50)
                R.color.level_high_2
            else if (percentToNextLevel < 75)
                R.color.level_high_3
            else
                R.color.level_high_4
        }
        Level.MASTER -> {
            if (percentToNextLevel < 25)
                R.color.level_master_1
            else if (percentToNextLevel < 50)
                R.color.level_master_2
            else if (percentToNextLevel < 75)
                R.color.level_master_3
            else
                R.color.level_master_4
        }
    }

    return ContextCompat.getColor(context, color)
}
