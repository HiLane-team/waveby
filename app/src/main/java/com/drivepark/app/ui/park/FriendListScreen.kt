package com.drivepark.app.ui.park

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivepark.app.domain.Friend
import com.drivepark.app.ui.character.CharacterAvatar

/** "새 친구"와 "다녀간 친구" 두 구역. 시각은 표시하지 않는다. 화면을 벗어날 때 모두 확인함으로 바꾼다. */
@Composable
fun FriendListScreen(vm: ParkViewModel) {
    val state by vm.state.collectAsState()
    DisposableEffect(Unit) { onDispose { vm.markAllSeen() } }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("친구 목록", style = MaterialTheme.typography.headlineMedium)
        }
        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Section("새 친구 (${state.newFriends.size})") }
            if (state.newFriends.isEmpty()) item { Empty("새로 놀러온 친구가 없어요") }
            items(state.newFriends, key = { it.id }) { FriendRow(it) }
            item { Section("다녀간 친구 (${state.seenFriends.size})") }
            if (state.seenFriends.isEmpty()) item { Empty("아직 다녀간 친구가 없어요") }
            items(state.seenFriends, key = { it.id }) { FriendRow(it) }
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
private fun FriendRow(friend: Friend) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CharacterAvatar(friend.character.parts, Modifier.size(64.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(friend.character.nickname, style = MaterialTheme.typography.titleMedium)
                Text(friend.character.greeting, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
