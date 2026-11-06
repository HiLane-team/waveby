package com.drivepark.app.pleos

import android.content.Context
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import com.drivepark.app.domain.Greeter
import java.util.Locale

/**
 * Pleos TTS(ON_DEVICE 모드, 앱 등록 불필요)로 인사말을 읽는다. 예외를 밖으로 던지지 않는다.
 * 초기화가 끝나기 전에 들어온 문장은 마지막 하나만 기억했다가 준비되면 읽는다.
 */
class PleosGreeter(context: Context) : Greeter {
    @Volatile private var ready = false
    @Volatile private var queued: String? = null

    private val events = object : EventListener {
        override fun onStart() { Log.d(TAG, "onStart") }
        override fun onReady() { Log.d(TAG, "onReady") }
        override fun onDone() { Log.d(TAG, "onDone") }
        override fun onError(error: ErrorType) { Log.w(TAG, "onError: $error") }
        override fun onStop() { Log.d(TAG, "onStop") }
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private val tts: TextToSpeech? = runCatching {
        TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    }.onFailure { Log.w(TAG, "tts create failed", it) }.getOrNull()

    init {
        runCatching {
            tts?.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    Log.d(TAG, "tts ready")
                    runCatching { tts.addEventListener(events) }
                    // 국가 코드가 없으면(ko) 온디바이스 엔진이 "unsupported locale: ko-"로 실패한다. ko_KR이어야 한다.
                    runCatching { tts.setLocale(Locale.KOREA) }.onFailure { Log.w(TAG, "setLocale failed", it) }
                    ready = true
                    queued?.let { queued = null; say(it) }
                }

                override fun onFailure(error: ErrorType) {
                    Log.w(TAG, "tts init failed: $error")
                }
            })
        }.onFailure { Log.w(TAG, "tts init threw", it) }
    }

    override fun speak(text: String) {
        Log.d(TAG, "speak(ready=$ready, length=${text.length})")
        if (ready) say(text) else queued = text
    }

    private fun say(text: String) {
        // 두 번째 인자의 기본값은 true(문장 끝). false로 보내면 온디바이스 스트림이 다음 조각을 기다리며 멈춘다.
        runCatching { tts?.speak(text) }.onFailure { Log.w(TAG, "speak failed", it) }
    }

    private companion object {
        const val TAG = "DrivePark/TTS"
    }
}
