package today.sam.android.notificationreader

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

private const val T = "TTS"
class TTSQueue(applicationContext: Context) {
    val myTts = TextToSpeech(applicationContext) {
        Log.d(T, "TTS is ready")
    }
    val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(T, "afChange: $focusChange")
    }

    val queue = ConcurrentLinkedQueue<String>()
    var isConsuming = false

    fun enqueue(message: String) {
        queue.add(message)
        if (!isConsuming) {
            consume()
        }
    }

    private fun consume() {
        isConsuming = true

        val result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("TTS", "Duck request result $result != success")
        }

        consumeNext(true)
    }

    private fun consumeNext(isFirst: Boolean) {
        val message = queue.poll()
        if (message == null) {
            stopConsuming()
            return
        }

        Handler().postDelayed(Runnable {
            if (audioManager.isWiredHeadsetOn) {
                myTts!!.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            } else {
                Log.d(T, "No wired headset")
                stopConsuming()
            }
        }, if (isFirst) 1000 else 500)

        val isSpeakingHandler = Handler()
        val isSpeakingCheck = object : Runnable {
            override fun run() {
                if (myTts!!.isSpeaking) {
                    isSpeakingHandler.postDelayed(this, 300)
                } else {
                    consumeNext(false)
                }
            }
        }
        isSpeakingHandler.postDelayed(isSpeakingCheck, 1500)
    }

    private fun stopConsuming() {
        Log.d("TTS", "Finished speaking")
        audioManager.abandonAudioFocus(afChangeListener)
        queue.clear()
        isConsuming = false
    }
}