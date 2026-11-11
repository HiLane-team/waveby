package com.drivepark.app.ui.drawing

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.DRAWING_PALETTE
import com.drivepark.app.domain.DRAWN_SHAPE
import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.GridSize
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.ui.park.WALK_FRAME_MS
import com.drivepark.app.ui.pixel.Anatomy
import com.drivepark.app.ui.pixel.BodyLines
import com.drivepark.app.ui.pixel.CharacterRenderer
import com.drivepark.app.ui.pixel.DrawingHint
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.pixel.StandSprite
import com.drivepark.app.ui.theme.LocalAnimationsEnabled
import com.drivepark.app.ui.theme.LocalCompact
import com.drivepark.app.ui.theme.Pleos
import kotlinx.coroutines.delay

/** 차량 화면 버튼 최소 크기. */
private val TOUCH = 64.dp

/** 손을 멈춘 뒤 미리보기·안내를 다시 계산하기까지 기다리는 시간. */
private const val SETTLE_MS = 150L

/** 안내 문구(저장은 막지 않는다). */
internal fun DrawingHint.message(): String = when (this) {
    DrawingHint.NO_FEET -> "발이 맨 아랫줄에 닿게 그려 주세요"
    DrawingHint.NO_HEAD -> "머리를 안내선 위쪽에 그려 주세요"
    DrawingHint.STRAY_PIXEL -> "떨어진 점이 있어요"
}

private fun Brush.label(): String = when (this) {
    Brush.OUTLINE -> "테두리"; Brush.BODY -> "몸 색"; Brush.LIGHT -> "밝은 색"; Brush.SHADE -> "그늘"
    Brush.EYE -> "눈"; Brush.NOSE -> "코"; Brush.HAIR -> "머리카락"; Brush.BOTTOM -> "하의"
    Brush.FIXED1 -> "색 1"; Brush.FIXED2 -> "색 2"; Brush.FIXED3 -> "색 3"; Brush.FIXED4 -> "색 4"; Brush.ERASER -> "지우개"
}

/**
 * 직접 그리기 화면. 그림판 상태는 바깥(뷰모델)이 들고, 여기서는 바뀐 판을 [onChange]로 돌려준다.
 * 완성하면 [onFinish]에 저장 형식 글자를 넘긴다. [onCancel]은 그리던 것을 버린다.
 */
@Composable
fun DrawingScreen(
    board: DrawingBoard,
    parts: CharacterParts,
    kind: CharacterKind,
    look: PersonLook,
    onChange: (DrawingBoard) -> Unit,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
) {
    // 시스템 뒤로 가기 = 돌아가기(그리던 것을 버리고 캐릭터 만들기로). 초기 설정 단계가 넘어가지 않게 여기서 받는다.
    BackHandler(onBack = onCancel)
    if (LocalCompact.current) {
        // 3분할처럼 좁은 화면에서는 칸이 너무 작아 그릴 수 없다.
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("넓은 화면에서 그려 주세요", style = MaterialTheme.typography.headlineSmall, color = Pleos.Text)
            Spacer(Modifier.height(8.dp))
            Text("화면을 크게 펼치면 그릴 수 있어요", color = Pleos.TextSecondary)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.height(TOUCH)) { Text("돌아가기") }
        }
        return
    }


    var brush by remember { mutableStateOf(Brush.OUTLINE) }
    val size = board.size
    // 커서: 처음엔 가운데. 크기가 바뀌면 새 판 안으로 다시 맞춘다.
    var cursor by remember { mutableStateOf(Cursor(size.w / 2, size.h / 2)) }
    if (cursor.x >= size.w || cursor.y >= size.h) cursor = cursor.moved(0, 0, size)
    // 칠하며 옮기기: 켜져 있는 동안 커서가 지나간 칸을 모두 칠한다(켤 때~끌 때 = 되돌리기 한 번).
    var painting by remember { mutableStateOf(false) }
    var askSize by remember { mutableStateOf<GridSize?>(null) }
    val colors = remember(parts.body, parts.eye, look, board.colors) {
        CharacterRenderer.drawingColors(parts.body, parts.eye, look, board.colors).mapValues { (_, argb) -> Color(argb) }
    }
    val current by rememberUpdatedState(board)
    val currentBrush by rememberUpdatedState(brush)

    /** 커서를 한 칸씩 옮긴다(칠하며 옮기기면 지나간 칸마다 칠한다). */
    fun move(dx: Int, dy: Int) {
        var b = current
        var c = cursor
        val steps = List(kotlin.math.abs(dx)) { Integer.signum(dx) to 0 } + List(kotlin.math.abs(dy)) { 0 to Integer.signum(dy) }
        for ((sx, sy) in steps) {
            c = c.moved(sx, sy, size)
            if (painting) b = b.paint(c.x, c.y, currentBrush)
        }
        cursor = c
        if (b != current) onChange(b)
    }

    // 손가락으로 그리기: 누른 칸을 바로 칠하고, 문지르면 지나간 칸을 모두 칠한다(손을 뗄 때까지 = 되돌리기 한 번).
    // 이벤트가 화면 갱신보다 빨리 와도 칸을 잃지 않게, 문지르는 동안의 판은 여기서 직접 들고 있다.
    val finger = remember { FingerStroke() }

    fun fingerDown(c: Cursor) {
        if (painting) painting = false
        val b = current.endStroke().beginStroke().paint(c.x, c.y, currentBrush)
        finger.board = b
        finger.last = c
        cursor = c
        onChange(b)
    }

    fun fingerMove(c: Cursor) {
        var b = finger.board ?: return
        val last = finger.last ?: c
        if (c == last) return
        for (p in cellsBetween(last, c).drop(1)) b = b.paint(p.x, p.y, currentBrush)
        finger.board = b
        finger.last = c
        cursor = c
        onChange(b)
    }

    fun fingerUp() {
        val b = finger.board ?: return
        finger.board = null
        finger.last = null
        onChange(b.endStroke())
    }

    fun togglePainting() {
        if (painting) {
            painting = false
            onChange(current.endStroke())
        } else {
            painting = true
            onChange(current.beginStroke().paint(cursor.x, cursor.y, currentBrush))
        }
    }

    // 칠할 때마다 미리보기·안내를 다시 계산하지 않고, 손을 잠깐 멈추면 한 번에 계산한다(차량 기기 부하).
    // 저장 글자(색 줄 포함)를 기준으로 기다린다: 색만 바꿔도 미리보기가 따라 바뀐다.
    val text = board.toText()
    var settled by remember { mutableStateOf(text) }
    LaunchedEffect(text) {
        if (settled != text) delay(SETTLE_MS)
        settled = text
    }
    val hints = remember(settled) {
        Drawing.parse(settled)?.let { Anatomy.of(StandSprite(it.rows)).hints.distinct() }.orEmpty()
    }

    // 크기 확인 창은 주행 잠금 화면 위에 따로 뜬다. 주행이 시작되면 닫는다.
    val driving = com.drivepark.app.ui.theme.LocalDriving.current
    LaunchedEffect(driving) { if (driving) askSize = null }
    askSize?.takeIf { !driving }?.let { to ->
        SizeDialog(
            board, to, colors,
            onConfirm = {
                if (painting) { painting = false }
                onChange(current.endStroke().resized(to))
                askSize = null
            },
            onDismiss = { askSize = null },
        )
    }

    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        // 왼쪽: 그림판(손가락으로 누르거나 문질러 칠함) + 한 칸씩 고치는 보조 조작(화살표·칠하기).
        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val labelW = 40.dp
                val cell = min((maxWidth - labelW) / size.w, maxHeight / size.h)
                Row(verticalAlignment = Alignment.Top) {
                    ZoneLabels(size, cell, labelW)
                    BoardCanvas(
                        board, colors, cell, cursor, guides = true,
                        touch = BoardTouch(::fingerDown, ::fingerMove, ::fingerUp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("칸을 누르거나 문질러서 칠해요 · 한 칸씩 고칠 때는 아래 버튼", color = Pleos.TextSecondary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("←") { move(-1, 0) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tool("↑") { move(0, -1) }
                    Tool("↓") { move(0, 1) }
                }
                Tool("→") { move(1, 0) }
                Spacer(Modifier.width(16.dp))
                // 칠하기(크게)와 칠하며 옮기기(켜고 끄기)를 위아래로. 좁은 폭에서도 글자가 꺾이지 않게 너비를 나눠 쓴다.
                Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onChange(current.paint(cursor.x, cursor.y, brush)) },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                    ) { Text("칠하기", style = MaterialTheme.typography.titleLarge) }
                    OutlinedButton(
                        onClick = { togglePainting() },
                        modifier = Modifier.fillMaxWidth().height(TOUCH),
                        border = BorderStroke(if (painting) 3.dp else 1.dp, if (painting) Pleos.Accent else Pleos.Divider),
                    ) { Text(if (painting) "칠하며 옮기기 켜짐" else "칠하며 옮기기", color = if (painting) Pleos.Accent else Pleos.Text) }
                }
            }
        }

        // 오른쪽: 크기·붓·도구·미리보기·안내·완성.
        Column(
            Modifier.width(360.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("직접 그리기", style = MaterialTheme.typography.headlineSmall, color = Pleos.Text)
            SizeRow(size) { if (it != size) askSize = it }
            BrushRow(brush, colors) { brush = it }
            if (brush.letter in Drawing.FIXED_LETTERS) {
                // 고정 색 붓을 고르면 바로 아래에 색판을 펼친다. 색을 바꾸면 그 붓으로 칠한 칸이 모두 바뀐다.
                ColorPalette(board.colors[brush.letter]) { argb -> onChange(board.setColor(brush.letter, argb)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tool("되돌리기", enabled = board.canUndo && !painting) { onChange(board.undo()) }
                Tool("빈 칸으로", enabled = !painting) { onChange(board.clear()) }
            }
            Box(
                Modifier.fillMaxWidth().height(180.dp).background(Pleos.Surface, RoundedCornerShape(Pleos.CardRadius)),
                contentAlignment = Alignment.Center,
            ) {
                WalkPreview(settled, parts, kind, look, Modifier.size(150.dp))
            }
            hints.forEach { h ->
                Text(
                    "⚠ ${h.message()}",
                    color = Color(0xFF7A5A00),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF4C2), RoundedCornerShape(12.dp)).padding(12.dp),
                )
            }
            if (board.isBlank) {
                // 빈 그림은 공원·주행 팝업에서 보이지 않는 캐릭터가 된다.
                Text("그림을 한 칸 이상 칠하면 완성할 수 있어요", color = Pleos.TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(TOUCH)) { Text("돌아가기") }
                Button(
                    onClick = { onFinish(board.endStroke().toText()) }, enabled = !board.isBlank,
                    modifier = Modifier.weight(1f).height(TOUCH),
                ) { Text("완성") }
            }
        }
    }
}

private fun GridSize.label(): String = when (this) {
    GridSize.BASIC -> "기본"; GridSize.ROUND -> "둥근"; GridSize.WIDE -> "가로"; GridSize.TALL -> "세로"
}

/** 그림판 크기 고르기(4종). 다른 크기를 누르면 확인 창이 뜬다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SizeRow(selected: GridSize, onPick: (GridSize) -> Unit) {
    Text("크기", style = MaterialTheme.typography.titleMedium, color = Pleos.Text)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GridSize.entries.forEach { s ->
            val on = s == selected
            OutlinedButton(
                onClick = { onPick(s) },
                modifier = Modifier.height(TOUCH).width(80.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                border = BorderStroke(if (on) 3.dp else 1.dp, if (on) Pleos.Accent else Pleos.Divider),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.label(), color = if (on) Pleos.Accent else Pleos.Text, fontWeight = if (on) FontWeight.Bold else null)
                    Text("${s.w}×${s.h}", style = MaterialTheme.typography.labelSmall, color = Pleos.TextSecondary)
                }
            }
        }
    }
}

/** 크기 바꾸기 확인: 바꾼 그림 미리보기와 잘리는 칸 수. [바꾸기]를 눌러야 판이 바뀐다(원래 그림은 그대로 남음). */
@Composable
private fun SizeDialog(board: DrawingBoard, to: GridSize, colors: Map<Char, Color>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val preview = remember(board, to) { board.resized(to) }
    val cut = remember(board, to) { board.cropCount(to) }
    // 기본 AlertDialog는 버튼을 작게 줄이므로, 차량 규칙(64dp)대로 직접 그린다.
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.background(Pleos.Surface, RoundedCornerShape(Pleos.CardRadius)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${to.label()} ${to.w}×${to.h}로 바꿀까요?", style = MaterialTheme.typography.titleLarge, color = Pleos.Text)
                BoardCanvas(preview, colors, 8.dp, cursor = null, guides = false, touch = null)
                Text("그림은 아래 가운데에 맞춰 옮겨요. 되돌리기로 원래대로 돌아갈 수 있어요.", color = Pleos.TextSecondary)
                if (cut > 0) Text("${cut}칸이 잘려요", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.width(140.dp).height(TOUCH)) { Text("그대로") }
                Button(onClick = onConfirm, modifier = Modifier.width(140.dp).height(TOUCH)) { Text("바꾸기") }
            }
        }
    }
}

/** 그림판 왼쪽 구역 이름. 안내선(머리 끝·다리 시작 줄)과 높이를 맞춘다. */
@Composable
private fun ZoneLabels(size: GridSize, cell: Dp, width: Dp) {
    val lines = BodyLines.default(size)
    val zones = listOf(
        "머리" to lines.headEnd + 1,
        "몸" to lines.legsFrom - lines.headEnd - 1,
        "다리" to size.h - lines.legsFrom,
    )
    Column(Modifier.width(width)) {
        zones.forEach { (name, count) ->
            Box(Modifier.width(width).height(cell * count), contentAlignment = Alignment.Center) {
                Text(name, color = Pleos.TextSecondary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** 문지르는 동안의 판과 마지막 칸(화면 갱신과 상관없이 이어 칠하려고 들고 있는다). */
private class FingerStroke {
    var board: DrawingBoard? = null
    var last: Cursor? = null
}

/** 그림판 손가락 이벤트: 누름(칸), 문지름(지금 칸), 뗌. */
private class BoardTouch(val down: (Cursor) -> Unit, val move: (Cursor) -> Unit, val up: () -> Unit)

/**
 * 칸 그림(전체). [touch]가 있으면 손가락이 닿은 칸을 바로 칠한다(누르기·문지르기). 판 밖으로 나간 손가락은 무시한다.
 * 빈칸은 바둑판, [guides]면 머리/몸/다리 안내선을 점선으로, [cursor] 칸(마지막으로 칠한 칸)은 주황 테두리.
 */
@Composable
private fun BoardCanvas(
    board: DrawingBoard,
    colors: Map<Char, Color>,
    cell: Dp,
    cursor: Cursor?,
    guides: Boolean,
    touch: BoardTouch?,
) {
    val size = board.size
    val noseMark = if (Pleos.IsDark) Color(0xFFF4F4F5) else Color(0xFFFF7A1A)
    // 빈칸 바둑판. 다크에서는 테두리 붓(거의 검정)과 구분되도록 중간 회색으로 둔다.
    val dark = Pleos.IsDark
    val checkA = if (dark) Color(0xFF5B5B64) else Pleos.Surface
    val checkB = if (dark) Color(0xFF4E4E56) else Pleos.Divider
    val guide = Pleos.TextSecondary
    val accent = Pleos.Accent
    val handler by rememberUpdatedState(touch)
    var touchModifier: Modifier = Modifier
    if (touch != null) {
        touchModifier = Modifier.pointerInput(cell, size) {
            val px = cell.toPx()
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                cellAt(down.position.x, down.position.y, px, size)?.let { handler?.down(it) }
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    cellAt(change.position.x, change.position.y, px, size)?.let { handler?.move(it) }
                }
                handler?.up()
            }
        }
    }
    Canvas(Modifier.size(cell * size.w, cell * size.h).then(touchModifier)) {
        val s = cell.toPx()
        for (y in 0 until size.h) for (x in 0 until size.w) {
            val ch = board.at(x, y)
            val color = colors[ch] ?: if ((x + y) % 2 == 0) checkA else checkB
            drawRect(color, Offset(x * s, y * s), Size(s + 0.5f, s + 0.5f))
            // 코(P)는 테두리와 같은 색으로 그려지므로 그림판에서만 작은 점으로 구분한다.
            if (ch == Brush.NOSE.letter) drawCircle(noseMark, s * 0.15f, Offset(x * s + s / 2, y * s + s / 2))
        }
        if (guides) {
            val lines = BodyLines.default(size)
            val dash = PathEffect.dashPathEffect(floatArrayOf(s * 0.4f, s * 0.3f))
            for (afterRow in listOf(lines.headEnd, lines.legsFrom - 1)) {
                val y = (afterRow + 1) * s
                drawLine(guide, Offset(0f, y), Offset(this.size.width, y), strokeWidth = 3f, pathEffect = dash)
            }
        }
        cursor?.let { c ->
            val w = 3.dp.toPx()
            drawRect(accent, Offset(c.x * s + w / 2, c.y * s + w / 2), Size(s - w, s - w), style = Stroke(w))
        }
    }
}


/** 걷는 미리보기(공원과 같은 110ms). 주행 중·쉬는 중에는 멈춘다. 그림이 바뀔 때마다 4장만 새로 만든다. */
@Composable
private fun WalkPreview(text: String, parts: CharacterParts, kind: CharacterKind, look: PersonLook, modifier: Modifier) {
    val frames = remember(text, parts, kind, look) {
        (0..3).map { f ->
            val img = CharacterRenderer.render(parts, Pose.WALK, f, shape = DRAWN_SHAPE, kind = kind, look = look, drawing = text)
            Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
    var frame by remember { mutableIntStateOf(0) }
    if (LocalAnimationsEnabled.current) {
        LaunchedEffect(Unit) { while (true) { delay(WALK_FRAME_MS.toLong()); frame = (frame + 1) % 4 } }
    }
    Image(frames[frame], contentDescription = "걷는 미리보기", modifier = modifier, filterQuality = FilterQuality.None)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrushRow(selected: Brush, colors: Map<Char, Color>, onSelect: (Brush) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Brush.PICKABLE.forEach { b ->
            val on = b == selected
            Column(
                Modifier.size(width = 80.dp, height = 84.dp)
                    .border(BorderStroke(if (on) 3.dp else 1.dp, if (on) Pleos.Accent else Pleos.Divider), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(b) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val swatch = colors[b.letter]
                Box(
                    Modifier.size(36.dp).background(swatch ?: Pleos.Surface, RoundedCornerShape(8.dp))
                        .border(1.dp, swatch?.let { swatchEdge(it) } ?: Pleos.Divider, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (swatch == null) Text("✕", color = Pleos.TextSecondary)
                    if (b == Brush.NOSE) Box(Modifier.size(8.dp).background(Color.White, RoundedCornerShape(4.dp)))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    b.label(), style = MaterialTheme.typography.labelMedium,
                    color = if (on) Pleos.Accent else Pleos.Text, fontWeight = if (on) FontWeight.Bold else null,
                )
            }
        }
    }
}

/** 색판 20색의 이름(화면 낭독용). [DRAWING_PALETTE]와 같은 순서. */
private val PALETTE_NAMES = listOf(
    "빨강", "주황", "노랑", "연두", "초록", "청록", "파랑", "남색",
    "보라", "분홍", "갈색", "베이지", "흰색", "회색", "진회색", "검정",
    "밝은 피부", "중간 피부", "구릿빛 피부", "검은 머리",
)

/** 고정 색 붓의 색판(4×5, 칸 64dp). 고른 색에 주황 테두리. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(selected: Int?, onPick: (Int) -> Unit) {
    FlowRow(
        maxItemsInEachRow = 4,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DRAWING_PALETTE.forEachIndexed { i, argb ->
            val on = argb == selected
            Box(
                Modifier.size(64.dp)
                    .border(BorderStroke(if (on) 3.dp else 1.dp, if (on) Pleos.Accent else Pleos.Divider), RoundedCornerShape(12.dp))
                    .padding(if (on) 5.dp else 3.dp)
                    .background(Color(argb), RoundedCornerShape(9.dp))
                    .border(1.dp, swatchEdge(Color(argb)), RoundedCornerShape(9.dp))
                    .clip(RoundedCornerShape(9.dp))
                    .clickable { onPick(argb) }
                    .semantics { contentDescription = PALETTE_NAMES[i] + if (on) " (고름)" else "" },
            )
        }
    }
}

@Composable
private fun Tool(label: String, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = Modifier.height(TOUCH),
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Pleos.Accent else Pleos.Divider),
    ) { Text(label) }
}

@Composable
private fun Choice(label: String, selected: Boolean, minWidth: Dp, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = Modifier.height(TOUCH).width(minWidth),
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Pleos.Accent else Pleos.Divider),
    ) { Text(label, color = if (selected) Pleos.Accent else Pleos.Text) }
}

/** 색 칸 테두리 색. 배경에 묻히는 색(다크의 검정, 라이트의 흰색)은 진한 회색 테두리로 칸을 보이게 한다. */
@Composable
private fun swatchEdge(c: Color): Color =
    if (needsStrongEdge(c.toArgb(), Pleos.Background.toArgb())) Pleos.TextSecondary else Pleos.Divider
