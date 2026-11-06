package com.drivepark.app.ui.character

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.drivepark.app.ui.theme.Pleos

/** 숨쉬기 한 번(들숨+날숨) 길이. 백호 깜빡임 간격과 같은 3.4초. */
private const val BREATH_MS = 3400

/**
 * 캐릭터 뒤에 은은한 조명을 깔고 천천히 숨쉬듯 밝아졌다 어두워진다.
 * 색은 테마 토큰 Pleos.Glow(라이트: 살구빛 #FFB27A, 다크: 호박빛 가로등 #FFC27A).
 */
@Composable
fun CharacterGlow(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val glow = Pleos.Glow
    // 라이트는 흰 배경이라 조금 더 진하게 해야 보인다.
    val peak = if (Pleos.IsDark) 0.42f else 0.55f
    val breath by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val alpha = peak * (0.45f + 0.55f * breath)
            val radius = size.minDimension / 2 * (0.86f + 0.14f * breath)
            drawCircle(
                Brush.radialGradient(
                    0f to glow.copy(alpha = alpha),
                    0.55f to glow.copy(alpha = alpha * 0.45f),
                    1f to Color.Transparent,
                    center = center, radius = radius,
                ),
                radius = radius,
            )
        }
        content()
    }
}
