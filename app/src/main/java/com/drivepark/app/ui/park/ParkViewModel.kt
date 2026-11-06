package com.drivepark.app.ui.park

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.Friend
import com.drivepark.app.domain.ParkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ParkUiState(
    val me: Character? = null,
    val wandering: List<Friend> = emptyList(),
    val newFriends: List<Friend> = emptyList(),
    val seenFriends: List<Friend> = emptyList(),
)

class ParkViewModel(private val repo: ParkRepository) : ViewModel() {
    val state: StateFlow<ParkUiState> =
        combine(repo.observeMyCharacter(), repo.observeFriends()) { me, friends ->
            ParkUiState(
                me = me,
                wandering = friends.take(MAX_WANDERING), // 이미 최신순
                newFriends = friends.filter { it.isNew },
                seenFriends = friends.filterNot { it.isNew },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ParkUiState())

    fun markAllSeen() {
        viewModelScope.launch { repo.markAllSeen() }
    }

    companion object {
        const val MAX_WANDERING = 10
    }
}
