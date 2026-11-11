package com.drivepark.app.ui.park

import com.drivepark.app.ui.pixel.Pose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ParkMotionTest {
    @Test
    fun blinkDelay_isBetween3and4Seconds() {
        val rnd = Random(7)
        repeat(1000) { assertTrue(nextBlinkDelay(rnd) in 3000L..4000L) }
        assertEquals(120L, BLINK_MS)
    }

    @Test
    fun hop_crouchAirCrouchThenDone() {
        assertEquals(HopFrame(Pose.CROUCH, 0f), hopAt(0))
        assertEquals(HopFrame(Pose.CROUCH, 0f), hopAt(79))
        val top = hopAt(240)!!
        assertEquals(Pose.AIR, top.pose)
        assertEquals(3f, top.liftDots, 0.01f)
        assertEquals(Pose.AIR, hopAt(80)!!.pose)
        assertEquals(Pose.CROUCH, hopAt(420)!!.pose)
        assertNull(hopAt(480))
        assertEquals(480L, HOP_MS)
    }

    @Test
    fun pickVisitors_rules() {
        val all = { _: String -> true }
        assertEquals(emptyList<String>(), pickVisitors(emptyList(), Random(1), hasMascot = all))
        assertEquals(listOf("jeju"), pickVisitors(listOf("jeju"), Random(1), hasMascot = all))
        val five = listOf("hoengseong", "boseong", "jeju", "gangneung", "yeongdeok")
        val two = pickVisitors(five, Random(3), hasMascot = all)
        assertEquals(2, two.size)
        assertEquals(2, two.toSet().size)
        assertTrue(five.containsAll(two))
        assertEquals(emptyList<String>(), pickVisitors(five, Random(1), max = 0, hasMascot = all))
    }

    @Test
    fun pickVisitors_onlyRegionsWithANewDesign() {
        // 새 그림이 없는 지역 캐릭터는 오지 않는다(지금은 5종 모두 등록돼 있다).
        assertEquals(emptyList<String>(), pickVisitors(listOf("jeju", "boseong"), Random(1), hasMascot = { false }))
        assertEquals(2, pickVisitors(listOf("jeju", "boseong"), Random(1)).size)
        assertEquals(listOf("jeju"), pickVisitors(listOf("jeju", "boseong"), Random(1), hasMascot = { it == "jeju" }))
    }

    @Test
    fun visitorKeys_doNotCollideWithFriendsOrMe() {
        com.drivepark.app.domain.Regions.ALL.forEach { r ->
            val k = visitorKey(r.id)
            assertTrue(k != "me" && !Regex("f\\d+").matches(k))
        }
    }

    @Test
    fun doze_frameInterval() {
        assertEquals(1500L, DOZE_FRAME_MS)
    }

    @Test
    fun newFriend_startsAtTheLeftEdge() {
        // 새로 놀러온 친구는 공원 왼쪽 끝에서 시작해 첫 걸음으로 걸어 들어온다(밖에서 시작하면 메뉴 바와 겹친다).
        assertEquals(0f, startX(enterFromEdge = true, random = kotlin.random.Random(1), maxX = 500f), 0f)
        val x = startX(enterFromEdge = false, random = kotlin.random.Random(1), maxX = 500f)
        assertTrue(x in 0f..500f)
    }

    @Test
    fun inviteHop_hopsTwiceThenRests_andRepeats() {
        // 폴짝폴짝(두 번 연달아) → 쉬기, [INVITE_CYCLE_MS]마다 반복.
        assertEquals(hopAt(0), inviteHopAt(0))
        assertEquals(hopAt(240), inviteHopAt(240))
        assertEquals(hopAt(0), inviteHopAt(INVITE_SECOND_HOP_MS))
        assertEquals(hopAt(240), inviteHopAt(INVITE_SECOND_HOP_MS + 240))
        assertNull(inviteHopAt(INVITE_SECOND_HOP_MS + HOP_MS))
        assertNull(inviteHopAt(INVITE_CYCLE_MS - 1))
        assertEquals(hopAt(240), inviteHopAt(INVITE_CYCLE_MS + 240))
        assertTrue(INVITE_SECOND_HOP_MS >= HOP_MS)
        assertTrue(INVITE_CYCLE_MS > INVITE_SECOND_HOP_MS + HOP_MS)
    }
}
