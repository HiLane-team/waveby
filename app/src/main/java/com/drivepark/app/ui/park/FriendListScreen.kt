package com.drivepark.app.ui.park

import com.drivepark.app.domain.CharacterKind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivepark.app.domain.Friend
import com.drivepark.app.domain.Sticker
import com.drivepark.app.ui.theme.Pleos
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedButton
import com.drivepark.app.ui.character.CharacterAvatar

/** "새 친구"와 "다녀간 친구" 두 구역. 시각은 표시하지 않는다. 화면을 벗어날 때 모두 확인함으로 바꾼다. */
@Composable
fun FriendListScreen(vm: ParkViewModel) {
    val state by vm.state.collectAsState()
    DisposableEffect(Unit) { onDispose { vm.markAllSeen() } }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("친구 목록", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            // 지금은 가짜 친구라 스티커는 이 기기에만 남는다(보내기는 서버 단계).
            Text("스티커로 답례를 남겨 보세요 · 지금은 이 기기에만 저장돼요", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Section("새 친구 (${state.newFriends.size})") }
            if (state.newFriends.isEmpty()) item { Empty("새로 놀러 온 친구가 없어요") }
            items(state.newFriends, key = { it.id }) { f -> FriendRow(f) { vm.toggleSticker(f.id, it) } }
            item { Section("다녀간 친구 (${state.seenFriends.size})") }
            if (state.seenFriends.isEmpty()) item { Empty("아직 다녀간 친구가 없어요") }
            items(state.seenFriends, key = { it.id }) { f -> FriendRow(f) { vm.toggleSticker(f.id, it) } }
        }
    }
}

@Composable
private fun Section(title: String) =
    Text(title, Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.titleLarge)

@Composable
private fun Empty(text: String) =
    Text(text, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun FriendRow(friend: Friend, onSticker: (Sticker) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CharacterAvatar(friend.character, Modifier.size(64.dp), sameDot = false)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.character.nickname, style = MaterialTheme.typography.titleMedium)
                if (friend.character.kind == CharacterKind.PET) {
                    Text(PET_BADGE, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(friend.character.greeting, style = MaterialTheme.typography.bodyLarge)
            }
            // 답례 스티커(64dp). 다시 누르면 지운다.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sticker.entries.forEach { s ->
                    val on = friend.sticker == s
                    OutlinedButton(
                        onClick = { onSticker(s) },
                        modifier = Modifier.size(64.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(if (on) 3.dp else 1.dp, if (on) Pleos.Accent else Pleos.Divider),
                    ) { Text(s.emoji(), style = MaterialTheme.typography.headlineSmall) }
                }
            }
        }
    }
}

/** 스티커 그림과 읽어 줄 이름. */
internal fun Sticker.emoji() = when (this) { Sticker.WAVE -> "👋"; Sticker.HEART -> "❤️"; Sticker.THUMBS -> "👍" }
