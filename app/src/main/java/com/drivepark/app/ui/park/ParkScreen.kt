package com.drivepark.app.ui.park

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.Region
import com.drivepark.app.domain.KeeperMode
import com.drivepark.app.domain.ParkKeeper
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.character.CharacterGlow
import com.drivepark.app.ui.character.rememberIdleFrame
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.pixel.Poser
import com.drivepark.app.ui.theme.LocalAnimationsEnabled
import com.drivepark.app.ui.theme.Pleos
import com.drivepark.app.ui.theme.pressScale
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ParkScreen(
    vm: ParkViewModel,
    paused: Boolean,
    onTogglePause: () -> Unit,
    keeperTutorialDone: Boolean = true,
    onKeeperTutorialDone: () -> Unit = {},
    theme: ParkTheme = if (Pleos.IsDark) PleosNight else PleosLight,
) {
    val state by vm.state.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) } // 말풍선을 띄운 캐릭터 key
    // 공원지기: 친구 수는 새 친구 + 본 친구(공원지기 자신은 세지 않는다).
    // 안내를 끝내면 저장(설정)이 반영되기를 기다리지 않고 바로 닫는다.
    var finishedNow by remember { mutableStateOf(false) }
    val finishTutorial = { finishedNow = true; onKeeperTutorialDone() }
    // 이호랑을 눌러야 안내가 시작된다(그 전에는 공원에서 폴짝이며 누르기를 유도).
    var tutorialStarted by remember { mutableStateOf(false) }
    val keeperMode = ParkKeeper.mode(state.newFriends.size + state.seenFriends.size, keeperTutorialDone || finishedNow, tutorialStarted)
    var tutorialStep by remember { mutableIntStateOf(0) }
    var helpIndex by remember { mutableStateOf<Int?>(null) }

    // 주차 중 아무도 화면을 만지지 않으면 공원도 쉰다(제자리에 앉고 조명이 멈춘다).
    // 터치, 친구 도착, 주행 후 주차, 사색 토글이 있으면 다시 깨어난다.
    val parentAnimate = LocalAnimationsEnabled.current
    var touches by remember { mutableIntStateOf(0) }
    var resting by remember { mutableStateOf(false) }
    LaunchedEffect(touches, parentAnimate, state.wandering.size, paused) {
        resting = false
        if (!parentAnimate) return@LaunchedEffect
        delay(REST_AFTER_MS)
        resting = true
    }

    CompositionLocalProvider(
        LocalAnimationsEnabled provides (parentAnimate && !resting),
        LocalParkResting provides (parentAnimate && resting),
    ) {
      val guiding = keeperMode == KeeperMode.GUIDE
      val parkBlur by animateDpAsState(if (guiding) 16.dp else 0.dp, tween(KEEPER_FADE_MS), label = "keeperBlur")
      Box(Modifier.fillMaxSize()) {
        // 처음 안내 중에는 공원 전체를 흐리게 하고, 그 위에 이호랑을 크게 띄운다.
        Box(
            Modifier.fillMaxSize().blur(parkBlur).background(theme.ground).pointerInput(Unit) {
                // 터치를 가로채지 않고 지켜보기만 한다(캐릭터 탭, 토글은 그대로 동작).
                awaitPointerEventScope {
                    while (true) {
                        if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) touches++
                    }
                }
            },
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(top = 72.dp)) {
                ParkScenery(theme)
                val areaW = maxWidth
                val areaH = maxHeight
                // 사색 중: 친구들은 공원에 없고, 내 캐릭터만 첫 벤치에 앉아 있다.
                if (paused) {
                    state.me?.let { me ->
                        val bench = theme.props.first { it.kind == PropKind.BENCH }
                        Box(Modifier.offset(areaW * bench.x - AVATAR / 2, areaH * bench.y - AVATAR + 12.dp)) {
                            CharacterGlow(Modifier.size(AVATAR)) { CharacterAvatar(me, Modifier.size(AVATAR), pose = Pose.SIT) }
                        }
                    }
                } else {
                    state.me?.let { me ->
                        Wanderer("me", WandererLook.of(me), areaW, areaH, selected == "me", isMe = true) { selected = if (selected == "me") null else "me" }
                    }
                    state.wandering.forEach { friend ->
                        val key = "f${friend.id}"
                        Wanderer(key, WandererLook.of(friend.character).copy(sticker = friend.sticker?.emoji()), areaW, areaH, selected == key, enterFromEdge = friend.isNew) { selected = if (selected == key) null else key }
                    }
                    // 모은 지역 캐릭터(새 그림이 등록된 것만) 최대 2마리. 공원 화면을 열 때마다 새로 고른다.
                    val regionIds = state.collectedRegions.map { it.id }
                    val visitors = remember(regionIds) { pickVisitors(regionIds, Random.Default) }
                    state.collectedRegions.filter { it.id in visitors }.forEach { region ->
                        val key = visitorKey(region.id)
                        Wanderer(key, WandererLook.of(region), areaW, areaH, selected == key) { selected = if (selected == key) null else key }
                    }
                    // 공원지기 이호랑(안내 전): 공원 가운데쯤 서서 폴짝폴짝 + 뒤 조명 + "나 좀 눌러 봐!". 누르면 처음 안내 시작.
                    if (keeperMode == KeeperMode.INVITE) {
                        KeeperInvite(Modifier.offset(areaW * 0.55f - KEEPER_INVITE_SIZE / 2, areaH * 0.30f)) {
                            tutorialStep = 0
                            tutorialStarted = true
                        }
                    }
                    // 공원지기 이호랑(안내를 마친 뒤): 왼쪽 아래 구석에서 놀다가 누르면 도움말.
                    // 말풍선이 다른 캐릭터에 가리지 않게 맨 마지막(맨 위)에 그린다. 처음 안내는 아래 [KeeperTutorial].
                    if (keeperMode == KeeperMode.PLAY) {
                        Box(Modifier.offset(y = areaH * 0.55f).size(areaW * 0.32f, areaH * 0.45f)) {
                            val name = ParkKeeper.CHARACTER.nickname
                            val look = WandererLook.of(ParkKeeper.CHARACTER).copy(
                                title = name,
                                line = helpIndex?.let { ParkKeeper.HELP[it] } ?: "",
                            )
                            Wanderer(KEEPER_KEY, look, areaW * 0.32f, areaH * 0.45f, helpIndex != null, badge = name) {
                                helpIndex = ParkKeeper.nextHelp(helpIndex)
                            }
                        }
                    }
                }
            }

            if (!paused && state.wandering.isEmpty() && keeperMode == KeeperMode.PLAY) {
                Hint("드라이브를 떠나면 친구들이 놀러와요", Modifier.align(Alignment.BottomCenter))
            }

            // 상단: 제목 + 사색 토글
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.me?.nickname ?: "나"}의 공원", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.weight(1f))
                PauseToggle(paused, onTogglePause)
            }
        }
        // 안내가 끝나면 휙 사라지지 않고, 흐림이 걷히는 동안 이호랑과 대화 상자도 함께 서서히 사라진다.
        AnimatedVisibility(guiding, enter = fadeIn(tween(KEEPER_FADE_MS)), exit = fadeOut(tween(KEEPER_FADE_MS))) {
            KeeperTutorial(
                step = tutorialStep,
                onNext = {
                    val next = ParkKeeper.nextTutorialStep(tutorialStep)
                    if (next == null) finishTutorial() else tutorialStep = next
                },
                onSkip = finishTutorial,
            )
        }
      }
    }
}

/**
 * 걷기 그림 한 장이 머무는 시간(ms). 주행 팝업의 제자리걸음도 같은 값을 쓴다.
 * 4장에 두 걸음. 걷는 속도(초당 약 11 도트칸)에 맞춰 발이 미끄러져 보이지 않게 했다.
 * 예전 300ms는 걸음이 너무 느려 발이 미끄러지는 것처럼 보였다(백호 원본은 125ms).
 * 110 / 140 / 180ms를 비교해 110ms(종종걸음)로 정했다(2026-11-06 사용자 선택).
 */
const val WALK_FRAME_MS = 110

/** 이 시간 동안 아무도 공원을 만지지 않으면 쉰다. */
private const val REST_AFTER_MS = 3 * 60_000L

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

/**
 * 도트 잔디 산책길: 잔디 바닥 + 풀 점, 가운데를 가로지르며 한 번 꺾이는 길, 네 구석의 나무·벤치·가로등.
 * 모든 그림은 도트 한 칸([PARK_DOT]) 단위의 사각형이라 캐릭터 도트와 같은 느낌이 난다.
 */
@Composable
private fun ParkScenery(theme: ParkTheme) {
    Canvas(Modifier.fillMaxSize()) {
        val u = PARK_DOT.toPx()
        grass(theme, u)
        walkway(theme, u)
        theme.props.forEach { p ->
            val c = Offset(snap(size.width * p.x, u), snap(size.height * p.y, u))
            when (p.kind) {
                PropKind.TREE -> tree(c, theme, u)
                PropKind.BENCH -> bench(c, theme, u)
                PropKind.LAMP -> lamp(c, theme, u)
            }
        }
    }
}

/** 공원 그림의 도트 한 칸. */
private val PARK_DOT = 4.dp

private fun snap(v: Float, u: Float) = (v / u).toInt() * u

/** 도트 칸 단위 사각형: c 기준 (x, y)칸에서 w×h칸. */
private fun DrawScope.dots(color: Color, c: Offset, x: Float, y: Float, w: Float, h: Float, u: Float) =
    drawRect(color, Offset(c.x + x * u, c.y + y * u), Size(w * u, h * u))

/** 풀 점: 화면 크기만 정해지면 늘 같은 자리(고정 씨앗)라 다시 그려도 흔들리지 않는다. */
private fun DrawScope.grass(theme: ParkTheme, u: Float) {
    val rnd = java.util.Random(7)
    val count = (size.width * size.height / (u * u * 220)).toInt()
    repeat(count) {
        val x = snap(rnd.nextFloat() * size.width, u)
        val y = snap(rnd.nextFloat() * size.height, u)
        drawRect(theme.grass, Offset(x, y), Size(u * 2, u))
    }
}

/** 산책길: 왼쪽은 아래(59~70%), 오른쪽은 위(46~57%), 29~46% 구간에서 계단처럼 비스듬히 올라간다. */
private fun DrawScope.walkway(theme: ParkTheme, u: Float) {
    val band = snap(size.height * 0.11f, u)
    val lowTop = snap(size.height * 0.59f, u)
    val highTop = snap(size.height * 0.46f, u)
    val x0 = snap(size.width * 0.29f, u)
    val x1 = snap(size.width * 0.46f, u)
    drawRect(theme.path, Offset(0f, lowTop), Size(x0, band))
    drawRect(theme.path, Offset(x1, highTop), Size(size.width - x1, band))
    var x = x0
    while (x < x1) {
        val t = (x - x0) / (x1 - x0)
        val top = snap(lowTop + (highTop - lowTop) * t, u)
        drawRect(theme.path, Offset(x, top), Size(u, band))
        x += u
    }
}

/** 나무: 모서리를 깎은 잎(14×12칸), 왼쪽 위 밝은 잎, 기둥. c = 잎 가운데. */
private fun DrawScope.tree(c: Offset, theme: ParkTheme, u: Float) {
    dots(theme.wood, c, -1f, 5f, 3f, 5f, u)
    dots(theme.leaf, c, -7f, -5f, 14f, 10f, u)
    dots(theme.leaf, c, -6f, -6f, 12f, 12f, u)
    dots(theme.leafLight, c, -4f, -4f, 4f, 1f, u)
}

/** 벤치: 가로 널빤지 세 줄 + 다리 둘. c = 앉는 자리 가운데. */
private fun DrawScope.bench(c: Offset, theme: ParkTheme, u: Float) {
    listOf(-3f, -1f, 1f).forEach { y -> dots(theme.wood, c, -7f, y, 14f, 1f, u) }
    dots(theme.wood, c, -6f, 2f, 1f, 2f, u)
    dots(theme.wood, c, 5f, 2f, 1f, 2f, u)
}

/** 가로등: 기둥 + 네모 불빛. 밤에는 둘레에 은은한 주황 빛. c = 불빛. */
private fun DrawScope.lamp(c: Offset, theme: ParkTheme, u: Float) {
    if (theme.glow.alpha > 0f) drawCircle(theme.glow, u * 6, c + Offset(u / 2, u))
    dots(theme.wood, c, 0f, 2f, 1f, 9f, u)
    dots(theme.lamp, c, -0.5f, 0f, 2f, 2f, u)
}

private val AVATAR = 112.dp

/** 공원지기 Wanderer key(친구 "f숫자"·나 "me"·지역 캐릭터와 겹치지 않게). */
private const val KEEPER_KEY = "keeper"

/** 안내가 끝날 때 흐림이 걷히고 이호랑이 사라지는 시간(ms). */
private const val KEEPER_FADE_MS = 700

/** 캐릭터 발밑의 작은 이름표(나·공원지기). */
@Composable
private fun NameBadge(text: String, modifier: Modifier) {
    Text(
        text, modifier.background(Pleos.Text, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp),
        color = Pleos.OnText, style = MaterialTheme.typography.labelMedium,
    )
}

/**
 * 공원지기 이호랑의 처음 안내(전체 화면): 뒤 공원은 흐리게(호출하는 쪽에서 blur) + 어둡게 덮고,
 * 가운데에 이호랑을 크게, 아래에 대화 상자를 둔다. 화면 아무 곳이나 누르면 다음 줄로 넘어간다(저절로 넘어가지 않는다).
 * 마지막 줄에서 누르면 끝. 오른쪽 위 [건너뛰기]로 바로 끝낼 수도 있다. 뒤 공원은 누를 수 없다.
 */
@Composable
private fun KeeperTutorial(step: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val frame = rememberIdleFrame()
    val last = step == ParkKeeper.TUTORIAL.lastIndex
    val compact = com.drivepark.app.ui.theme.LocalCompact.current
    Box(
        Modifier.fillMaxSize().background(Pleos.Scrim)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onNext),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CharacterGlow(Modifier.size(if (compact) 220.dp else 320.dp)) {
                CharacterAvatar(ParkKeeper.CHARACTER, Modifier.size(if (compact) 160.dp else 240.dp), frame = frame)
            }
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = Pleos.Background, shadowElevation = 4.dp) {
                Column(Modifier.widthIn(min = 320.dp, max = 760.dp).padding(horizontal = 32.dp, vertical = 24.dp)) {
                    Text(ParkKeeper.CHARACTER.nickname, style = MaterialTheme.typography.titleMedium, color = Pleos.Accent)
                    Spacer(Modifier.height(8.dp))
                    Text(ParkKeeper.TUTORIAL[step], style = MaterialTheme.typography.headlineSmall, color = Pleos.Text)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (last) "눌러서 시작하기" else "화면을 눌러 다음 ▶  ${step + 1}/${ParkKeeper.TUTORIAL.size}",
                        style = MaterialTheme.typography.titleSmall, color = Pleos.TextSecondary,
                    )
                }
            }
        }
        if (!last) {
            // 어두운 덮개 위에서도 버튼으로 보이게 흰 테두리(알약 모양). 높이 64dp(차량 터치 규칙).
            androidx.compose.material3.OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).height(64.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.8f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
            ) { Text("건너뛰기", color = Color.White, style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** 공원을 걸어 다니는 캐릭터의 모습과 말풍선. 친구·나·지역 캐릭터가 같은 Wanderer를 쓴다. */
private data class WandererLook(
    val parts: CharacterParts,
    val shape: String,
    val title: String,
    val line: String,
    val look: PersonLook = PersonLook(),
    /** 반려동물이면 말풍선에 "🐾 반려동물"을 붙인다. */
    val pet: Boolean = false,
    /** 내가 남긴 답례 스티커(그림 글자). 머리 위에 작게 보인다. */
    val sticker: String? = null,
    val kind: CharacterKind = CharacterKind.PERSON,
    /** 직접 그린 그림(모양이 drawn일 때만 쓴다). 잘못된 그림은 렌더러가 누구에 따라 대신 그린다. */
    val drawing: String? = null,
) {
    companion object {
        fun of(c: Character) = WandererLook(
            c.parts, c.shape, c.nickname, c.greeting, c.look, pet = c.kind == CharacterKind.PET, kind = c.kind, drawing = c.drawing,
        )

        fun of(r: Region) = WandererLook(
            CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE), r.id, r.mascotName, "${r.name}에서 놀러 왔어요",
        )
    }
}

@Composable
private fun Wanderer(
    key: String,
    character: WandererLook,
    areaW: Dp,
    areaH: Dp,
    showBubble: Boolean,
    isMe: Boolean = false,
    /** 발밑 이름표. 내 캐릭터는 "나", 공원지기는 "공원지기". */
    badge: String? = if (isMe) "나" else null,
    /** 새로 놀러온 친구: 공원 왼쪽 밖에서 시작해 걸어 들어온다. */
    enterFromEdge: Boolean = false,
    onTap: () -> Unit,
) {
    val maxX = (areaW - AVATAR).value.coerceAtLeast(0f)
    val maxY = (areaH - AVATAR).value.coerceAtLeast(0f)
    val rnd = remember(key) { Random(key.hashCode()) }
    val x = remember(key) { Animatable(startX(enterFromEdge, rnd, maxX)) }
    val y = remember(key) { Animatable(rnd.nextFloat() * maxY) }
    var walking by remember { mutableStateOf(false) }
    var facingLeft by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }
    // 주행 중에는 공원이 가려져 있으므로 걷기·숨쉬기를 멈춘다(자리는 그대로 두고, 주차하면 이어서 움직인다).
    val animate = LocalAnimationsEnabled.current
    // 3분 동안 손대지 않아 쉬는 중이면 앉아서 존다(주행 중 멈춤은 그냥 앉아 있음).
    val dozing = !animate && LocalParkResting.current
    var eyesClosed by remember { mutableStateOf(false) }
    var hopTaps by remember { mutableIntStateOf(0) }
    var hop by remember { mutableStateOf<HopFrame?>(null) }
    val hopping = hop != null

    // 걷는 중 [WALK_FRAME_MS], 멈췄을 때 0.6초 간격으로 프레임을 바꾼다.
    LaunchedEffect(key, animate) {
        if (!animate) return@LaunchedEffect
        while (true) {
            delay(if (walking) WALK_FRAME_MS.toLong() else 600L)
            frame++
        }
    }

    // 눈 깜빡임: 3~4초마다 0.12초. 주행 중·쉬는 중에는 멈춘다.
    LaunchedEffect(key, animate) {
        eyesClosed = false
        if (!animate) return@LaunchedEffect
        while (true) {
            delay(nextBlinkDelay(rnd))
            eyesClosed = true
            delay(BLINK_MS)
            eyesClosed = false
        }
    }

    // 졸기: 쉬는 중에만 1.5초마다 꾸벅.
    LaunchedEffect(key, dozing) {
        if (!dozing) return@LaunchedEffect
        while (true) {
            delay(DOZE_FRAME_MS)
            frame++
        }
    }

    // 폴짝: 누를 때마다 처음부터 한 번. 화면 높이는 hop.liftDots만큼 띄운다.
    LaunchedEffect(key, hopTaps) {
        if (hopTaps == 0) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            val f = withFrameMillis { hopAt(it - start) }
            hop = f
            if (f == null) break
        }
    }

    // 천천히 무작위로 돌아다닌다. 말풍선이 떠 있거나 폴짝 중이면 멈춘다.
    LaunchedEffect(key, showBubble, maxX, maxY, animate, hopping) {
        if (showBubble || !animate || hopping) { walking = false; return@LaunchedEffect }
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

    Box(
        Modifier.offset {
            // 폴짝 높이는 도트 칸 단위(CharacterAvatar가 그림을 키운 정수배와 같은 크기).
            val dot = floor(AVATAR.toPx() / Poser.REF_CANVAS_H)
            val lift = ((hop?.liftDots ?: 0f) * dot).roundToInt()
            IntOffset(x.value.dp.roundToPx(), y.value.dp.roundToPx() - lift)
        },
    ) {
        Box(
            Modifier.size(AVATAR).clickable(remember { MutableInteractionSource() }, indication = null) {
                hopTaps++
                onTap()
            },
        ) {
            val avatar = @Composable {
                CharacterAvatar(
                    character.parts, Modifier.fillMaxSize(), shape = character.shape, look = character.look,
                    kind = character.kind, drawing = character.drawing,
                    // 폴짝 > 졸기(쉬는 중) > 앉기(주행 중) > 걷기 > 서기.
                    pose = when {
                        hop != null -> hop!!.pose
                        dozing -> Pose.DOZE
                        !animate -> Pose.SIT
                        walking -> Pose.WALK
                        else -> Pose.IDLE
                    },
                    frame = frame,
                    // 스프라이트는 오른쪽에 꼬리가 있다. 왼쪽으로 갈 때 뒤집는다.
                    mirrored = !facingLeft,
                    eyesClosed = eyesClosed,
                )
            }
            // 내 캐릭터만 뒤에 조명이 숨쉰다(친구들과 구분).
            if (isMe) CharacterGlow(Modifier.fillMaxSize()) { avatar() } else avatar()
            character.sticker?.let { s ->
                Text(s, Modifier.align(Alignment.TopEnd), style = MaterialTheme.typography.titleLarge)
            }
            badge?.let { NameBadge(it, Modifier.align(Alignment.BottomCenter).offset(y = 8.dp)) }
        }
        AnimatedVisibility(showBubble, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.offset(y = (-44).dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = Pleos.Background, shadowElevation = 2.dp) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(character.title, style = MaterialTheme.typography.labelLarge)
                    if (character.pet) Text(PET_BADGE, style = MaterialTheme.typography.labelMedium, color = Pleos.TextSecondary)
                    Text(character.line, style = MaterialTheme.typography.bodyMedium, color = Pleos.TextSecondary)
                }
            }
        }
    }
}

/** 유도 중인 공원지기 크기(다른 캐릭터보다 조금 커서 눈에 띄고 누르기 쉽다). */
private val KEEPER_INVITE_SIZE = 144.dp

/**
 * 처음 안내 전의 공원지기 이호랑: 제자리에서 폴짝폴짝 뛰고, 뒤 조명이 숨쉬고, 머리 위 말풍선 "나 좀 눌러 봐!".
 * 누르면 [onTap](처음 안내 시작). 주행 중·쉬는 중([LocalAnimationsEnabled] false)에는 폴짝·조명이 멈춘다.
 * 그림은 고정된 이호랑 그대로 쓴다.
 */
@Composable
private fun KeeperInvite(modifier: Modifier, onTap: () -> Unit) {
    val animate = LocalAnimationsEnabled.current
    val frame = rememberIdleFrame()
    var hop by remember { mutableStateOf<HopFrame?>(null) }
    LaunchedEffect(animate) {
        hop = null
        if (!animate) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            val elapsed = withFrameMillis { it } - start
            val f = inviteHopAt(elapsed)
            hop = f
            if (f == null) {
                // 쉬는 동안은 매 프레임 깨지 않고 다음 폴짝까지 잔다.
                val t = elapsed % INVITE_CYCLE_MS
                delay(if (t < INVITE_SECOND_HOP_MS) INVITE_SECOND_HOP_MS - t else INVITE_CYCLE_MS - t)
            }
        }
    }
    Column(modifier.width(KEEPER_INVITE_SIZE * 2).offset(x = -KEEPER_INVITE_SIZE / 2), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(16.dp), color = Pleos.Background, shadowElevation = 2.dp) {
            Text(
                ParkKeeper.INVITE_LINE, Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium, color = Pleos.Text,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(KEEPER_INVITE_SIZE)
                .offset {
                    val dot = floor(KEEPER_INVITE_SIZE.toPx() / Poser.REF_CANVAS_H)
                    IntOffset(0, -((hop?.liftDots ?: 0f) * dot).roundToInt())
                }
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onTap),
        ) {
            // 조명은 캐릭터보다 크게 퍼져야 잔디 위에서도 보인다(처음 안내의 큰 이호랑과 같은 비율).
            CharacterGlow(Modifier.requiredSize(KEEPER_INVITE_SIZE * 1.6f).align(Alignment.Center)) {
                CharacterAvatar(ParkKeeper.CHARACTER, Modifier.size(KEEPER_INVITE_SIZE), pose = hop?.pose ?: Pose.IDLE, frame = frame)
            }
            NameBadge(ParkKeeper.CHARACTER.nickname, Modifier.align(Alignment.BottomCenter).offset(y = 8.dp))
        }
    }
}
