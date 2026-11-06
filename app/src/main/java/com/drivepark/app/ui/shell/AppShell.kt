package com.drivepark.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 메뉴 바 + 내용. showRail=false면(초기 설정 등) 내용만. 주행 중에는 전체를 잠근다. */
@Composable
fun AppShell(
    showRail: Boolean,
    selected: RailItem?,
    newFriends: Int,
    driving: Boolean,
    onDemoPark: (() -> Unit)?,
    onSelect: (RailItem) -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (showRail) NavRail(selected, newFriends, onSelect)
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
        DriveLockOverlay(driving, onDemoPark)
    }
}
