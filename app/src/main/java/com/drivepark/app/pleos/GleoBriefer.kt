package com.drivepark.app.pleos

import android.content.Context
import android.util.Log
import ai.pleos.playground.llm.LLM
import ai.pleos.playground.llm.constant.Mode
import ai.pleos.playground.llm.listener.ResultListener
import com.drivepark.app.domain.ArrivalSummary
import com.drivepark.app.domain.Briefer
import com.drivepark.app.domain.BriefingPrompt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 글레오(Pleos LLM SDK, 온디바이스)로 도착 브리핑을 만든다. 권한: pleos.car.permission.LLM_SERVICE (normal).
 * 실패하면 null(ArrivalCoordinator가 정해진 문장으로 대체한다). 타임아웃은 호출하는 쪽이 건다.
 */
class GleoBriefer(context: Context) : Briefer {
    private val llm: LLM? = runCatching {
        LLM(context.applicationContext, Mode.ON_DEVICE).also { it.initialize() }
    }.onFailure { Log.w(TAG, "LLM unavailable", it) }.getOrNull()

    override suspend fun brief(summary: ArrivalSummary): String? {
        val model = llm ?: return null
        val prompt = BriefingPrompt.build(summary)
        return suspendCancellableCoroutine { cont ->
            val collected = StringBuilder()
            runCatching {
                model.generateContent(prompt, object : ResultListener {
                    override fun onResponse(text: String, isFinal: Boolean) {
                        collected.append(text)
                        if (isFinal && cont.isActive) {
                            Log.d(TAG, "briefing length=${collected.length}")
                            cont.resume(collected.toString())
                        }
                    }

                    override fun onError(message: String) {
                        Log.w(TAG, "gleo error: $message")
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }.onFailure {
                Log.w(TAG, "generateContent threw", it)
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private companion object {
        const val TAG = "DrivePark/Gleo"
    }
}
