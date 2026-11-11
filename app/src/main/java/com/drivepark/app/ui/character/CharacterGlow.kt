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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.drivepark.app.ui.theme.LocalAnimationsEnabled
import com.drivepark.app.ui.theme.Pleos

/** 숨쉬기 한 번(들숨+날숨) 길이. 백호 깜빡임 간격과 같은 3.4초. */
private const val BREATH_MS = 3400

/** 애니메이션을 멈췄을 때 쓰는 숨 위치(중간 밝기). */
private const val RESTING_BREATH = 0.5f

/**
 * 캐릭터 뒤에 은은한 조명을 깔고 천천히 숨쉬듯 밝아졌다 어두워진다.
 * 색은 테마 토큰 Pleos.Glow(라이트: 살구빛 #FFB27A, 다크: 호박빛 가로등 #FFC27A).
 *
 * 조명 원은 한 번만 그리고, 숨쉬기는 그 층의 투명도·크기만 바꾼다(GPU가 처리해 매 프레임 다시 그리지 않음).
 * [LocalAnimationsEnabled]가 false면(주행 중, 공원이 쉬는 중) 숨쉬지 않고 중간 밝기로 멈춰 있다.
 */
@Composable
fun CharacterGlow(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val glow = Pleos.Glow
    // 라이트는 흰 배경이라 조금 더 진하게 해야 보인다.
    val peak = if (Pleos.IsDark) 0.42f else 0.55f
    Box(modifier, contentAlignment = Alignment.Center) {
        if (LocalAnimationsEnabled.current) {
            val breath = rememberInfiniteTransition(label = "glow").animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(BREATH_MS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "breath",
            )
            GlowLayer(glow, peak) { breath.value }
        } else {
            GlowLayer(glow, peak) { RESTING_BREATH }
        }
        content()
    }
}

/** [breath]는 graphicsLayer 블록 안에서만 읽는다. 값이 바뀌어도 조합·그리기를 다시 하지 않는다. */
@Composable
private fun BoxScope.GlowLayer(glow: Color, peak: Float, breath: () -> Float) {
    Canvas(
        Modifier.matchParentSize().graphicsLayer {
            val b = breath()
            alpha = 0.45f + 0.55f * b
            val s = 0.86f + 0.14f * b
            scaleX = s
            scaleY = s
        },
    ) {
        val radius = size.minDimension / 2
        drawCircle(
            Brush.radialGradient(
                0f to glow.copy(alpha = peak),
                0.55f to glow.copy(alpha = peak * 0.45f),
                1f to Color.Transparent,
                center = center, radius = radius,
            ),
            radius = radius,
        )
    }
}
