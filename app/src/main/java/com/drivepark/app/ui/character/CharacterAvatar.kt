package com.drivepark.app.ui.character

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.ui.pixel.CharacterRenderer
import com.drivepark.app.ui.pixel.PixelImage
import com.drivepark.app.ui.pixel.Pose
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
) {
    val bitmap = remember(parts, pose, frame % 4) { PixelCache.character(parts, pose, frame % 4) }
    PixelCanvas(bitmap, modifier, silhouette, mirrored)
}

/** 지역 캐릭터. silhouette=true면 아직 못 모은 상태(회색 실루엣). */
@Composable
fun MascotAvatar(regionId: String, modifier: Modifier = Modifier, silhouette: Boolean = false) {
    val bitmap = remember(regionId) { PixelCache.mascot(regionId) }
    PixelCanvas(bitmap, modifier, silhouette, mirrored = false)
}

@Composable
private fun PixelCanvas(bitmap: ImageBitmap, modifier: Modifier, silhouette: Boolean, mirrored: Boolean) {
    Canvas(modifier.aspectRatio(1f)) {
        // 정수배로 키워 픽셀 크기를 고르게(너무 작으면 실수배). 세로 기준으로 맞추고 가운데 정렬.
        val raw = min(size.width / bitmap.width, size.height / bitmap.height)
        val scale = if (raw >= 1f) floor(raw) else raw
        val dw = (bitmap.width * scale).toInt()
        val dh = (bitmap.height * scale).toInt()
        val left = ((size.width - dw) / 2).toInt()
        val top = ((size.height - dh) / 2).toInt()
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

/** 같은 조합은 한 번만 합성한다. */
private object PixelCache {
    private val characters = HashMap<Triple<CharacterParts, Pose, Int>, ImageBitmap>()
    private val mascots = HashMap<String, ImageBitmap>()

    fun character(parts: CharacterParts, pose: Pose, frame: Int): ImageBitmap = synchronized(this) {
        characters.getOrPut(Triple(parts, pose, frame)) { CharacterRenderer.render(parts, pose, frame).toImageBitmap() }
    }

    fun mascot(regionId: String): ImageBitmap = synchronized(this) {
        mascots.getOrPut(regionId) { CharacterRenderer.renderMascot(regionId).toImageBitmap() }
    }

    private fun PixelImage.toImageBitmap(): ImageBitmap =
        Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** 털 색 이름(캐릭터 만들기 화면 라벨). enum 이름은 DB 호환을 위해 그대로 둔다. */
fun BodyColor.furLabel(): String = when (this) {
    BodyColor.PURPLE -> "늑대"
    BodyColor.RED -> "노을"
    BodyColor.ORANGE -> "여우"
    BodyColor.YELLOW -> "모래"
    BodyColor.GREEN -> "숲"
    BodyColor.BLUE -> "새벽"
}
