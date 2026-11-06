package com.drivepark.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drivepark.app.BuildConfig
import com.drivepark.app.domain.ThemeMode
import com.drivepark.app.domain.StartupMode
import com.drivepark.app.pleos.PleosLocationProvider
import com.drivepark.app.ui.theme.Pleos

@Composable
fun SettingsScreen(vm: SettingsViewModel, onEditCharacter: () -> Unit, onOpenDev: () -> Unit) {
    val state by vm.state.collectAsState()
    val s = state.settings
    var confirmReset by remember { mutableStateOf(false) }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshPermissions() }
    LaunchedEffect(Unit) { vm.refreshPermissions() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineMedium)

        Group("내 캐릭터") {
            ActionRow("캐릭터 바꾸기", "털 색, 표정, 모자, 소품, 닉네임, 인사말") {
                OutlinedButton(onClick = onEditCharacter) { Text("수정") }
            }
        }

        Group("화면") {
            ActionRow("테마", "시스템을 고르면 차량(Pleos) 다크모드 설정을 따라가요") {
                ThemeModePicker(s.themeMode) { m -> vm.set { it.copy(themeMode = m) } }
            }
            ActionRow("시동을 걸면", "인사 후 어떤 화면을 보여줄지 골라요") {
                SegmentedPicker(StartupMode.entries, s.startupMode, StartupMode::label) { m -> vm.set { it.copy(startupMode = m) } }
            }
            SwitchRow("주차 10분 뒤 공원 띄우기", "P에 둔 채 10분이 지나면 공원 화면을 앞으로 띄워요", s.showParkWhenIdle) { v ->
                vm.set { it.copy(showParkWhenIdle = v) }
            }        }

        Group("드라이브") {
            SwitchRow("잠깐 사색에 잠기기", "켜면 친구 방문을 잠시 멈춰요. 지역 캐릭터는 계속 모아요", s.pauseVisits) { v -> vm.set { it.copy(pauseVisits = v) } }
            SwitchRow("도착 알림", "주차하면 놀러온 친구를 알려줘요", s.arrivalNotification) { v -> vm.set { it.copy(arrivalNotification = v) } }
            SwitchRow("인사 음성", "도착 인사를 소리 내어 읽어줘요", s.greetingVoice) { v -> vm.set { it.copy(greetingVoice = v) } }
            SwitchRow("글레오 브리핑", "글레오가 도착 인사를 만들어줘요", s.gleoBriefing) { v -> vm.set { it.copy(gleoBriefing = v) } }
        }

        Group("권한") {
            ActionRow(
                "주행 팝업",
                if (state.overlayGranted) "허용됨" else "주행 중 팝업을 쓰려면 다른 앱 위에 표시 권한이 필요해요",
            ) {}
            ActionRow("위치", if (state.locationGranted) "허용됨" else "위치를 허용하면 지역 캐릭터를 모을 수 있어요") {
                if (!state.locationGranted) Button(onClick = { locationLauncher.launch(PleosLocationProvider.PERMISSION) }) { Text("허용") }
            }
        }

        Group("데이터") {
            ActionRow("데이터 초기화", "친구, 대기 중인 만남, 도감 기록을 지워요. 내 캐릭터는 남아요") {
                OutlinedButton(onClick = { confirmReset = true }) { Text("초기화", color = Color(0xFFD92D20)) }
            }
        }

        Group("앱 정보") {
            Row(
                Modifier.fillMaxWidth().clickable { if (vm.onVersionTapped()) onOpenDev() }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("버전", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(BuildConfig.VERSION_NAME, color = Pleos.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("데이터를 초기화할까요?") },
            text = { Text("친구, 대기 중인 만남, 도감 기록이 모두 지워져요. 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = { vm.resetAll(); confirmReset = false }) { Text("초기화") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, color = Pleos.TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Column(
            Modifier.fillMaxWidth().background(Pleos.Surface, RoundedCornerShape(Pleos.CardRadius)).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ActionRow(title, desc) {
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Pleos.Accent, checkedThumbColor = Color.White),
        )
    }
}

/** 시스템 / 라이트 / 다크 세 칸짜리 선택 버튼. */
@Composable
private fun ThemeModePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) =
    SegmentedPicker(ThemeMode.entries, selected, { m -> when (m) { ThemeMode.SYSTEM -> "시스템"; ThemeMode.LIGHT -> "라이트"; ThemeMode.DARK -> "다크" } }, onSelect)

fun StartupMode.label() = when (this) { StartupMode.TO_NAVI -> "인사 후 내비"; StartupMode.STAY -> "인사 후 공원" }

/** 알약 모양 여러 칸 선택 버튼(설정·초기 설정에서 같이 쓴다). */
@Composable
fun <T> SegmentedPicker(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier.background(Pleos.Background, RoundedCornerShape(50)).border(1.dp, Pleos.Divider, RoundedCornerShape(50)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { m ->
            val on = m == selected
            Text(
                label(m),
                Modifier.background(if (on) Pleos.Text else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onSelect(m) }.padding(horizontal = 20.dp, vertical = 10.dp),
                color = if (on) Pleos.OnText else Pleos.Text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ActionRow(title: String, desc: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, color = Pleos.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        trailing()
    }
}
