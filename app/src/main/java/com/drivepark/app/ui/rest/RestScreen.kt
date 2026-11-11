package com.drivepark.app.ui.rest

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivepark.app.domain.Character
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.character.CharacterGlow
import com.drivepark.app.ui.character.rememberIdleFrame
import com.drivepark.app.ui.pixel.CoffeeCup
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.theme.Pleos

/** 주차 후 휴식 권유 화면의 문구(음성으로도 읽는다). */
const val REST_MESSAGE = "오래 달렸어요. 저와 같이 잠깐 쉬어 갈까요?"

/** 주행 중 잠금 화면(3분할 칸 등)에 보이는 짧은 문구. 조작은 없다. */
const val REST_DRIVING_TITLE = "오래 달렸어요"
const val REST_DRIVING_BODY = "다음 쉼터에서 잠깐 쉬어 가요"

/**
 * 커피를 들고 꾸벅꾸벅 조는 내 캐릭터. [frame]이 바뀌면 고개가 까딱인다(주행 중에는 0으로 고정해 정지 그림).
 * 커피잔은 캐릭터 엔진과 따로 그린 그림이다(이호랑 그림은 건드리지 않는다).
 */
@Composable
fun SleepyWithCoffee(me: Character, size: Dp, frame: Int = 0) {
    val cup = remember {
        val img = CoffeeCup.image
        Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Row(verticalAlignment = Alignment.Bottom) {
        CharacterAvatar(me, Modifier.size(size), pose = Pose.DOZE, frame = frame, eyesClosed = true)
        Image(
            cup, contentDescription = "커피", filterQuality = FilterQuality.None,
            modifier = Modifier.size(size * 0.28f).padding(bottom = size * 0.06f),
        )
    }
}

/**
 * 오래 달린 뒤 주차하면 뜨는 휴식 권유 화면(시동 인사와 같은 모양). 누르거나 "좋아요"로 닫는다.
 * 권유일 뿐 의학적 판단이 아니다. 설정에서 끌 수 있다.
 */
@Composable
fun RestScreen(me: Character, onDone: () -> Unit) {
    val frame = rememberIdleFrame()
    Column(
        Modifier.fillMaxSize().background(Pleos.Background)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "오래 달렸어요.\n저와 같이 잠깐 쉬어 갈까요?", color = Pleos.Text, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, fontWeight = FontWeight.SemiBold, lineHeight = 52.sp),
        )
        Spacer(Modifier.height(32.dp))
        CharacterGlow(Modifier.size(360.dp)) { SleepyWithCoffee(me, 240.dp, frame) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.height(64.dp).width(260.dp)) {
            Text("좋아요", style = MaterialTheme.typography.titleLarge)
        }
    }
}
