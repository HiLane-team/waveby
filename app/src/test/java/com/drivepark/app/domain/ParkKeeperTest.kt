package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkKeeperTest {

    @Test
    fun keeper_isIhorangFinalDesign() {
        // 대표가 2026-11-08 직접 꾸며 저장한 이호랑(내 그림·기본 회색·초록 눈·빨간 목도리·"안녕하세요.").
        val c = ParkKeeper.CHARACTER
        assertEquals("이호랑", c.nickname)
        assertEquals("안녕하세요.", c.greeting)
        assertEquals(DRAWN_SHAPE, c.shape)
        assertEquals(BodyColor.PURPLE, c.parts.body)
        assertEquals(EyeColor.GREEN, c.parts.eye)
        assertEquals(Accessory.SCARF, c.parts.accessory)
        assertEquals(Hat.NONE, c.parts.hat)
        val d = requireNotNull(Drawing.parse(c.drawing)) { "그림 형식" }
        assertEquals(GridSize.BASIC, d.size)
        assertEquals(".KK.......KK.....", d.rows.first())
        assertEquals(".KWWWKWWWW2WWLBK.", d.rows[12]) // 코 자리와 분홍 볼
        assertEquals(mapOf('2' to 0xFFF2A7B5.toInt()), d.colors)
    }

    @Test
    fun guides_untilTutorialDoneOrThreeFriends() {
        assertEquals(KeeperMode.GUIDE, ParkKeeper.mode(friendCount = 0, tutorialDone = false, tutorialStarted = true))
        assertEquals(KeeperMode.GUIDE, ParkKeeper.mode(friendCount = 2, tutorialDone = false, tutorialStarted = true))
        assertEquals(KeeperMode.PLAY, ParkKeeper.mode(friendCount = 3, tutorialDone = false))
        assertEquals(KeeperMode.PLAY, ParkKeeper.mode(friendCount = 0, tutorialDone = true))
    }

    @Test
    fun tutorial_advancesOneStepPerTap_thenEnds() {
        assertEquals(1, ParkKeeper.nextTutorialStep(0))
        assertNull(ParkKeeper.nextTutorialStep(ParkKeeper.TUTORIAL.lastIndex))
    }

    @Test
    fun help_opensCyclesThenCloses() {
        assertEquals(0, ParkKeeper.nextHelp(null))
        assertEquals(1, ParkKeeper.nextHelp(0))
        assertNull(ParkKeeper.nextHelp(ParkKeeper.HELP.lastIndex))
    }

    @Test
    fun lines_fitTheirBoxes() {
        // 처음 안내는 큰 대화 상자(두 줄까지), 도움말은 작은 말풍선(한 줄).
        ParkKeeper.TUTORIAL.forEach { assertTrue(it, it.length <= 44) }
        ParkKeeper.HELP.forEach { assertTrue(it, it.length <= 30) }
        assertTrue(ParkKeeper.TUTORIAL.size in 3..7)
        assertTrue(ParkKeeper.TUTORIAL.first().startsWith("안녕? 난 이호랑이야."))
    }

    @Test
    fun invites_untilKeeperTapped_thenGuides() {
        // 처음엔 공원에서 "나 좀 눌러 봐!"로 유도하고, 이호랑을 눌러야 안내가 시작된다.
        assertEquals(KeeperMode.INVITE, ParkKeeper.mode(friendCount = 0, tutorialDone = false, tutorialStarted = false))
        assertEquals(KeeperMode.GUIDE, ParkKeeper.mode(friendCount = 0, tutorialDone = false, tutorialStarted = true))
        assertEquals(KeeperMode.PLAY, ParkKeeper.mode(friendCount = 3, tutorialDone = false, tutorialStarted = false))
        assertEquals(KeeperMode.PLAY, ParkKeeper.mode(friendCount = 0, tutorialDone = true, tutorialStarted = false))
        assertEquals("나 좀 눌러 봐!", ParkKeeper.INVITE_LINE)
    }
}
