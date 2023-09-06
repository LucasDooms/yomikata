package com.jehutyno.yomikata.util


fun Int.toBool(): Boolean {
    return this != 0
}

fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}
