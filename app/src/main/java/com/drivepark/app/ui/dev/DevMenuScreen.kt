package com.drivepark.app.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drivepark.app.AppContainer
import com.drivepark.app.domain.DriveState
import kotlinx.coroutines.launch

/** 시연·테스트용 메뉴. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevMenuScreen(container: AppContainer, onBack: () -> Unit) {
    var count by remember { mutableIntStateOf(3) }
    var running by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf<String?>(null) }
    val override by container.monitor.override.collectAsState()
    val state by container.monitor.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 돌아가기") }
            Spacer(Modifier.width(12.dp))
            Text("개발자 메뉴", style = MaterialTheme.typography.headlineMedium)
        }

        Text("순정 내비 모의 주행", style = MaterialTheme.typography.titleLarge)
        Text("현재 주행 상태: ${state.label()} · 덮어쓰기: ${override?.label() ?: "없음(실제 기어)"}")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                container.monitor.setOverride(DriveState.DRIVING)
                val ok = container.naviDemo.startSimulatedDrive(HOENGSEONG_LAT, HOENGSEONG_LNG, "횡성")
                last = if (ok) "횡성까지 모의 주행을 요청했어요" else "내비 연동 불가, 기어 덮어쓰기만 실행"
                // 내비가 앞에 있어야 모의 주행 요청을 받는다(가설 검증 중).
                if (ok) context.packageManager.getLaunchIntentForPackage(NAVI_PACKAGE)?.let(context::startActivity)
            }) { Text("순정 내비 모의 주행 (횡성)") }
            OutlinedButton(onClick = { container.monitor.setOverride(DriveState.PARKED) }) { Text("주차") }
            OutlinedButton(onClick = { container.monitor.setOverride(null) }) { Text("덮어쓰기 해제") }
        }

        // 졸음 휴식 권유 시연: 주행(기어 덮어쓰기 D) 중에 누르면 바로 "쉴 때"(팝업·잠금 화면이 조는 모습).
        // 그 뒤 "주차"를 누르면 휴식 권유 화면이 뜬다.
        Text("휴식 권유 시연", style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                container.monitor.setOverride(DriveState.DRIVING)
                last = "주행으로 덮어썼어요. 잠시 뒤 '쉴 때로 건너뛰기'를 누르세요"
            }) { Text("주행 시작(덮어쓰기)") }
            OutlinedButton(onClick = {
                container.restReminder.markDueNow()
                last = "쉴 때로 건너뛰었어요(주행 중일 때만). '주차'를 누르면 권유 화면"
            }) { Text("쉴 때로 건너뛰기") }
            OutlinedButton(onClick = { container.monitor.setOverride(DriveState.PARKED) }) { Text("주차") }
        }

        OutlinedButton(onClick = { container.greeter.speak("안녕하세요, 웨이브바이예요") }) { Text("음성 테스트") }

        // "주차 10분 뒤 공원 안내"를 10분 기다리지 않고 확인: 내비를 띄우고 5초 뒤 내비 위에 안내 박스를 띄운다.
        OutlinedButton(onClick = {
            com.drivepark.app.drive.ScreenSwitcher.openNavi(context)
            scope.launch {
                kotlinx.coroutines.delay(5_000)
                container.showIdleParkBanner()
            }
            last = "내비를 띄웠어요. 5초 뒤 내비 위에 공원 안내가 떠야 해요(15초 뒤 저절로 사라짐)"
        }) { Text("공원 안내 시험 (5초)") }

        HorizontalDivider()

        Text("즉시 도착 시뮬레이션", style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..5).forEach { n -> FilterChip(selected = count == n, onClick = { count = n }, label = { Text("$n 명") }) }
        }
        Button(
            enabled = !running,
            onClick = {
                running = true
                scope.launch {
                    val s = container.driveSession.simulateDrive(count)
                    last = if (s.isEmpty) "전달할 것이 없어 조용히 끝났어요" else "친구 ${s.friends.size}명 전달, 새 지역: ${s.newRegion?.name ?: "없음"}"
                    running = false
                }
            },
        ) { Text(if (running) "실행 중…" else "드라이브 시뮬레이션 실행") }

        last?.let { Text("결과: $it") }
    }
}

private fun DriveState.label() = if (this == DriveState.DRIVING) "주행(D)" else "주차(P)"

private const val HOENGSEONG_LAT = 37.49
private const val HOENGSEONG_LNG = 127.98
private const val NAVI_PACKAGE = com.drivepark.app.drive.ScreenSwitcher.NAVI_PACKAGE
