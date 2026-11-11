package com.drivepark.app.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.DRAWN_SHAPE
import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.ParkRepository
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.ui.drawing.DrawingBoard
import com.drivepark.app.ui.pixel.Shapes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 사람 모양 id. 반려동물은 고를 수 없다. */
const val PERSON_SHAPE = "person"

/**
 * 이 앱이 아는 모양이면 그대로, 모르면(다른 버전에서 만든 모양) 누구에 맞는 기본 모양.
 * Shapes.resolve와 같은 규칙: 나 → 사람, 반려동물 → 첫 동물.
 */
internal fun knownShape(id: String, kind: CharacterKind, animalShapes: List<String>): String = when {
    id == PERSON_SHAPE && kind == CharacterKind.PERSON -> id
    id in animalShapes -> id
    kind == CharacterKind.PET -> animalShapes.firstOrNull() ?: id
    else -> PERSON_SHAPE
}

/**
 * 새 캐릭터의 모습별 눈 기본색(모습에 어울리는 퍼스널 컬러): 사람 검정, 늑대 금색, 강아지 갈색, 고양이 파랑. 그 밖의 모습(내 그림 등)은 null = 지금 눈 색 그대로.
 * 저장된 캐릭터를 불러올 때는 쓰지 않는다(이미 고른 눈 색을 바꾸지 않는다).
 */
internal fun defaultEye(shape: String): EyeColor? = when (shape) {
    PERSON_SHAPE -> EyeColor.BLACK
    "wolf" -> EyeColor.GOLD
    "dog" -> EyeColor.BROWN
    "cat" -> EyeColor.BLUE
    else -> null
}

/** 모습을 바꿀 때 눈 색: 지금 눈이 이전 모습의 기본색(= 사용자가 안 고름)이면 새 모습의 기본색으로. */
private fun eyeAfterShapeChange(s: CreateCharacterUiState, newShape: String): CharacterParts {
    val next = defaultEye(newShape) ?: return s.parts
    return if (s.parts.eye == defaultEye(s.shape)) s.parts.copy(eye = next) else s.parts
}

data class CreateCharacterUiState(
    val parts: CharacterParts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE, EyeColor.BLACK),
    val nickname: String = "",
    val greeting: String = "",
    val kind: CharacterKind = CharacterKind.PERSON,
    val shape: String = PERSON_SHAPE,
    val look: PersonLook = PersonLook(),
    /** 사용자가 모습을 직접 골랐는지. 고른 뒤에는 "누구"를 바꿔도 모습을 자동으로 바꾸지 않는다(반려동물의 사람 모습 제외). */
    val shapeTouched: Boolean = false,
    /** 등록된 동물 모양 id(등록 순서). */
    val animalShapes: List<String> = listOf("wolf"),
    val error: String? = null,
    val saved: Boolean = false,
    /** 직접 그린 그림(완성한 것). 다른 모습을 골라도 남겨 두어 "내 그림"으로 돌아올 수 있다. */
    val drawing: String? = null,
    /** 그리는 중인 그림판. null이면 그리기 화면이 닫혀 있다. 앱이 켜져 있는 동안 남는다(주행 잠금 뒤에도). */
    val board: DrawingBoard? = null,
) {
    val hasDrawing: Boolean get() = Drawing.parse(drawing) != null

    /** 모습 줄에 보일 선택지: 나 → 사람 + 동물, 반려동물 → 동물만. 그림이 있으면 끝에 "내 그림". */
    val shapeChoices: List<String>
        get() = (if (kind == CharacterKind.PET) animalShapes else listOf(PERSON_SHAPE) + animalShapes) +
            (if (hasDrawing) listOf(DRAWN_SHAPE) else emptyList())

    /** 이름 칸 라벨. */
    val nameLabel: String get() = if (kind == CharacterKind.PET) "반려동물 이름" else "닉네임"

    val isPersonShape: Boolean get() = shape == PERSON_SHAPE

    /** 몸 색 줄: 기본 늑대 회색 + 색판 16색. 예전 5색을 쓰던 캐릭터는 그 색을 끝에 남겨 그대로 보이게 한다. */
    val bodyChoices: List<BodyColor>
        get() = if (parts.body in BodyColor.CHOICES) BodyColor.CHOICES else BodyColor.CHOICES + parts.body

    /** 지금 그림에 쓰인 글자. "내 그림"이 아니거나 그림이 없으면 빈 집합. */
    private val drawnLetters: Set<Char>
        get() = if (shape == DRAWN_SHAPE) Drawing.parse(drawing)?.rows?.flatMapTo(HashSet()) { it.toList() }.orEmpty() else emptySet()

    /** 머리색 줄: 사람 모습, 또는 머리(H) 붓을 쓴 내 그림. */
    val showHairRow: Boolean get() = isPersonShape || 'H' in drawnLetters

    /** 하의 줄: 사람 모습, 또는 하의(N) 붓을 쓴 내 그림. */
    val showBottomRow: Boolean get() = isPersonShape || 'N' in drawnLetters
}

class CreateCharacterViewModel(
    private val repo: ParkRepository,
    animalShapes: List<String> = listOf("wolf"),
) : ViewModel() {
    private val _state = MutableStateFlow(CreateCharacterUiState(animalShapes = animalShapes))
    val state: StateFlow<CreateCharacterUiState> = _state.asStateFlow()

    /** 저장된 캐릭터를 다 읽었는지. 읽는 중에 누른 선택은 [whileLoading]에 모았다가 불러온 값 위에 다시 적용한다. */
    private var loaded = false
    private val whileLoading = mutableListOf<(CreateCharacterUiState) -> CreateCharacterUiState>()

    init {
        // 이미 캐릭터가 있으면 수정 모드로 불러온다.
        viewModelScope.launch {
            repo.getMyCharacter()?.let { c ->
                _state.update {
                    it.copy(
                        parts = c.parts, nickname = c.nickname, greeting = c.greeting,
                        kind = c.kind, look = c.look, shapeTouched = true, drawing = c.drawing,
                        shape = if (c.shape == DRAWN_SHAPE && Drawing.parse(c.drawing) != null) DRAWN_SHAPE
                        else knownShape(c.shape, c.kind, it.animalShapes),
                    )
                }
            }
            whileLoading.forEach { _state.update(it) }
            whileLoading.clear()
            loaded = true
        }
    }

    /** 사용자가 누른 변경. 아직 불러오는 중이면 기억해 두었다가 불러온 뒤 다시 적용한다. */
    private fun edit(change: (CreateCharacterUiState) -> CreateCharacterUiState) {
        _state.update(change)
        if (!loaded) whileLoading += change
    }

    /**
     * 나/반려동물 바꾸기. 반려동물은 사람 모습을 쓸 수 없어 첫 동물로 바꾼다.
     * 나로 돌아올 때는 사용자가 모습을 직접 고르지 않았으면 사람 모습으로 되돌린다.
     */
    fun setKind(kind: CharacterKind) = edit { s ->
        val shape = when {
            kind == CharacterKind.PET && s.shape == PERSON_SHAPE -> s.animalShapes.firstOrNull() ?: s.shape
            kind == CharacterKind.PERSON && !s.shapeTouched -> PERSON_SHAPE
            else -> s.shape
        }
        s.copy(kind = kind, shape = shape, parts = eyeAfterShapeChange(s, shape))
    }

    fun setShape(id: String) = edit { it.copy(shape = id, shapeTouched = true, parts = eyeAfterShapeChange(it, id)) }
    fun setLook(look: PersonLook) = edit { it.copy(look = look) }
    fun setParts(parts: CharacterParts) = edit { it.copy(parts = parts) }
    // 입력하는 순간부터 최대 길이에서 멈춘다(저장할 때만 막으면 15자까지 들어가 보였다).
    fun setNickname(value: String) = edit { it.copy(nickname = value.take(Character.NICKNAME_MAX), error = null) }
    fun setGreeting(value: String) = edit { it.copy(greeting = value.take(Character.GREETING_MAX), error = null) }

    /**
     * 그리기 화면 열기. 그리던 판이 있으면 그대로, 완성한 그림이 있으면 그 그림,
     * 없으면 지금 고른 모습(사람·늑대)을 불러와 고치게 한다.
     */
    fun startDrawing(): DrawingBoard {
        val s = _state.value
        val board = s.board
            ?: if (s.hasDrawing) DrawingBoard.fromText(s.drawing)
            else Shapes.resolve(s.shape, s.kind).let { def -> DrawingBoard.fromStand(def.stand.rows, def.fixed) }
        edit { it.copy(board = board) }
        return board
    }

    fun updateBoard(board: DrawingBoard) = _state.update { it.copy(board = board) }

    /**
     * 완성: 그림을 담고 "내 그림"을 고른다. 실제 저장은 [save]에서 캐릭터와 함께.
     * 형식이 틀리거나 칠한 칸이 없으면(공원·주행 팝업에서 보이지 않는 캐릭터가 된다) 받지 않는다.
     */
    fun finishDrawing(text: String) {
        val d = Drawing.parse(text) ?: return
        if (d.rows.all { r -> r.all { it == '.' } }) return
        edit { it.copy(drawing = text, shape = DRAWN_SHAPE, shapeTouched = true, board = null, error = null) }
    }

    /** 돌아가기: 그리던 것을 버린다(완성한 그림은 그대로). */
    fun cancelDrawing() = _state.update { it.copy(board = null) }

    fun save() {
        val s = _state.value
        if (s.shape == DRAWN_SHAPE && !s.hasDrawing) {
            _state.update { it.copy(error = "그림을 먼저 그려 주세요(\"직접 그리기\")") }
            return
        }
        val character = Character.create(s.parts, s.nickname, s.greeting, s.kind, s.shape, s.look, s.drawing)
        if (character == null) {
            _state.update {
                it.copy(error = "${s.nameLabel}은 1~${Character.NICKNAME_MAX}자, 인사말은 1~${Character.GREETING_MAX}자로 적어주세요")
            }
            return
        }
        viewModelScope.launch {
            repo.saveMyCharacter(character)
            _state.update { it.copy(saved = true, error = null) }
        }
    }

    /** 저장 신호를 한 번 쓰고 지운다. 초기 설정에서 "이전"으로 돌아왔을 때 바로 다시 넘어가지 않게. */
    fun consumeSaved() = _state.update { it.copy(saved = false) }
}
