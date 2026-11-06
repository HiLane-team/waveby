package com.drivepark.app.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.ParkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateCharacterUiState(
    val parts: CharacterParts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE), // 기본: 늑대
    val nickname: String = "",
    val greeting: String = "",
    val error: String? = null,
    val saved: Boolean = false,
)

class CreateCharacterViewModel(private val repo: ParkRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateCharacterUiState())
    val state: StateFlow<CreateCharacterUiState> = _state.asStateFlow()

    init {
        // 이미 캐릭터가 있으면 수정 모드로 불러온다.
        viewModelScope.launch {
            repo.getMyCharacter()?.let { c ->
                _state.update { it.copy(parts = c.parts, nickname = c.nickname, greeting = c.greeting) }
            }
        }
    }

    fun setParts(parts: CharacterParts) = _state.update { it.copy(parts = parts) }
    // 입력하는 순간부터 최대 길이에서 멈춘다(저장할 때만 막으면 15자까지 들어가 보였다).
    fun setNickname(value: String) = _state.update { it.copy(nickname = value.take(Character.NICKNAME_MAX), error = null) }
    fun setGreeting(value: String) = _state.update { it.copy(greeting = value.take(Character.GREETING_MAX), error = null) }

    fun save() {
        val s = _state.value
        val character = Character.create(s.parts, s.nickname, s.greeting)
        if (character == null) {
            _state.update {
                it.copy(error = "닉네임은 1~${Character.NICKNAME_MAX}자, 인사말은 1~${Character.GREETING_MAX}자로 적어주세요")
            }
            return
        }
        viewModelScope.launch {
            repo.saveMyCharacter(character)
            _state.update { it.copy(saved = true, error = null) }
        }
    }
}
