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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.drivepark.app.ui.theme.Pleos
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.drivepark.app.domain.DRAWN_SHAPE
import com.drivepark.app.ui.drawing.DrawingScreen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.SkinTone
import com.drivepark.app.ui.pixel.Shapes
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat

@Composable
fun CreateCharacterScreen(
    vm: CreateCharacterViewModel,
    title: String = "내 캐릭터 만들기",
    saveLabel: String = "저장하고 공원으로",
    /** 설정에서 수정할 때만 맨 위에 "누구: 나 / 반려동물"을 보인다(초기 설정은 앞 단계에서 고름). */
    showKindRow: Boolean = false,
    onSaved: () -> Unit,
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) {
            vm.consumeSaved()
            onSaved()
        }
    }

    // 그리는 중이면 같은 자리에서 그리기 화면을 보인다(초기 설정·설정 둘 다 같은 뷰모델을 쓴다).
    state.board?.let { board ->
        DrawingScreen(
            board, state.parts, state.kind, state.look,
            onChange = vm::updateBoard, onFinish = vm::finishDrawing, onCancel = vm::cancelDrawing,
        )
        return
    }

    // 차량 가로 화면: 왼쪽 미리보기, 오른쪽 선택. 3분할처럼 좁으면 미리보기를 줄인다.
    // 이름표 칸: 저장했는데 비어 있으면 그 칸까지 내려가 바로 입력하게 하고, 키보드의 다음/완료로 칸을 옮긴다.
    val scroll = rememberScrollState()
    val nameFocus = remember { FocusRequester() }
    val greetingFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.error) {
        if (state.error == null) return@LaunchedEffect
        val target = when {
            state.nickname.isBlank() -> nameFocus
            state.greeting.isBlank() -> greetingFocus
            else -> null
        } ?: return@LaunchedEffect
        scroll.animateScrollTo(scroll.maxValue)
        runCatching { target.requestFocus() }
    }

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
            CharacterGlow(Modifier.size(previewW)) {
                CharacterAvatar(
                    state.parts, Modifier.size(previewW * 0.8f), shape = state.shape, look = state.look,
                    kind = state.kind, drawing = state.drawing,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(state.nickname.ifBlank { state.nameLabel }, style = MaterialTheme.typography.titleLarge)
            Text(state.greeting.ifBlank { "인사말" }, style = MaterialTheme.typography.bodyLarge)
            // 저장은 미리보기 아래에 고정: 오른쪽 목록을 끝까지 내리지 않아도 바로 누를 수 있다. 오류도 버튼 바로 위에.
            Spacer(Modifier.height(24.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text(saveLabel) }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().imePadding().verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 소제목 네 묶음: 누구·모습 → 색 → 꾸미기 → 이름표.
            SectionTitle(if (showKindRow) "누구·모습" else "모습", first = true)
            if (showKindRow) {
                PartRow("누구", CharacterKind.entries, state.kind, CharacterKind::label) { vm.setKind(it) }
            }
            PartRow("모습", state.shapeChoices, state.shape, ::shapeLabel) { vm.setShape(it) }
            OutlinedButton(onClick = { vm.startDrawing() }, modifier = Modifier.height(64.dp)) {
                Text(if (state.hasDrawing) "내 그림 고치기" else "직접 그리기")
            }
            SectionTitle("색")
            PartRow(
                when {
                    state.isPersonShape -> "상의 색"
                    state.shape == DRAWN_SHAPE -> "몸 색"
                    else -> "털 색"
                },
                state.bodyChoices, state.parts.body, BodyColor::furLabel,
            ) { vm.setParts(state.parts.copy(body = it)) }
            if (state.showBottomRow) {
                PartRow("하의 색", BottomColor.entries, state.look.bottom, BottomColor::label) { vm.setLook(state.look.copy(bottom = it)) }
            }
            if (state.isPersonShape) {
                PartRow("피부 색", SkinTone.entries, state.look.skin, SkinTone::label) { vm.setLook(state.look.copy(skin = it)) }
            }
            if (state.showHairRow) {
                PartRow("머리 색", HairColor.entries, state.look.hair, HairColor::label) { vm.setLook(state.look.copy(hair = it)) }
            }
            PartRow("눈 색", EyeColor.entries, state.parts.eye, EyeColor::label) { vm.setParts(state.parts.copy(eye = it)) }
            SectionTitle("꾸미기")
            PartRow("얼굴", Face.entries, state.parts.face, Face::label) { vm.setParts(state.parts.copy(face = it)) }
            PartRow("모자", Hat.entries, state.parts.hat, Hat::label) { vm.setParts(state.parts.copy(hat = it)) }
            PartRow("소품", Accessory.entries, state.parts.accessory, Accessory::label) { vm.setParts(state.parts.copy(accessory = it)) }
            SectionTitle("이름표")
            OutlinedTextField(
                value = state.nickname, onValueChange = vm::setNickname, singleLine = true,
                label = { Text("${state.nameLabel} (1~${Character.NICKNAME_MAX}자)") }, modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { greetingFocus.requestFocus() }),
            )
            OutlinedTextField(
                value = state.greeting, onValueChange = vm::setGreeting, singleLine = true,
                label = { Text("인사말 (1~${Character.GREETING_MAX}자)") }, modifier = Modifier.fillMaxWidth().focusRequester(greetingFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 묶음 소제목(설정 화면의 묶음 이름과 같은 모양: 회색 작은 글자). 첫 묶음 말고는 위에 여백을 둔다. */
@Composable
private fun SectionTitle(text: String, first: Boolean = false) {
    Text(
        text, color = Pleos.TextSecondary, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = if (first) 0.dp else 16.dp),
    )
}

/** 선택지가 폭을 넘으면 다음 줄로 넘긴다(3분할에서도 모든 선택지가 보이게). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> PartRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        // 차량 화면 터치 영역 64dp(AGENTS.md). 글자도 한 단계 크게.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val on = option == selected
                // 고른 칩은 그리기 화면 붓처럼 주황 테두리 + 주황 굵은 글자(다크에서도 한눈에 보이게).
                FilterChip(
                    selected = on, onClick = { onSelect(option) },
                    label = {
                        Text(
                            label(option), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp),
                            fontWeight = if (on) FontWeight.Bold else null,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent, selectedLabelColor = Pleos.Accent,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = on,
                        borderColor = Pleos.Divider, selectedBorderColor = Pleos.Accent,
                        borderWidth = 1.dp, selectedBorderWidth = 3.dp,
                    ),
                    modifier = Modifier.height(64.dp),
                )
            }
        }
    }
}

private fun shapeLabel(id: String) = if (id == DRAWN_SHAPE) "내 그림" else Shapes.def(id).label

private fun Face.label() = when (this) {
    Face.SMILE -> "웃음"; Face.WINK -> "윙크"; Face.SURPRISED -> "놀람"; Face.SLEEPY -> "졸림"
}

private fun Hat.label() = when (this) {
    Hat.NONE -> "없음"; Hat.CAP -> "캡모자"; Hat.BEANIE -> "비니"; Hat.CROWN -> "왕관"
}

private fun CharacterKind.label() = when (this) { CharacterKind.PERSON -> "나"; CharacterKind.PET -> "반려동물" }

private fun BottomColor.label() = when (this) {
    BottomColor.NAVY -> "남색"; BottomColor.DENIM -> "데님"; BottomColor.BLACK -> "검정"; BottomColor.BEIGE -> "베이지"; BottomColor.GRAY -> "회색"
}

private fun SkinTone.label() = when (this) { SkinTone.LIGHT -> "밝은"; SkinTone.MEDIUM -> "중간"; SkinTone.DEEP -> "짙은" }

private fun HairColor.label() = when (this) {
    HairColor.BLACK -> "검정"; HairColor.BROWN -> "갈색"; HairColor.BLOND -> "금발"; HairColor.GRAY -> "회색"
}

private fun Accessory.label() = when (this) {
    Accessory.NONE -> "없음"; Accessory.GLASSES -> "안경"; Accessory.SCARF -> "목도리"; Accessory.BALLOON -> "풍선"
}

private fun EyeColor.label() = when (this) {
    EyeColor.GREEN -> "초록"; EyeColor.BLUE -> "파랑"; EyeColor.BROWN -> "갈색"
    EyeColor.BLACK -> "검정"; EyeColor.PURPLE -> "보라"; EyeColor.GOLD -> "금색"
}
