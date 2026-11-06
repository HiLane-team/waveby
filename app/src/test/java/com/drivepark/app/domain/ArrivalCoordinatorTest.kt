package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLocation(var at: LatLng?) : LocationProvider {
    override suspend fun currentLocation() = at
}

class RecordingGreeter(private val fail: Boolean = false) : Greeter {
    val spoken = mutableListOf<String>()
    override fun speak(text: String) {
        spoken += text
        if (fail) throw IllegalStateException("tts down")
    }
}

/** 응답, 지연, 예외를 정할 수 있는 브리퍼. */
class FakeBriefer(
    private val reply: String? = null,
    private val delayMillis: Long = 0,
    private val fail: Boolean = false,
) : Briefer {
    var calls = 0
    override suspend fun brief(summary: ArrivalSummary): String? {
        calls++
        if (delayMillis > 0) delay(delayMillis)
        if (fail) throw IllegalStateException("gleo down")
        return reply
    }
}

val SEOUL = LatLng(37.57, 126.98)
val HOENGSEONG = LatLng(37.49, 127.98)

fun testCharacter(name: String) =
    Character(CharacterParts(BodyColor.GREEN, Face.WINK, Hat.NONE, Accessory.NONE), name, "반가워요!")

fun testCoordinator(
    repo: ParkRepository,
    location: LocationProvider = FakeLocation(SEOUL),
    greeter: Greeter = RecordingGreeter(),
    briefer: Briefer = FakeBriefer(),
    settings: SettingsRepository = InMemorySettingsRepository(),
) = ArrivalCoordinator(repo, location, RegionResolver(), greeter, briefer, settings)

@OptIn(ExperimentalCoroutinesApi::class)
class ArrivalCoordinatorTest {
    private val repo = InMemoryParkRepository()
    private val location = FakeLocation(SEOUL)
    private val greeter = RecordingGreeter()

    private fun coordinator(
        briefer: Briefer = FakeBriefer(),
        settings: SettingsRepository = InMemorySettingsRepository(),
        greeter: Greeter = this.greeter,
    ) = testCoordinator(repo, location, greeter, briefer, settings)

    @Test
    fun deliversPendingFriends() = runTest {
        repo.addPending(listOf(testCharacter("가"), testCharacter("나")))
        val s = coordinator().onParked()
        assertEquals(2, s.friends.size)
        assertEquals(listOf("이번 드라이브에서 2명이 놀러왔어요\n가: 반가워요!"), greeter.spoken)
    }

    @Test
    fun emptyArrival_isEmpty_andNoGreeting() = runTest {
        val briefer = FakeBriefer("안녕")
        val s = coordinator(briefer).onParked()
        assertTrue(s.isEmpty)
        assertTrue(greeter.spoken.isEmpty())
        assertEquals(0, briefer.calls)
    }

    @Test
    fun newRegion_collectedOnce() = runTest {
        location.at = HOENGSEONG
        val c = coordinator()
        assertEquals("hoengseong", c.onParked().newRegion?.id)
        assertNull(c.onParked().newRegion)
    }

    @Test
    fun locationNull_skipsRegionButDeliversFriends() = runTest {
        location.at = null
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator().onParked()
        assertNull(s.newRegion)
        assertEquals(1, s.friends.size)
    }

    @Test
    fun arrival_withoutMyCharacter_stillDelivers() = runTest {
        assertNull(repo.getMyCharacter())
        repo.addPending(listOf(testCharacter("가")))
        assertEquals(1, coordinator().onParked().friends.size)
    }

    @Test
    fun greeterThrows_arrivalStillCompletes() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        assertEquals(1, coordinator(greeter = RecordingGreeter(fail = true)).onParked().friends.size)
        assertTrue(repo.pending.isEmpty())
    }

    @Test
    fun briefer_success_isSpoken() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator(FakeBriefer("어서 오세요!")).onParked()
        assertEquals(listOf("어서 오세요!"), greeter.spoken)
        assertEquals("어서 오세요!", s.briefing)
    }

    @Test
    fun briefer_null_usesFallback() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator(FakeBriefer(null)).onParked()
        assertEquals(fallbackBriefing(s), greeter.spoken.single())
    }

    @Test
    fun briefer_over80chars_usesFallback() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator(FakeBriefer("가".repeat(81))).onParked()
        assertEquals(fallbackBriefing(s), greeter.spoken.single())
    }

    @Test
    fun briefer_throws_usesFallback_andCompletes() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator(FakeBriefer(fail = true)).onParked()
        assertEquals(fallbackBriefing(s), greeter.spoken.single())
        assertTrue(repo.pending.isEmpty())
    }

    @Test
    fun slowBriefer_fallsBackOnce() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator(FakeBriefer("늦은 인사", delayMillis = 5_000)).onParked()
        testScheduler.advanceTimeBy(10_000)
        assertEquals(listOf(fallbackBriefing(s)), greeter.spoken)
    }

    @Test
    fun gleoOff_skipsBriefer() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val briefer = FakeBriefer("글레오")
        val s = coordinator(briefer, InMemorySettingsRepository(AppSettings(gleoBriefing = false))).onParked()
        assertEquals(0, briefer.calls)
        assertEquals(fallbackBriefing(s), greeter.spoken.single())
    }

    @Test
    fun voiceOff_noSpeak_butBriefingSet() = runTest {
        repo.addPending(listOf(testCharacter("가")))
        val s = coordinator(settings = InMemorySettingsRepository(AppSettings(greetingVoice = false))).onParked()
        assertTrue(greeter.spoken.isEmpty())
        assertEquals(fallbackBriefing(s), s.briefing)
    }
}
