package com.drivepark.app.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 시작 화면: 내 캐릭터가 있으면 공원, 없으면 초기 설정. */
fun startRoute(hasCharacter: Boolean): String = if (hasCharacter) "park" else "onboarding"

/** 초기 설정 5단계(환영, 권한, 누구, 캐릭터, 완료)의 현재 단계. 음성·시동 화면 같은 취향은 기본값으로 두고 설정에서 바꾼다. */
class OnboardingViewModel : ViewModel() {
    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    fun next() = _step.update { (it + 1).coerceAtMost(LAST_STEP) }
    fun back() = _step.update { (it - 1).coerceAtLeast(0) }

    companion object {
        const val STEP_WHO = 2
        const val STEP_CHARACTER = 3
        const val LAST_STEP = 4
    }
}
