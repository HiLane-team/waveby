package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdleParkBannerTest {
    private val state = MutableStateFlow(DriveState.PARKED)
    private val events = mutableListOf<String>()
    private val port = object : ParkBannerPort {
        override fun show(title: String, subtitle: String) { events += "show:$title|$subtitle" }
        override fun hide() { events += "hide" }
    }
    private val fifteen = 15_000L

    @Test
    fun subtitle_namesTheKeeperAndFriendCount() {
        assertEquals("공원에 친구들이 놀고 있어요", IdleParkBanner.TITLE)
        assertEquals("이호랑과 친구 5명이 기다려요", IdleParkBanner.subtitle(5))
        assertEquals("이호랑과 친구 1명이 기다려요", IdleParkBanner.subtitle(1))
        assertEquals("이호랑이 공원에서 기다려요", IdleParkBanner.subtitle(0))
    }

    @Test
    fun parked_showsThenHidesAfter15Seconds() = runTest {
        val banner = IdleParkBanner(state, port, fifteen)
        banner.show(backgroundScope, friendCount = 3); runCurrent()
        assertEquals(listOf("show:${IdleParkBanner.TITLE}|이호랑과 친구 3명이 기다려요"), events)
        advanceTimeBy(fifteen - 1); runCurrent()
        assertEquals(1, events.size)
        advanceTimeBy(2); runCurrent()
        assertEquals("hide", events.last())
    }

    @Test
    fun driving_neverShows_andDrivingOffHidesAtOnce() = runTest {
        val banner = IdleParkBanner(state, port, fifteen)
        state.value = DriveState.DRIVING
        banner.show(backgroundScope, friendCount = 3); runCurrent()
        assertEquals("주행 중에는 띄우지 않는다", emptyList<String>(), events)

        state.value = DriveState.PARKED
        banner.show(backgroundScope, friendCount = 3); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        state.value = DriveState.DRIVING; runCurrent()
        assertEquals("출발하면 바로 숨긴다", listOf("show", "hide"), events.map { it.substringBefore(':') })
    }

    @Test
    fun dismiss_hidesAndStopsTheTimer() = runTest {
        val banner = IdleParkBanner(state, port, fifteen)
        banner.show(backgroundScope, friendCount = 2); runCurrent()
        banner.dismiss(); runCurrent()
        advanceTimeBy(fifteen * 2); runCurrent()
        assertEquals("닫은 뒤 타이머로 또 숨기지 않는다", listOf("show", "hide"), events.map { it.substringBefore(':') })
    }
}
