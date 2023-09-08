package com.jehutyno.yomikata.furigana.utils

/**
 * Created by lorand on 2016-04-14.
 */
object FuriganaUtils {
    /**
     * The method parseRuby converts kanji enclosed in ruby tags to the
     * format which is supported by the textview {Kanji:furigana}
     *
     * @param textWithRuby
     */
    fun parseRuby(textWithRuby: String): String {
        var parsed = textWithRuby.replace("<ruby>", "{")
        parsed = parsed.replace("<rt>", ";")
        parsed = parsed.replace("</rt>", "")
        return parsed.replace("</ruby>", "}")
    }
}
