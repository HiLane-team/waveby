package com.drivepark.app.ui.park

import androidx.compose.runtime.compositionLocalOf
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.pixel.Shapes
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** 눈을 감고 있는 시간. */
const val BLINK_MS = 120L

/** 다음 깜빡임까지 기다릴 시간: 3~4초 사이 무작위(캐릭터마다 박자가 달라 보이게). */
fun nextBlinkDelay(random: Random): Long = random.nextLong(3000L, 4001L)

/** 폴짝 한 장면: 그릴 동작과, 화면에서 띄울 높이(도트 칸, 소수). */
data class HopFrame(val pose: Pose, val liftDots: Float)

/** 폴짝 전체 길이: 웅크리기 80 → 공중 320 → 웅크리기 80ms. */
const val HOP_MS = 480L

/** 폴짝 시작 뒤 elapsedMs에 보일 장면. 끝났으면 null. 공중에서는 반원(사인)으로 최고 3칸 뜬다. */
fun hopAt(elapsedMs: Long): HopFrame? = when {
    elapsedMs < 80 -> HopFrame(Pose.CROUCH, 0f)
    elapsedMs < 400 -> HopFrame(Pose.AIR, (3 * sin(PI * (elapsedMs - 80) / 320.0)).toFloat())
    elapsedMs < HOP_MS -> HopFrame(Pose.CROUCH, 0f)
    else -> null
}

/**
 * 공원에 놀러 올 지역 캐릭터: 모은 지역 중 새 그림(17×23)이 등록된 것에서 최대 [max]마리를 무작위로 고른다.
 * 새 그림이 없으면 아무도 오지 않는다(예전 둥근 그림은 걷지 못함).
 */
fun pickVisitors(
    collected: List<String>,
    random: Random,
    max: Int = 2,
    hasMascot: (String) -> Boolean = Shapes::hasMascot,
): List<String> = collected.filter(hasMascot).distinct().shuffled(random).take(max.coerceAtLeast(0))

/** 공원 Wanderer 열쇠. 친구("f<id>")·나("me")와 겹치지 않게 접두어를 붙인다. */
fun visitorKey(regionId: String) = "m-$regionId"

/** 쉬는 모드에서 졸기 장면을 넘기는 간격. */
const val DOZE_FRAME_MS = 1500L

/** 공원이 쉬는 중인지(3분 동안 손대지 않음). 주행 중 멈춤과 구분해 졸기를 보여 줄 때 쓴다. */
val LocalParkResting = compositionLocalOf { false }

/** 반려동물 캐릭터 표시(친구 말풍선·친구 목록). */
const val PET_BADGE = "🐾 반려동물"

/**
 * 공원에서 처음 설 x 위치(dp 값). 새로 놀러온 친구는 왼쪽 끝에서 시작해(밖에서 시작하면 메뉴 바와 겹친다),
 * 첫 번째 무작위 걷기로 공원 안으로 걸어 들어온다. 나머지는 공원 안 무작위 자리.
 */
fun startX(enterFromEdge: Boolean, random: Random, maxX: Float): Float =
    if (enterFromEdge) 0f else random.nextFloat() * maxX

/** 공원지기 유도: 첫 폴짝 시작 뒤 두 번째 폴짝까지(첫 폴짝이 끝나고 잠깐 땅을 디딘 뒤). */
const val INVITE_SECOND_HOP_MS = 600L

/** 공원지기 유도 한 바퀴(폴짝폴짝 + 쉬기). */
const val INVITE_CYCLE_MS = 2400L

/** 공원지기 유도 중 elapsedMs에 보일 폴짝 장면. 폴짝폴짝 두 번 뒤 쉬고(null), [INVITE_CYCLE_MS]마다 되풀이. */
fun inviteHopAt(elapsedMs: Long): HopFrame? {
    val t = elapsedMs % INVITE_CYCLE_MS
    return hopAt(t) ?: if (t >= INVITE_SECOND_HOP_MS) hopAt(t - INVITE_SECOND_HOP_MS) else null
}
