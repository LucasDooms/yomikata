package com.jehutyno.yomikata.managers

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.util.*


/**
 * Created by valentinlanfranchi on 01/09/2017.
 */
/**
 * Voices manager
 *
 * Used to play audio of sentences or words. Uses either google tts (text-to-speech),
 * or downloaded voices, depending on which is available.
 *
 * This should always be called from the Main Thread.
 *
 * Remember to call releasePlayer() in the onDestroy method of an activity/fragment!
 *
 * @property context Context
 * @constructor Create Voices manager
 */
class VoicesManager(private val context: Context) {

    private val exoPlayer = ExoPlayer.Builder(context).build()

    private fun playUriWhenReady(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun speakSentence(sentence: Sentence, ttsSupported: Int, tts: TextToSpeech?) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            val toast = Toast.makeText(context, R.string.message_adjuste_volume, Toast.LENGTH_LONG)
            toast.show()
        }
        when (checkSpeechAvailability(context, ttsSupported, sentence.level)) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                try {
                    val uri = Uri.parse("${FileUtils.getDataDir(context, "Voices").absolutePath}/s_${sentence.id}.mp3")
                    playUriWhenReady(uri)
                } catch (e: Exception) {
                    speechNotSupportedAlert(context, sentence.level) {}
                }
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(sentenceNoFuri(sentence), TextToSpeech.QUEUE_FLUSH, null, null)
                } else {    // remove this if minBuildVersion >= 21 (LOLLIPOP)
                    @Suppress("DEPRECATION")
                    tts?.speak(sentenceNoFuri(sentence), TextToSpeech.QUEUE_FLUSH, null)
                }
            }
            else -> speechNotSupportedAlert(context, sentence.level) {}
        }
    }

    fun speakWord(word: Word, ttsSupported: Int, tts: TextToSpeech?) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            val toast = Toast.makeText(context, R.string.message_adjuste_volume, Toast.LENGTH_SHORT)
            toast.show()
        }
        val level = getCategoryLevel(word.baseCategory)

        when (checkSpeechAvailability(context, ttsSupported, level)) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                try {
                    val uri = Uri.parse("${FileUtils.getDataDir(context, "Voices").absolutePath}/w_${word.id}.mp3")
                    playUriWhenReady(uri)
                } catch (e: Exception) {
                    speechNotSupportedAlert(context, level) {}
                }
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(if (word.isKana >= 1)
                        word.japanese.split("/")[0].split(";")[0]
                    else
                        word.reading.split("/")[0].split(";")[0],
                            TextToSpeech.QUEUE_FLUSH, null,null)
                } else {    // remove this if minBuildVersion >= 21 (LOLLIPOP)
                    @Suppress("DEPRECATION")
                    tts?.speak(if (word.isKana >= 1)
                        word.japanese.split("/")[0].split(";")[0]
                    else
                        word.reading.split("/")[0].split(";")[0],
                            TextToSpeech.QUEUE_FLUSH, null)
                }
            }
            else -> speechNotSupportedAlert(context, level) {}
        }
    }

    /**
     * Release player
     *
     * Call this when you no longer need the ExoPlayer.
     */
    fun releasePlayer() {
        exoPlayer.release()
    }

}
