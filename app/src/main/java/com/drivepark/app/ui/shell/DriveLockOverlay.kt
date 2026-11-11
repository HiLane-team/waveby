package com.drivepark.app.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import com.drivepark.app.domain.Character
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.pixel.Pose
import androidx.compose.foundation.layout.size
import com.drivepark.app.ui.rest.REST_DRIVING_BODY
import com.drivepark.app.ui.rest.REST_DRIVING_TITLE
import com.drivepark.app.ui.rest.SleepyWithCoffee
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.drivepark.app.ui.character.CharacterGlow
import com.drivepark.app.ui.park.WALK_FRAME_MS
import kotlinx.coroutines.delay
import com.drivepark.app.ui.theme.LocalCompact
import com.drivepark.app.ui.theme.Pleos

/** 주행 중 잠금 화면 문구. */
const val DRIVE_LOCK_TITLE = "주행 중이라 공원을 볼 수 없어요"
const val DRIVE_LOCK_BODY = "주차하면 길에서 만난 친구들이 놀러 와요"

/**
 * 주행 중 앱 전체를 덮어 조작을 막는다. 모든 터치를 소비한다.
 * 오래 달려 쉴 때가 되면([rest]) 커피를 든 조는 모습(정지 그림)과 짧은 문구를 보여 준다(3분할 칸에 둔 경우 등). 조작은 없다.
 */
@Composable
fun DriveLockOverlay(visible: Boolean, onDemoPark: (() -> Unit)? = null, rest: Boolean = false, me: Character? = null) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Pleos.Scrim).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (rest && me != null) {
                val compact = LocalCompact.current
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    SleepyWithCoffee(me, if (compact) 180.dp else 240.dp)
                    Spacer(Modifier.height(24.dp))
                    Text(REST_DRIVING_TITLE, color = Color.White, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(REST_DRIVING_BODY, color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                }
            } else if (me != null) {
                // 시동 인사처럼: 테마 배경 가운데에서 내 캐릭터가 제자리걸음하고, 아래에 안내 문구. 조작은 없다.
                // 주행 중에는 앱 애니메이션을 멈추지만(LocalAnimationsEnabled false), 이 걸음은 주행 팝업의 제자리걸음과 같은
                // 예외로 둔다: 4장짜리 그림을 [WALK_FRAME_MS]마다 바꾸는 것뿐이고, 잠금 화면이 보일 때만 돈다.
                val compact = LocalCompact.current
                var frame by remember { mutableIntStateOf(0) }
                LaunchedEffect(Unit) { while (true) { delay(WALK_FRAME_MS.toLong()); frame = (frame + 1) % 4 } }
                Column(
                    Modifier.fillMaxSize().background(Pleos.Background).padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CharacterGlow(Modifier.size(if (compact) 240.dp else 360.dp)) {
                        CharacterAvatar(me, Modifier.size(if (compact) 180.dp else 260.dp), pose = Pose.WALK, frame = frame)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(DRIVE_LOCK_TITLE, color = Pleos.Text, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(8.dp))
                    Text(DRIVE_LOCK_BODY, color = Pleos.TextSecondary, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                }
            } else {
                Text(DRIVE_LOCK_TITLE, color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            // 시연 모드(기어 덮어쓰기)에서만 보이는 탈출 버튼. 실제 기어 D에서는 나오지 않는다.
            onDemoPark?.let {
                OutlinedButton(onClick = it, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
                    Text("시연: 주차", color = if (rest || me == null) Color.White else Pleos.Text)
                }
            }
        }
    }
}
