package com.drivepark.app.domain

import kotlinx.coroutines.withTimeoutOrNull

data class ArrivalSummary(
    val friends: List<Character>,
    val newRegion: Region?,
    /** 도착 처리에서 읽은(또는 음성이 꺼져 있으면 보여줄) 브리핑 문장. */
    val briefing: String? = null,
) {
    val isEmpty: Boolean get() = friends.isEmpty() && newRegion == null
}

/** 주차 시 대기함 전달, 지역 획득, 도착 브리핑을 묶어 처리한다. */
class ArrivalCoordinator(
    private val repo: ParkRepository,
    private val location: LocationProvider,
    private val resolver: RegionResolver,
    private val greeter: Greeter,
    private val briefer: Briefer,
    private val settings: SettingsRepository,
    private val briefTimeoutMillis: Long = 3_000,
) {
    suspend fun onParked(): ArrivalSummary {
        val friends = repo.deliverPending()
        val region = location.currentLocation()?.let(resolver::resolve)
        val newRegion = region?.takeIf { repo.collectRegion(it.id) }
        val base = ArrivalSummary(friends, newRegion)
        if (base.isEmpty) return base

        val prefs = settings.current()
        val text = (if (prefs.gleoBriefing) gleo(base) else null) ?: fallbackBriefing(base)
        if (prefs.greetingVoice) runCatching { greeter.speak(text) }
        return base.copy(briefing = text)
    }

    private suspend fun gleo(summary: ArrivalSummary): String? =
        withTimeoutOrNull(briefTimeoutMillis) { runCatching { briefer.brief(summary) }.getOrNull() }
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_BRIEFING_LENGTH }
}
