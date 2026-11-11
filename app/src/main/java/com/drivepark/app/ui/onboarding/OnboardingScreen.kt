package com.drivepark.app.ui.onboarding

import com.drivepark.app.ui.character.rememberIdleFrame
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.drivepark.app.AppContainer
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.ui.character.defaultEye
import com.drivepark.app.domain.Hat
import com.drivepark.app.ui.character.PERSON_SHAPE
import com.drivepark.app.ui.pixel.Shapes
import com.drivepark.app.domain.AppSettings
import com.drivepark.app.domain.StartupMode
import com.drivepark.app.ui.settings.SegmentedPicker
import com.drivepark.app.ui.settings.label
import com.drivepark.app.ui.character.CharacterAvatar
import com.drivepark.app.ui.character.CreateCharacterScreen
import com.drivepark.app.ui.character.CreateCharacterViewModel
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.theme.Pleos
import com.drivepark.app.pleos.PleosLocationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 첫 실행 초기 설정: 환영 → 권한 → 캐릭터 → 취향 → 완료. */
@Composable
fun OnboardingScreen(
    vm: OnboardingViewModel,
    characterVm: CreateCharacterViewModel,
    container: AppContainer,
    onFinished: () -> Unit,
) {
    val step by vm.step.collectAsState()
    Column(Modifier.fillMaxSize().background(Pleos.Background)) {
        StepDots(step, Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(tween(220)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(220)))
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "onboarding",
        ) { s ->
            when (s) {
                0 -> Welcome(characterVm)
                1 -> Permissions(container)
                OnboardingViewModel.STEP_WHO -> WhoStep { kind -> characterVm.setKind(kind); vm.next() }
                OnboardingViewModel.STEP_CHARACTER ->
                    CreateCharacterScreen(characterVm, title = "캐릭터를 만들어요", saveLabel = "저장하고 다음") { vm.next() }
                else -> Done(characterVm, onFinished)
            }
        }
        // "누구" 단계는 카드를 눌러 넘어가고, 캐릭터 단계는 저장 버튼으로만, 완료 단계는 자체 버튼을 쓴다.
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step in 1 until OnboardingViewModel.LAST_STEP) TextButton(onClick = vm::back, modifier = Modifier.height(64.dp).widthIn(min = 120.dp)) { Text("이전", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.weight(1f))
            if (step != OnboardingViewModel.STEP_WHO && step != OnboardingViewModel.STEP_CHARACTER && step != OnboardingViewModel.LAST_STEP) {
                // 차량 터치 규칙: 높이 64dp 이상.
                Button(onClick = vm::next, modifier = Modifier.height(64.dp).width(220.dp)) { Text("다음", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(OnboardingViewModel.LAST_STEP + 1) { i ->
            val w by animateDpAsState(if (i == step) 28.dp else 10.dp, label = "dot")
            Box(Modifier.size(width = w, height = 10.dp).background(if (i == step) Pleos.Accent else Pleos.Divider, RoundedCornerShape(5.dp)))
        }
    }
}

@Composable
private fun Welcome(characterVm: CreateCharacterViewModel) {
    // 인트로 늑대는 늑대 기본 눈(금색)으로 보여 준다(사용자 캐릭터 상태와 무관, 그림은 그대로).
    val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE, defaultEye("wolf") ?: EyeColor.GREEN)
    val frame = rememberIdleFrame()
    Column(Modifier.fillMaxSize().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        com.drivepark.app.ui.character.CharacterGlow(Modifier.size(300.dp)) {
            CharacterAvatar(parts, Modifier.size(220.dp), frame = frame)
        }
        Spacer(Modifier.height(24.dp))
        Text("스쳐 지나간 차들의 캐릭터가 내 공원에 놀러와요", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(
            "만남은 주차할 때 모아서 전해요. 언제 어디서 만났는지는 아무에게도 보여주지 않아요",
            Modifier.background(Pleos.Surface, RoundedCornerShape(Pleos.CardRadius)).padding(horizontal = 32.dp, vertical = 20.dp),
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Pleos.TextSecondary,
        )
    }
}

/** 누구의 캐릭터인가요? 카드를 누르면 고른 값으로 다음 단계(캐릭터 만들기)로 넘어간다. */
@Composable
private fun WhoStep(onPick: (CharacterKind) -> Unit) {
    val frame = rememberIdleFrame()
    val compact = com.drivepark.app.ui.theme.LocalCompact.current
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("누구의 캐릭터인가요?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("나중에 설정에서 바꿀 수 있어요", color = Pleos.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        val cards = @Composable {
            WhoCard("나", PERSON_SHAPE, frame, Modifier.weight(1f, fill = false)) { onPick(CharacterKind.PERSON) }
            WhoCard("우리 집 반려동물", Shapes.animals.firstOrNull()?.id ?: "wolf", frame, Modifier.weight(1f, fill = false)) { onPick(CharacterKind.PET) }
        }
        if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { cards() }
        else Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) { cards() }
    }
}

@Composable
private fun WhoCard(title: String, shape: String, frame: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .width(280.dp)
            .background(Pleos.Surface, RoundedCornerShape(Pleos.CardRadius))
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CharacterAvatar(
            CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE, defaultEye(shape) ?: EyeColor.GREEN),
            Modifier.size(180.dp), frame = frame, shape = shape,
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Permissions(container: AppContainer) {
    val context = LocalContext.current
    var location by remember { mutableStateOf(container.location.hasPermission()) }
    var notifications by remember { mutableStateOf(Build.VERSION.SDK_INT < 33) }
    var overlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    // Pleos 차량에는 '다른 앱 위에 표시' 설정 화면이 없을 수 있다 → 처음부터 알고 '허용' 버튼 대신 대안을 안내한다.
    val overlayIntent = remember { Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")) }
    val overlayUnsupported = remember { overlayIntent.resolveActivity(context.packageManager) == null }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { location = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifications = it }

    Column(Modifier.fillMaxSize().padding(horizontal = 80.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("필요한 권한을 허용해 주세요", style = MaterialTheme.typography.headlineMedium)
        Text("나중에 설정에서도 바꿀 수 있어요", color = Pleos.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        PermissionRow("위치", "지역 캐릭터를 모을 때 써요", location) { locationLauncher.launch(PleosLocationProvider.PERMISSION) }
        PermissionRow("알림", "주차하면 놀러 온 친구를 알려줘요", notifications) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        PermissionRow(
            "주행 팝업",
            if (overlayUnsupported && !overlay) "이 차량에서는 아직 켤 수 없어요. 3분할 칸에 Waveby를 두면 주행 중에도 내 캐릭터가 보여요"
            else "주행 중 내비 위에 '친구를 찾는 중…'을 띄워요",
            overlay,
            allowable = !overlayUnsupported,
        ) { context.startActivity(overlayIntent) }
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, granted: Boolean, allowable: Boolean = true, onAllow: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Pleos.Surface, RoundedCornerShape(Pleos.CardRadius)).padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, color = Pleos.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        if (granted) Text("✓ 허용됨", color = Pleos.Accent, style = MaterialTheme.typography.titleMedium)
        else if (allowable) OutlinedButton(onClick = onAllow, modifier = Modifier.height(64.dp).widthIn(min = 120.dp)) { Text("허용", style = MaterialTheme.typography.titleMedium) }
    }
}

/** 완료: 내 캐릭터가 왼쪽에서 가운데로 걸어 들어온다. */
@Composable
private fun Done(characterVm: CreateCharacterViewModel, onFinished: () -> Unit) {
    val me = characterVm.state.collectAsState().value
    val parts = me.parts
    val progress = remember { Animatable(0f) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2400, easing = LinearEasing)) }
    LaunchedEffect(Unit) { while (true) { delay(if (progress.value < 1f) 300 else 600); frame++ } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val avatar = 200.dp
        val startX = 0.dp
        val endX = (maxWidth - avatar) / 2
        Box(Modifier.offset(x = startX + (endX - startX) * progress.value, y = maxHeight * 0.18f)) {
            CharacterAvatar(
                parts, Modifier.size(avatar), pose = if (progress.value < 1f) Pose.WALK else Pose.IDLE, frame = frame, mirrored = true,
                shape = me.shape, look = me.look, kind = me.kind, drawing = me.drawing,
            )
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("준비 끝! 이제 드라이브를 떠나 볼까요?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onFinished, modifier = Modifier.height(64.dp).width(260.dp)) { Text("공원으로") }
        }
    }
}
