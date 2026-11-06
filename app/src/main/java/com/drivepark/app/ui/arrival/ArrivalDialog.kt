package com.drivepark.app.ui.arrival

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.drivepark.app.domain.ArrivalSummary
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.arrivalMessage
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.character.MascotAvatar
import com.drivepark.app.ui.theme.Pleos
import kotlinx.coroutines.delay

/** 도착 요약 바닥 시트. 친구 캐릭터가 0.08초 간격으로 튀어오르며 등장한다. */
@Composable
fun ArrivalDialog(summary: ArrivalSummary?, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(summary != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier.fillMaxSize().background(Pleos.Scrim.copy(alpha = 0.4f))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            summary != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(),
        ) {
            summary?.let { Sheet(it, onDismiss) }
        }
    }
}

@Composable
private fun Sheet(summary: ArrivalSummary, onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Pleos.Background, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 40.dp, vertical = 32.dp),
    ) {
        Text("도착했어요!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(summary.briefing ?: arrivalMessage(summary), style = MaterialTheme.typography.titleMedium)
        if (summary.friends.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                summary.friends.forEachIndexed { i, c -> PopIn(i) { FriendChip(c) } }
            }
        }
        summary.newRegion?.let { region ->
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PopIn(summary.friends.size) { MascotAvatar(region.id, Modifier.size(96.dp)) }
                Spacer(Modifier.width(16.dp))
                Text("${region.name}의 ${region.mascotName}", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End).height(56.dp)) { Text("공원 보기") }
    }
}

@Composable
private fun FriendChip(c: Character) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CharacterAvatar(c.parts, Modifier.size(80.dp))
        Text(c.nickname, style = MaterialTheme.typography.labelMedium, color = Pleos.TextSecondary)
    }
}

/** index × 0.08초 뒤에 아래에서 튀어오른다. */
@Composable
private fun PopIn(index: Int, content: @Composable () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L * index)
        anim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Box(Modifier.graphicsLayer {
        alpha = anim.value.coerceIn(0f, 1f)
        translationY = (1f - anim.value) * 40f
        scaleX = 0.6f + 0.4f * anim.value
        scaleY = 0.6f + 0.4f * anim.value
    }) { content() }
}
