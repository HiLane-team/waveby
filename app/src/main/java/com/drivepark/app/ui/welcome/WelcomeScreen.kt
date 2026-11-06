package com.drivepark.app.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.startGreeting
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.character.CharacterGlow
import com.drivepark.app.ui.theme.Pleos
import kotlinx.coroutines.delay
import java.util.Calendar

/** 시동 인사가 떠 있는 시간. 지나거나 화면을 누르면 공원으로 넘어간다. */
const val WELCOME_MS = 3500L

/**
 * 시동 인사 화면: 테마 배경 가운데에 조명이 숨쉬는 내 캐릭터, 위에 시간대 인사말, 아래에 "○○의 공원".
 * 앱이 새로 켜질 때(차가 켜지며 앱이 뜰 때) 한 번 보여준다.
 */
@Composable
fun WelcomeScreen(me: Character, onDone: () -> Unit) {
    val text = remember(me.nickname, me.kind) { startGreeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), me.nickname, me.kind) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(600); frame++ } }
    LaunchedEffect(Unit) { delay(WELCOME_MS); onDone() }

    Column(
        Modifier.fillMaxSize().background(Pleos.Background)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text, color = Pleos.Text, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(32.dp))
        CharacterGlow(Modifier.size(360.dp)) {
            CharacterAvatar(me.parts, Modifier.size(260.dp), frame = frame)
        }
        Spacer(Modifier.height(16.dp))
        Text("${me.nickname}의 공원", color = Pleos.TextSecondary, style = MaterialTheme.typography.titleMedium)
    }
}
