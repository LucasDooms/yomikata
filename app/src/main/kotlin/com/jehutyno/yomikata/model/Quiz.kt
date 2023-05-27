package com.jehutyno.yomikata.model

import java.io.Serializable
import java.util.Locale


/**
 * Created by valentin on 27/09/2016.
 */
data class Quiz(val id: Long, var nameEn: String, var nameFr: String,
                val category: Int, var isSelected: Boolean): Serializable {

    fun getName(): String {
        return if (Locale.getDefault().language == "fr")
            nameFr
        else
            nameEn
    }

}
