package today.sam.android.notificationreader

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

private const val T = "TTS"
class TTSQueue(applicationContext: Context) {
    val mTts = TextToSpeech(applicationContext) {
        Log.d(T, "TTS is ready")
    }
    val mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val mAfChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(T, "afChange: $focusChange")
    }

    private val mQueue = ConcurrentLinkedQueue<String>()
    private var mIsConsuming = false

    fun enqueue(message: String) {
        mQueue.add(message)
        if (!mIsConsuming) {
            consume()
        }
    }

    private fun consume() {
        mIsConsuming = true

        val result = mAudioManager.requestAudioFocus(mAfChangeListener, AudioManager.STREAM_NOTIFICATION, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("TTS", "Duck request result $result != success")
        }

        consumeNext(true)
    }

    private fun consumeNext(isFirst: Boolean) {
        val message = mQueue.poll()
        if (message == null) {
            stopConsuming()
            return
        }

        Handler().postDelayed(Runnable {
            if (mAudioManager.isWiredHeadsetOn) {
                mTts!!.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            } else {
                Log.d(T, "No wired headset")
                stopConsuming()
            }
        }, if (isFirst) 1000 else 500)

        val isSpeakingHandler = Handler()
        val isSpeakingCheck = object : Runnable {
            override fun run() {
                if (mTts!!.isSpeaking) {
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
        mAudioManager.abandonAudioFocus(mAfChangeListener)
        mQueue.clear()
        mIsConsuming = false
    }
}