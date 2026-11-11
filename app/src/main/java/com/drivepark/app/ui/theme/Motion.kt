package com.drivepark.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** 화면 전환: 새 화면은 8dp 아래에서 올라오며(220ms) 나타나고, 이전 화면은 150ms에 흐려진다. */
object PleosMotion {
    val enter: EnterTransition = fadeIn(tween(220)) + slideInVertically(tween(220)) { 16 }
    val exit: ExitTransition = fadeOut(tween(150))
}

/**
 * 반복 애니메이션(캐릭터 걷기, 조명 숨쉬기)을 돌려도 되는지. 주행 중에는 false.
 * Pleos는 주행 중에도 앱을 분할 칸에 띄워 두고 잠금 화면으로만 가리므로, 직접 멈추지 않으면
 * 보이지 않는 공원이 초당 60번 다시 그려져 내비와 CPU를 나눠 쓴다(2026-11-06 측정: 약 14%).
 */
val LocalAnimationsEnabled = compositionLocalOf { true }

/**
 * 지금 주행 중인지. 따로 뜨는 창(대화상자)은 주행 잠금 화면 위에 그려지므로,
 * 창을 띄우는 화면은 이 값이 true면 창을 닫는다([com.drivepark.app.domain.DriveGuard]).
 */
val LocalDriving = compositionLocalOf { false }

/** 누르면 0.97배로 줄었다가 돌아온다. 같은 interactionSource를 clickable에 넘긴다. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
