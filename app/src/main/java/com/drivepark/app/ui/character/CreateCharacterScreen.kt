package com.drivepark.app.ui.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.drivepark.app.ui.theme.LocalCompact
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat

@Composable
fun CreateCharacterScreen(
    vm: CreateCharacterViewModel,
    title: String = "내 캐릭터 만들기",
    saveLabel: String = "저장하고 공원으로",
    onSaved: () -> Unit,
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    // 차량 가로 화면: 왼쪽 미리보기, 오른쪽 선택. 3분할처럼 좁으면 미리보기를 줄인다.
    val compact = LocalCompact.current
    val previewW = if (compact) 220.dp else 320.dp
    Row(Modifier.fillMaxSize().padding(if (compact) 20.dp else 32.dp), horizontalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 40.dp)) {
        Column(
            Modifier.width(previewW).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(title, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            CharacterGlow(Modifier.size(previewW)) { CharacterAvatar(state.parts, Modifier.size(previewW * 0.8f)) }
            Spacer(Modifier.height(16.dp))
            Text(state.nickname.ifBlank { "닉네임" }, style = MaterialTheme.typography.titleLarge)
            Text(state.greeting.ifBlank { "인사말" }, style = MaterialTheme.typography.bodyLarge)
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PartRow("털 색", BodyColor.entries.sortedBy { it != BodyColor.PURPLE }, state.parts.body, BodyColor::furLabel) { vm.setParts(state.parts.copy(body = it)) }
            PartRow("얼굴", Face.entries, state.parts.face, Face::label) { vm.setParts(state.parts.copy(face = it)) }
            PartRow("모자", Hat.entries, state.parts.hat, Hat::label) { vm.setParts(state.parts.copy(hat = it)) }
            PartRow("소품", Accessory.entries, state.parts.accessory, Accessory::label) { vm.setParts(state.parts.copy(accessory = it)) }
            OutlinedTextField(
                value = state.nickname, onValueChange = vm::setNickname, singleLine = true,
                label = { Text("닉네임 (1~${Character.NICKNAME_MAX}자)") }, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.greeting, onValueChange = vm::setGreeting, singleLine = true,
                label = { Text("인사말 (1~${Character.GREETING_MAX}자)") }, modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(saveLabel) }
        }
    }
}

/** 선택지가 폭을 넘으면 다음 줄로 넘긴다(3분할에서도 모든 선택지가 보이게). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PartRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

private fun Face.label() = when (this) {
    Face.SMILE -> "웃음"; Face.WINK -> "윙크"; Face.SURPRISED -> "놀람"; Face.SLEEPY -> "졸림"
}

private fun Hat.label() = when (this) {
    Hat.NONE -> "없음"; Hat.CAP -> "캡모자"; Hat.BEANIE -> "비니"; Hat.CROWN -> "왕관"
}

private fun Accessory.label() = when (this) {
    Accessory.NONE -> "없음"; Accessory.GLASSES -> "안경"; Accessory.SCARF -> "목도리"; Accessory.BALLOON -> "풍선"
}
