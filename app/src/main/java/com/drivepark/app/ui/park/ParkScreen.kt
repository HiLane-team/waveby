package com.drivepark.app.ui.park

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drivepark.app.domain.Character
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.character.CharacterGlow
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.theme.Pleos
import com.drivepark.app.ui.theme.pressScale
import kotlin.math.hypot
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParkScreen(vm: ParkViewModel, paused: Boolean, onTogglePause: () -> Unit, theme: ParkTheme = if (Pleos.IsDark) PleosNight else PleosLight) {
    val state by vm.state.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) } // 말풍선을 띄운 캐릭터 key

    Box(Modifier.fillMaxSize().background(theme.ground)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 72.dp)) {
            ParkScenery(theme)
            val areaW = maxWidth
            val areaH = maxHeight
            // 사색 중: 친구들은 공원에 없고, 내 캐릭터만 첫 벤치에 앉아 있다.
            if (paused) {
                state.me?.let { me ->
                    val bench = theme.props.first { it.kind == PropKind.BENCH }
                    Box(Modifier.offset(areaW * bench.x - AVATAR / 2, areaH * bench.y - AVATAR + 12.dp)) {
                        CharacterGlow(Modifier.size(AVATAR)) { CharacterAvatar(me.parts, Modifier.size(AVATAR), pose = Pose.SIT) }
                    }
                }
            } else {
                state.me?.let { me ->
                    Wanderer("me", me, areaW, areaH, selected == "me", isMe = true) { selected = if (selected == "me") null else "me" }
                }
                state.wandering.forEach { friend ->
                    val key = "f${friend.id}"
                    Wanderer(key, friend.character, areaW, areaH, selected == key) { selected = if (selected == key) null else key }
                }
            }
        }

        if (!paused && state.wandering.isEmpty()) {
            Hint("드라이브를 떠나면 친구들이 놀러와요", Modifier.align(Alignment.BottomCenter))
        }

        // 상단: 제목 + 사색 토글
        Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.me?.nickname ?: "나"}의 공원", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            PauseToggle(paused, onTogglePause)
        }
    }
}

@Composable
private fun PauseToggle(paused: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val bg = if (paused) Pleos.Text else Pleos.Background
    val fg = if (paused) Pleos.OnText else Pleos.Text
    Row(
        Modifier.pressScale(interaction)
            .background(bg, RoundedCornerShape(50))
            .border(1.dp, Pleos.Divider, RoundedCornerShape(50))
            .clickable(interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(if (paused) Pleos.Accent else Pleos.TextSecondary, RoundedCornerShape(50)))
        Spacer(Modifier.width(10.dp))
        Text(if (paused) "사색 중 · 다시 만나기" else "잠깐 사색에 잠기기", color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier) {
    Surface(modifier.padding(32.dp), shape = RoundedCornerShape(50), color = Pleos.Background, shadowElevation = 1.dp) {
        Text(text, Modifier.padding(horizontal = 28.dp, vertical = 14.dp), style = MaterialTheme.typography.titleMedium)
    }
}

/** 선으로 그린 심플한 공원: 산책로 고리, 나무, 벤치, 가로등. */
@Composable
private fun ParkScenery(theme: ParkTheme) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = Stroke(width = 3.dp.toPx())
        // 산책로(둥근 사각형 고리)
        val inset = Offset(size.width * 0.18f, size.height * 0.24f)
        val pathSize = Size(size.width * 0.64f, size.height * 0.56f)
        drawRoundRect(theme.path, inset, pathSize, CornerRadius(pathSize.height / 2), style = Stroke(28.dp.toPx()))
        theme.props.forEach { p ->
            val c = Offset(size.width * p.x, size.height * p.y)
            when (p.kind) {
                PropKind.TREE -> tree(c, theme, stroke)
                PropKind.BENCH -> bench(c, theme, stroke)
                PropKind.LAMP -> lamp(c, theme, stroke)
            }
        }
    }
}

private fun DrawScope.tree(c: Offset, theme: ParkTheme, stroke: Stroke) {
    val r = 34.dp.toPx()
    drawLine(theme.line, c + Offset(0f, r * 0.6f), c + Offset(0f, r * 1.6f), stroke.width)
    drawCircle(theme.fill, r, c)
    drawCircle(theme.line, r, c, style = stroke)
    drawCircle(theme.line, r * 0.45f, c + Offset(-r * 0.2f, -r * 0.15f), style = Stroke(stroke.width * 0.6f))
}

private fun DrawScope.bench(c: Offset, theme: ParkTheme, stroke: Stroke) {
    val w = 72.dp.toPx()
    val h = 10.dp.toPx()
    drawRoundRect(theme.fill, c - Offset(w / 2, h), Size(w, h * 2), CornerRadius(h))
    drawRoundRect(theme.line, c - Offset(w / 2, h), Size(w, h * 2), CornerRadius(h), style = stroke)
    drawLine(theme.line, c + Offset(-w * 0.35f, h), c + Offset(-w * 0.35f, h * 2.6f), stroke.width)
    drawLine(theme.line, c + Offset(w * 0.35f, h), c + Offset(w * 0.35f, h * 2.6f), stroke.width)
}

private fun DrawScope.lamp(c: Offset, theme: ParkTheme, stroke: Stroke) {
    val h = 64.dp.toPx()
    drawLine(theme.line, c, c + Offset(0f, h), stroke.width)
    drawCircle(theme.accent.copy(alpha = 0.18f), 18.dp.toPx(), c)
    drawCircle(theme.accent, 7.dp.toPx(), c)
}

private val AVATAR = 112.dp

@Composable
private fun Wanderer(
    key: String,
    character: Character,
    areaW: Dp,
    areaH: Dp,
    showBubble: Boolean,
    isMe: Boolean = false,
    onTap: () -> Unit,
) {
    val maxX = (areaW - AVATAR).value.coerceAtLeast(0f)
    val maxY = (areaH - AVATAR).value.coerceAtLeast(0f)
    val rnd = remember(key) { Random(key.hashCode()) }
    val x = remember(key) { Animatable(rnd.nextFloat() * maxX) }
    val y = remember(key) { Animatable(rnd.nextFloat() * maxY) }
    var walking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }

    // 걷는 중 0.3초, 멈췄을 때 0.6초 간격으로 프레임을 바꾼다.
    LaunchedEffect(key) {
        while (true) {
            delay(if (walking) 300 else 600)
            frame++
        }
    }

    // 천천히 무작위로 돌아다닌다. 말풍선이 떠 있으면 멈춘다.
    LaunchedEffect(key, showBubble, maxX, maxY) {
        if (showBubble) { walking = false; return@LaunchedEffect }
        while (true) {
            val tx = Random.nextFloat() * maxX
            val ty = Random.nextFloat() * maxY
            facingLeft = tx < x.value
            val duration = (hypot(tx - x.value, ty - y.value) * 18).toInt().coerceIn(1500, 7000)
            walking = true
            coroutineScope {
                launch { x.animateTo(tx, tween(duration, easing = LinearEasing)) }
                launch { y.animateTo(ty, tween(duration, easing = LinearEasing)) }
            }
            walking = false
            delay(800L + Random.nextLong(3000))
        }
    }

    Box(Modifier.offset { IntOffset(x.value.dp.roundToPx(), y.value.dp.roundToPx()) }) {
        Box(Modifier.size(AVATAR).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onTap)) {
            val avatar = @Composable {
                CharacterAvatar(
                    character.parts, Modifier.fillMaxSize(),
                    pose = if (walking) Pose.WALK else Pose.IDLE, frame = frame,
                    // 스프라이트는 오른쪽에 꼬리가 있다. 왼쪽으로 갈 때 뒤집는다.
                    mirrored = !facingLeft,
                )
            }
            // 내 캐릭터만 뒤에 조명이 숨쉰다(친구들과 구분).
            if (isMe) CharacterGlow(Modifier.fillMaxSize()) { avatar() } else avatar()
            if (isMe) {
                Text(
                    "나", Modifier.align(Alignment.BottomCenter).offset(y = 8.dp)
                        .background(Pleos.Text, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp),
                    color = Pleos.OnText, style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        AnimatedVisibility(showBubble, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.offset(y = (-44).dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = Pleos.Background, shadowElevation = 2.dp) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(character.nickname, style = MaterialTheme.typography.labelLarge)
                    Text(character.greeting, style = MaterialTheme.typography.bodyMedium, color = Pleos.TextSecondary)
                }
            }
        }
    }
}
