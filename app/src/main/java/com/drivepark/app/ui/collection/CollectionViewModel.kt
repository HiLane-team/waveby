package com.drivepark.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.ParkRepository
import com.drivepark.app.domain.Region
import com.drivepark.app.domain.Regions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CollectionUiState(
    val items: List<Pair<Region, Boolean>> = Regions.ALL.map { it to false },
    val locationGranted: Boolean = true,
)

class CollectionViewModel(
    repo: ParkRepository,
    private val hasLocationPermission: () -> Boolean,
) : ViewModel() {
    private val granted = MutableStateFlow(hasLocationPermission())

    val state: StateFlow<CollectionUiState> =
        combine(repo.observeCollectedRegionIds(), granted) { ids, ok ->
            CollectionUiState(Regions.ALL.map { it to (it.id in ids) }, ok)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, CollectionUiState(locationGranted = granted.value))

    /** 권한 요청 결과가 돌아오거나 화면으로 돌아왔을 때 다시 확인한다. */
    fun refreshPermission() {
        granted.value = hasLocationPermission()
    }
}
