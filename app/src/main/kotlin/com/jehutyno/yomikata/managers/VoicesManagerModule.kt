package com.jehutyno.yomikata.managers

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


/**
 * Created by valentin on 25/10/2016.
 */
fun voicesManagerModule() = DI.Module("voicesManagerModule") {
    // do not use singleton, because VoicesManager may release ExoPlayer,
    // after which it should no longer be used
    bind<VoicesManager>() with provider { VoicesManager(instance()) }
}
