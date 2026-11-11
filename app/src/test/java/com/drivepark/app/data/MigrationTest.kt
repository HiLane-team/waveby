package com.drivepark.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.PersonLook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 버전 1(모양·외모 칸 없음) DB를 최신 버전으로 열어도 기존 데이터가 그대로 남는지. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val name = "migration-test.db"

    @Test
    fun migrate1to3_chain_v1DataSurvivesAndGetsDefaults() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(name)
        // 버전 1 스키마: Room이 버전 1에서 만들던 SQL 그대로(AppDatabase_Impl).
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `my_character` (`id` INTEGER NOT NULL, `body` TEXT NOT NULL, `face` TEXT NOT NULL, `hat` TEXT NOT NULL, `accessory` TEXT NOT NULL, `nickname` TEXT NOT NULL, `greeting` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `pending` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `body` TEXT NOT NULL, `face` TEXT NOT NULL, `hat` TEXT NOT NULL, `accessory` TEXT NOT NULL, `nickname` TEXT NOT NULL, `greeting` TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `friend` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isNew` INTEGER NOT NULL, `body` TEXT NOT NULL, `face` TEXT NOT NULL, `hat` TEXT NOT NULL, `accessory` TEXT NOT NULL, `nickname` TEXT NOT NULL, `greeting` TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collected_region` (`regionId` TEXT NOT NULL, PRIMARY KEY(`regionId`))")
            db.execSQL("INSERT INTO my_character VALUES (0, 'PURPLE', 'SMILE', 'NONE', 'SCARF', '늑대나', '안녕하세요')")
            db.execSQL("INSERT INTO pending (body, face, hat, accessory, nickname, greeting) VALUES ('RED', 'WINK', 'CAP', 'NONE', '대기', '곧 봐요')")
            db.execSQL("INSERT INTO friend (isNew, body, face, hat, accessory, nickname, greeting) VALUES (1, 'BLUE', 'SMILE', 'NONE', 'NONE', '친구', '반가워요')")
            db.execSQL("INSERT INTO collected_region VALUES ('jeju')")
            db.version = 1
        }

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).allowMainThreadQueries().build()
        val repo = RoomParkRepository(db)
        val me = repo.getMyCharacter()!!
        assertEquals("늑대나", me.nickname)
        assertEquals(CharacterKind.PERSON, me.kind)
        assertEquals("wolf", me.shape)
        assertEquals(PersonLook(), me.look)
        assertNull(me.drawing)
        val friends = repo.observeFriends().first()
        assertEquals(listOf("친구"), friends.map { it.character.nickname })
        assertTrue(friends.single().isNew)
        assertEquals(listOf("대기"), repo.deliverPending().map { it.nickname })
        assertEquals(setOf("jeju"), repo.observeCollectedRegionIds().first())
        db.close()
    }
}
