package com.drivepark.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomParkRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomParkRepository

    private fun c(name: String, body: BodyColor = BodyColor.RED) =
        Character(CharacterParts(body, Face.SMILE, Hat.CAP, Accessory.SCARF), name, "안녕!")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = RoomParkRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun myCharacter_roundTrips() = runTest {
        assertNull(repo.getMyCharacter())
        repo.saveMyCharacter(c("나"))
        assertEquals(c("나"), repo.getMyCharacter())
        assertEquals(c("나"), repo.observeMyCharacter().first())
        repo.saveMyCharacter(c("새나", BodyColor.BLUE))
        assertEquals(c("새나", BodyColor.BLUE), repo.getMyCharacter())
    }

    @Test
    fun deliverPending_movesAllAtomically_andEmptiesPending() = runTest {
        repo.addPending(listOf(c("a"), c("b"), c("c")))
        assertEquals(listOf(c("a"), c("b"), c("c")), repo.deliverPending())
        assertTrue(repo.deliverPending().isEmpty())
        val friends = repo.observeFriends().first()
        assertEquals(3, friends.size)
        assertTrue(friends.all { it.isNew })
    }

    @Test
    fun friends_newestFirst() = runTest {
        repo.addPending(listOf(c("a"), c("b")))
        repo.deliverPending()
        repo.addPending(listOf(c("x"), c("y")))
        repo.deliverPending()
        assertEquals(listOf("y", "x", "b", "a"), repo.observeFriends().first().map { it.character.nickname })
    }

    @Test
    fun markAllSeen_clearsNewFlag() = runTest {
        repo.addPending(listOf(c("a"), c("b")))
        repo.deliverPending()
        repo.markAllSeen()
        assertTrue(repo.observeFriends().first().none { it.isNew })
    }

    @Test
    fun resetAll_keepsMyCharacter() = runTest {
        repo.saveMyCharacter(c("나"))
        repo.addPending(listOf(c("a"), c("b")))
        repo.deliverPending()
        repo.addPending(listOf(c("p1"), c("p2")))
        repo.collectRegion("jeju")
        repo.resetAll()
        assertEquals(c("나"), repo.getMyCharacter())
        assertTrue(repo.deliverPending().isEmpty())
        assertTrue(repo.observeFriends().first().isEmpty())
        assertTrue(repo.observeCollectedRegionIds().first().isEmpty())
    }

    @Test
    fun collectRegion_onlyFirstTimeTrue() = runTest {
        assertTrue(repo.collectRegion("jeju"))
        assertFalse(repo.collectRegion("jeju"))
        assertEquals(setOf("jeju"), repo.observeCollectedRegionIds().first())
    }
}
