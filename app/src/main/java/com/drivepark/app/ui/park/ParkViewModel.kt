package com.drivepark.app.ui.park

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.Friend
import com.drivepark.app.domain.Sticker
import com.drivepark.app.domain.ParkRepository
import com.drivepark.app.domain.Region
import com.drivepark.app.domain.Regions
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
    /** 모은 지역(알려진 지역만, Regions.ALL 순서). 공원에 놀러 올 지역 캐릭터 후보. */
    val collectedRegions: List<Region> = emptyList(),
)

class ParkViewModel(private val repo: ParkRepository) : ViewModel() {
    val state: StateFlow<ParkUiState> =
        combine(repo.observeMyCharacter(), repo.observeFriends(), repo.observeCollectedRegionIds()) { me, friends, regionIds ->
            ParkUiState(
                me = me,
                wandering = friends.take(MAX_WANDERING), // 이미 최신순
                newFriends = friends.filter { it.isNew },
                seenFriends = friends.filterNot { it.isNew },
                collectedRegions = Regions.ALL.filter { it.id in regionIds },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ParkUiState())

    /** 친구에게 남길 스티커를 고른다. 이미 고른 스티커를 다시 누르면 지운다. */
    fun toggleSticker(friendId: Long, sticker: Sticker) {
        val current = state.value.let { it.newFriends + it.seenFriends }.firstOrNull { it.id == friendId }?.sticker
        viewModelScope.launch { repo.setSticker(friendId, if (current == sticker) null else sticker) }
    }

    fun markAllSeen() {
        viewModelScope.launch { repo.markAllSeen() }
    }

    companion object {
        const val MAX_WANDERING = 10
    }
}
