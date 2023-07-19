package com.jehutyno.yomikata.managers

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.COMMAND_RELEASE
import androidx.media3.common.Player.COMMAND_SET_SPEED_AND_PITCH
import androidx.media3.common.Player.COMMAND_SET_VOLUME
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
 * Remember to call [destroy] in the onDestroy method of an activity/fragment!
 *
 * @property context Context
 * @param onInitListener Something that implements onInit (usually, the activity or fragment itself)
 * @constructor Create Voices manager
 */
class VoicesManager(private val context: Context, private val onInitListener: OnInitListener): OnInitListener {

    private val tts = TextToSpeech(context, this)
    private var ttsSupported = TextToSpeech.LANG_NOT_SUPPORTED
    private val exoPlayer = ExoPlayer.Builder(context).build()
    private val volumeWarning: Toast = Toast.makeText(
        context, R.string.message_adjuste_volume, Toast.LENGTH_LONG
    )
    private var ttsParams = Bundle()

    init {
        setVolume(1.0f)
    }

    override fun onInit(status: Int) {
        ttsSupported = context.onTTSinit(status, tts)
        onInitListener.onInit(status)
    }

    private fun ttsToHashMap(): HashMap<String, String> {
        val hashMap = HashMap<String, String>()
        val getVol = TextToSpeech.Engine.KEY_PARAM_VOLUME
        hashMap[getVol] = ttsParams.getFloat(getVol).toString()
        return hashMap
    }

    /**
     * Set speech rate
     *
     * @param rate Factor to change speed by (e.g. 1.0 is normal, 0.5 is half speed)
     */
    fun setSpeechRate(rate: Float) {
        tts.setSpeechRate(rate)
        if (exoPlayer.isCommandAvailable(COMMAND_SET_SPEED_AND_PITCH)) {
            exoPlayer.setPlaybackSpeed(rate)
        }
    }

    /**
     * Set volume
     *
     * @param volume Factor between 0.0f (silence) and 1.0f (normal device audio)
     */
    fun setVolume(@FloatRange(from = 0.0, to = 1.0) volume: Float) {
        ttsParams.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        if (exoPlayer.isCommandAvailable(COMMAND_SET_VOLUME)) {
            exoPlayer.volume = volume
        }
    }

    fun getSpeechAvailability(level: Int): SpeechAvailability {
        return checkSpeechAvailability(context, ttsSupported, level)
    }

    private fun playUriWhenReady(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun warningLowVolume() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            volumeWarning.cancel()
            volumeWarning.show()
        }
    }

    /**
     * Speak TTS
     *
     * Speaks some text using the set ttsParams
     *
     * @param text Text to say
     */
    private fun speakTTS(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParams, null)
        } else {    // remove this if minBuildVersion >= 21 (LOLLIPOP)
            @Suppress("DEPRECATION")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, ttsToHashMap())
        }
    }

    /**
     * Speak exo
     *
     * Speaks the voice file stored in the voices folder, e.g s_20.mp3 is sentence with id = 20.
     *
     * @param prefix "s" for sentence, "w" for word
     * @param id Id of the word/sentence, should correspond to the voice file
     * @param downloadLevel Used if voices not downloaded, to get the specific level to download
     */
    private fun speakExo(prefix: String, id: Long, downloadLevel: Int) {
        try {
            val uri = Uri.parse(
                "${FileUtils.getDataDir(context, "Voices").absolutePath}/${prefix}_${id}.mp3"
            )
            playUriWhenReady(uri)
        } catch (e: Exception) {
            speechNotSupportedAlert(context, downloadLevel) {}
        }
    }

    /**
     * Speak sentence
     *
     * Will stop any previous sound and play the sentence.
     *
     * @param sentence Sentence to play
     * @param userAction True if the user chose to play this, false otherwise
     */
    fun speakSentence(sentence: Sentence, userAction: Boolean) {
        if (userAction) // don't display warning if the audio is automatic (in case user doesn't want volume)
            warningLowVolume()

        when (getSpeechAvailability(sentence.level)) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                speakExo("s", sentence.id, sentence.level)
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                speakTTS(sentenceNoFuri(sentence))
            }
            else -> speechNotSupportedAlert(context, sentence.level) {}
        }
    }

    /**
     * Speak word
     *
     * Will stop any previous sound and play the word.
     *
     * @param word Word to play
     * @param userAction True if the user chose to play this, false otherwise
     */
    fun speakWord(word: Word, userAction: Boolean) {
        if (userAction) // don't display warning if the audio is automatic (in case user doesn't want volume)
            warningLowVolume()

        val level = getCategoryLevel(word.baseCategory)

        when (getSpeechAvailability(level)) {
            SpeechAvailability.VOICES_AVAILABLE -> {
                speakExo("w", word.id, level)
            }
            SpeechAvailability.TTS_AVAILABLE -> {
                val say = if (word.isKana >= 1)
                        // if not kanji, take the japanese
                    word.japanese.split("/")[0].split(";")[0]
                else    // if kanji, use the reading
                    word.reading.split("/")[0].split(";")[0]

                speakTTS(say)
            }
            else -> speechNotSupportedAlert(context, level) {}
        }
    }

    /**
     * Stop
     *
     * Stops the audio, you should call this in onPause to prevent audio from continuing
     * after the user closes the app.
     */
    fun stop() {
        exoPlayer.stop()
        tts.stop()
    }

    /**
     * Destroy
     *
     * Call this when you no longer need the ExoPlayer and TTS.
     * Do not use the VoicesManager after calling this.
     */
    fun destroy() {
        if (exoPlayer.isCommandAvailable(COMMAND_RELEASE)) {
            exoPlayer.release()
        }
        tts.stop()
        tts.shutdown()
    }

}
