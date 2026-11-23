package com.koreana.prototype

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/** 누가 말하는지에 따라 음높이만 다르게 한다 (목업). */
enum class Speaker(val pitch: Float, val rate: Float) {
    Jiwoo(1.15f, 1f),
    Model(1f, 0.95f),
    Me(0.85f, 1f),
    FutureMe(0.95f, 1.1f),
}

/**
 * 목업 음성. 기기의 한국어 TTS로 대사를 읽고, TTS가 없으면 글자 수로 어림한 시간만큼 기다린다.
 * 실제 녹음·발음 평가는 하지 않는다.
 */
class Voice(context: Context) {
    private var ready = false
    private val waiting = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private var counter = 0

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = engine().setLanguage(Locale.KOREAN)
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun engine() = tts

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun finish(id: String?) {
        val c = waiting.remove(id ?: return) ?: return
        if (c.isActive) c.resume(Unit)
    }

    /** 대사 길이로 어림한 재생 시간(ms). 자막 강조와 재생 막대에 쓴다. */
    fun estimateMs(text: String, speed: Float = 1f): Long =
        ((450 + text.count { !it.isWhitespace() } * 185) / speed).toLong()

    suspend fun speak(text: String, speaker: Speaker = Speaker.Jiwoo, speed: Float = 1f) {
        val estimate = estimateMs(text, speed)
        if (!ready) {
            delay(estimate)
            return
        }
        val id = "u${++counter}"
        withTimeoutOrNull(estimate * 3 + 3000) {
            suspendCancellableCoroutine<Unit> { cont ->
                waiting[id] = cont
                cont.invokeOnCancellation {
                    waiting.remove(id)
                    tts.stop()
                }
                tts.setPitch(speaker.pitch)
                tts.setSpeechRate(speaker.rate * speed)
                val code = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                if (code != TextToSpeech.SUCCESS) finish(id)
            }
        }
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

val LocalVoice = staticCompositionLocalOf<Voice> { error("Voice not provided") }
