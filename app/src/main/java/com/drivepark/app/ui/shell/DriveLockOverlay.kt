package com.drivepark.app.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.drivepark.app.ui.theme.Pleos

/** 주행 중 앱 전체를 덮어 조작을 막는다. 모든 터치를 소비한다. */
@Composable
fun DriveLockOverlay(visible: Boolean, onDemoPark: (() -> Unit)? = null) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Pleos.Scrim).pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Text("주행 중에는 조작할 수 없어요", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            // 시연 모드(기어 덮어쓰기)에서만 보이는 탈출 버튼. 실제 기어 D에서는 나오지 않는다.
            onDemoPark?.let {
                OutlinedButton(onClick = it, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
                    Text("시연: 주차", color = Color.White)
                }
            }
        }
    }
}
