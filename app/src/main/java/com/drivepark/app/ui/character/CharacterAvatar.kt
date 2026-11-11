package com.drivepark.app.ui.character

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.drivepark.app.ui.theme.Pleos
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.ui.park.BLINK_MS
import com.drivepark.app.ui.park.nextBlinkDelay
import com.drivepark.app.ui.pixel.CharacterRenderer
import com.drivepark.app.ui.pixel.PixelImage
import com.drivepark.app.ui.pixel.Poser
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.pixel.Shapes
import com.drivepark.app.ui.theme.LocalAnimationsEnabled
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.min

/**
 * 도트 캐릭터. 그림은 CharacterRenderer가 만들고, 여기서는 픽셀을 각지게(보간 없이) 키워 그린다.
 * 나중에 PNG 스프라이트로 바꿔도 이 시그니처는 유지한다.
 */
@Composable
fun CharacterAvatar(
    parts: CharacterParts,
    modifier: Modifier = Modifier,
    silhouette: Boolean = false,
    pose: Pose = Pose.IDLE,
    frame: Int = 0,
    mirrored: Boolean = false,
    eyesClosed: Boolean = false,
    shape: String = "wolf",
    look: PersonLook = PersonLook(),
    kind: CharacterKind = CharacterKind.PERSON,
    drawing: String? = null,
    /** true = 모든 캐릭터 같은 도트 크기(아래 맞춤, 큰 그림은 넘침). false = 칸에 맞춤(목록 썸네일). */
    sameDot: Boolean = true,
) {
    val f = frame.mod(4)
    val bitmap = remember(parts, pose, f, eyesClosed, shape, look, kind, drawing) {
        PixelCache.character(parts, pose, f, eyesClosed, shape, look, kind, drawing)
    }
    PixelCanvas(bitmap, modifier, silhouette, mirrored, sameDot)
}

/** 저장된 캐릭터를 그 모양·외모(직접 그린 그림 포함)로 그린다. 모르는 모양 id·잘못된 그림은 누구에 따라 기본 모양으로 대신한다. */
@Composable
fun CharacterAvatar(
    character: Character,
    modifier: Modifier = Modifier,
    pose: Pose = Pose.IDLE,
    frame: Int = 0,
    mirrored: Boolean = false,
    eyesClosed: Boolean = false,
    sameDot: Boolean = true,
) = CharacterAvatar(
    character.parts, modifier, pose = pose, frame = frame, mirrored = mirrored, eyesClosed = eyesClosed,
    shape = character.shape, look = character.look, kind = character.kind, drawing = character.drawing, sameDot = sameDot,
)

/**
 * 지역 캐릭터. silhouette=true면 아직 못 모은 상태(회색 실루엣, 움직이지 않음).
 * 새 지역 캐릭터 그림(17×23)이 등록돼 있으면 엔진으로 그리고 모은 것은 숨쉬기·깜빡임을 한다.
 * 없으면 예전 둥근 그림을 쓴다.
 */
@Composable
fun MascotAvatar(regionId: String, modifier: Modifier = Modifier, silhouette: Boolean = false, sameDot: Boolean = true) {
    if (!Shapes.hasMascot(regionId)) {
        // 예전 둥근 그림(32×32)은 늘 칸에 맞춘다.
        val bitmap = remember(regionId) { PixelCache.mascot(regionId) }
        PixelCanvas(bitmap, modifier, silhouette, mirrored = false, sameDot = false)
        return
    }
    var frame by remember { mutableIntStateOf(0) }
    var closed by remember { mutableStateOf(false) }
    if (mascotAnimates(silhouette, LocalAnimationsEnabled.current)) {
        val rnd = remember(regionId) { kotlin.random.Random(regionId.hashCode()) }
        LaunchedEffect(regionId) { while (true) { delay(600); frame++ } }
        LaunchedEffect(regionId) {
            while (true) { delay(nextBlinkDelay(rnd)); closed = true; delay(BLINK_MS); closed = false }
        }
    }
    CharacterAvatar(MASCOT_PARTS, modifier, silhouette, Pose.IDLE, if (silhouette) 0 else frame, eyesClosed = closed, shape = regionId, sameDot = sameDot)
}

/** 지역 캐릭터가 숨쉬기·깜빡임을 할지: 모았고(실루엣 아님), 주행 중·쉬는 중이 아닐 때만. */
internal fun mascotAnimates(silhouette: Boolean, animationsEnabled: Boolean) = !silhouette && animationsEnabled

/**
 * 제자리 숨쉬기용 프레임 번호(0.6초마다 +1). 주행 중·쉬는 중([LocalAnimationsEnabled] false)에는 멈춘다.
 * 화면마다 따로 반복문을 두지 말고 이것을 쓴다(차량 부하 규칙).
 */
@Composable
fun rememberIdleFrame(): Int {
    var frame by remember { mutableIntStateOf(0) }
    if (mascotAnimates(silhouette = false, LocalAnimationsEnabled.current)) {
        LaunchedEffect(Unit) { while (true) { delay(600); frame++ } }
    }
    return frame
}

/** 지역 캐릭터는 털 색·모자를 쓰지 않으므로 기본 파츠로 그린다. */
private val MASCOT_PARTS = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

@Composable
private fun PixelCanvas(bitmap: ImageBitmap, modifier: Modifier, silhouette: Boolean, mirrored: Boolean, sameDot: Boolean = true) {
    // 다크모드에서는 검은 테두리가 어두운 배경에 묻히므로, 캐릭터 뒤에만 은은한 빛을 깐다(그림 픽셀은 그대로).
    val backlight = if (Pleos.IsDark && !silhouette) Pleos.Glow.copy(alpha = 0.38f) else null
    Canvas(modifier.aspectRatio(1f)) {
        backlight?.let { c ->
            drawCircle(
                Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), center = center, radius = size.minDimension * 0.5f),
                radius = size.minDimension * 0.5f,
            )
        }
        val (left, top, dw, dh) = pixelPlacement(size.width, size.height, bitmap.width, bitmap.height, sameDot)
        val draw = {
            drawImage(
                bitmap,
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(dw, dh),
                filterQuality = FilterQuality.None,
                colorFilter = if (silhouette) SILHOUETTE else null,
            )
        }
        if (mirrored) {
            drawContext.transform.scale(-1f, 1f, center)
            draw()
            drawContext.transform.scale(-1f, 1f, center)
        } else {
            draw()
        }
    }
}

private val SILHOUETTE = ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFFA1A1AA))

/** 같은 조합은 한 번만 합성한다. 열쇠에 모양·외모·누구·그림이 들어가야 다른 모양끼리 그림을 나눠 쓰지 않는다. */
internal object PixelCache {
    private data class Key(
        val parts: CharacterParts, val pose: Pose, val frame: Int, val eyesClosed: Boolean,
        val shape: String, val look: PersonLook, val kind: CharacterKind, val drawing: String?,
    )

    private val characters = HashMap<Key, ImageBitmap>()
    private val mascots = HashMap<String, ImageBitmap>()

    fun character(
        parts: CharacterParts, pose: Pose, frame: Int, eyesClosed: Boolean, shape: String, look: PersonLook,
        kind: CharacterKind = CharacterKind.PERSON, drawing: String? = null,
    ): ImageBitmap =
        synchronized(this) {
            characters.getOrPut(Key(parts, pose, frame, eyesClosed, shape, look, kind, drawing)) {
                CharacterRenderer.render(parts, pose, frame, eyesClosed, shape = shape, look = look, kind = kind, drawing = drawing).toImageBitmap()
            }
        }

    fun mascot(regionId: String): ImageBitmap = synchronized(this) {
        mascots.getOrPut(regionId) { CharacterRenderer.renderMascot(regionId).toImageBitmap() }
    }

    private fun PixelImage.toImageBitmap(): ImageBitmap =
        Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** 털 색 이름(캐릭터 만들기 화면 라벨). enum 이름은 DB 호환을 위해 그대로 둔다. */
fun BodyColor.furLabel(): String = when (this) {
    BodyColor.PURPLE -> "기본 회색"
    BodyColor.RED -> "노을"
    BodyColor.ORANGE -> "여우"
    BodyColor.YELLOW -> "모래"
    BodyColor.GREEN -> "숲"
    BodyColor.BLUE -> "새벽"
    BodyColor.C_RED -> "빨강"; BodyColor.C_ORANGE -> "주황"; BodyColor.C_YELLOW -> "노랑"; BodyColor.C_LIME -> "연두"
    BodyColor.C_GREEN -> "초록"; BodyColor.C_TEAL -> "청록"; BodyColor.C_BLUE -> "파랑"; BodyColor.C_NAVY -> "남색"
    BodyColor.C_PURPLE -> "보라"; BodyColor.C_PINK -> "분홍"; BodyColor.C_BROWN -> "갈색"; BodyColor.C_BEIGE -> "베이지"
    BodyColor.C_WHITE -> "흰색"; BodyColor.C_GRAY -> "회색"; BodyColor.C_CHARCOAL -> "진회색"; BodyColor.C_BLACK -> "검정"
}

/** 화면에 그림을 놓을 자리(px). 왼쪽·위는 상자 밖(음수)일 수 있다. */
internal data class PixelPlacement(val left: Int, val top: Int, val width: Int, val height: Int)

/**
 * 그림을 상자에 놓는 계산. 정수배로 키워 픽셀 크기를 고르게 한다(너무 작으면 실수배).
 * - sameDot=true: 도트 크기 = 상자 높이 ÷ [Poser.REF_CANVAS_H](17×23 그림판 높이). 그림 크기와 상관없이 도트가 같고,
 *   아래 가운데 맞춤(발이 같은 선). 큰 그림은 상자 밖으로 넘친다. 17×23은 예전과 같은 자리·크기.
 * - sameDot=false: 상자 안에 꽉 맞추고 가운데(목록 썸네일처럼 칸을 넘으면 안 되는 곳).
 */
internal fun pixelPlacement(boxW: Float, boxH: Float, bmpW: Int, bmpH: Int, sameDot: Boolean): PixelPlacement {
    val raw = if (sameDot) boxH / Poser.REF_CANVAS_H else min(boxW / bmpW, boxH / bmpH)
    val scale = if (raw >= 1f) floor(raw) else raw
    val dw = (bmpW * scale).toInt()
    val dh = (bmpH * scale).toInt()
    val left = ((boxW - dw) / 2).toInt()
    val top = if (sameDot) (boxH - dh).toInt() else ((boxH - dh) / 2).toInt()
    return PixelPlacement(left, top, dw, dh)
}
