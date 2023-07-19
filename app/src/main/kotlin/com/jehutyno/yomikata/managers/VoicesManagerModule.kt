package com.jehutyno.yomikata.managers

import android.speech.tts.TextToSpeech.OnInitListener
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance


/**
 * Created by valentin on 25/10/2016.
 */
fun voicesManagerModule() = DI.Module("voicesManagerModule") {
    // do not use singleton/multiton, because VoicesManager may release ExoPlayer,
    // after which it should no longer be used.
    // Make sure the context binding can be used with alertDialog (don't use application context)
    bind<VoicesManager>() with factory {
        onInitListener: OnInitListener -> VoicesManager(instance(), onInitListener)
    }
}
