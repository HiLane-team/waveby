package com.drivepark.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 테스트용 설정 저장소. */
class InMemorySettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override fun observe(): Flow<AppSettings> = state
    override suspend fun current() = state.value
    override suspend fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
}
