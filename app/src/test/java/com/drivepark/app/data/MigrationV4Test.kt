package com.drivepark.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 버전 3(스티커 칸 없음) DB를 버전 4로 열어도 친구와 그림이 그대로이고 스티커는 비어 있는지. */
@RunWith(RobolectricTestRunner::class)
class MigrationV4Test {
    private val name = "migration-v4-test.db"

    @Test
    fun migrate3to4_keepsFriendsAndAddsNullSticker() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(name)
        val cols = "`body` TEXT NOT NULL, `face` TEXT NOT NULL, `hat` TEXT NOT NULL, `accessory` TEXT NOT NULL, `nickname` TEXT NOT NULL, `greeting` TEXT NOT NULL, " +
            "`kind` TEXT NOT NULL DEFAULT 'PERSON', `shape` TEXT NOT NULL DEFAULT 'wolf', `skin` TEXT NOT NULL DEFAULT 'LIGHT', `hair` TEXT NOT NULL DEFAULT 'BLACK', `bottom` TEXT NOT NULL DEFAULT 'NAVY', `drawing` TEXT"
        // 버전 3 스키마: Room이 버전 3에서 만들던 SQL 그대로(AppDatabase_Impl).
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `my_character` (`id` INTEGER NOT NULL, $cols, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `pending` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, $cols)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `friend` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isNew` INTEGER NOT NULL, $cols)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collected_region` (`regionId` TEXT NOT NULL, PRIMARY KEY(`regionId`))")
            db.execSQL("INSERT INTO my_character (id, body, face, hat, accessory, nickname, greeting, shape, drawing) VALUES (0, 'BLUE', 'SMILE', 'NONE', 'NONE', '나', '안녕', 'wolf', NULL)")
            db.execSQL("INSERT INTO friend (isNew, body, face, hat, accessory, nickname, greeting) VALUES (1, 'RED', 'WINK', 'CAP', 'NONE', '친구', '반가워요')")
            db.version = 3
        }

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .allowMainThreadQueries().build()
        val repo = RoomParkRepository(db)
        assertEquals("나", repo.getMyCharacter()!!.nickname)
        val friend = repo.observeFriends().first().single()
        assertEquals("친구", friend.character.nickname)
        assertNull(friend.sticker)
        db.close()
    }
}
