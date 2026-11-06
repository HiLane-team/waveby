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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** 화면 전환: 새 화면은 8dp 아래에서 올라오며(220ms) 나타나고, 이전 화면은 150ms에 흐려진다. */
object PleosMotion {
    val enter: EnterTransition = fadeIn(tween(220)) + slideInVertically(tween(220)) { 16 }
    val exit: ExitTransition = fadeOut(tween(150))
}

/** 누르면 0.97배로 줄었다가 돌아온다. 같은 interactionSource를 clickable에 넘긴다. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
