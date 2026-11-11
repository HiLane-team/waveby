package com.drivepark.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.drivepark.app.domain.EyeColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 버전 4 DB를 버전 5로 열어도 캐릭터·대기함·친구·스티커·그림이 그대로이고 눈 색은 초록인지. */
@RunWith(RobolectricTestRunner::class)
class MigrationV5Test {
    private val name = "migration-v5-test.db"

    @Test
    fun migrate4to5_keepsEverythingAndEyeIsGreen() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(name)
        val cols = "`body` TEXT NOT NULL, `face` TEXT NOT NULL, `hat` TEXT NOT NULL, `accessory` TEXT NOT NULL, `nickname` TEXT NOT NULL, `greeting` TEXT NOT NULL, " +
            "`kind` TEXT NOT NULL DEFAULT 'PERSON', `shape` TEXT NOT NULL DEFAULT 'wolf', `skin` TEXT NOT NULL DEFAULT 'LIGHT', `hair` TEXT NOT NULL DEFAULT 'BLACK', `bottom` TEXT NOT NULL DEFAULT 'NAVY', `drawing` TEXT"
        val drawing = List(23) { ".".repeat(17) }.joinToString("\n")
        // 버전 4 스키마: 버전 3 + friend.sticker.
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `my_character` (`id` INTEGER NOT NULL, $cols, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `pending` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, $cols)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `friend` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isNew` INTEGER NOT NULL, $cols, `sticker` TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collected_region` (`regionId` TEXT NOT NULL, PRIMARY KEY(`regionId`))")
            db.execSQL("INSERT INTO my_character (id, body, face, hat, accessory, nickname, greeting, shape, drawing) VALUES (0, 'BLUE', 'SMILE', 'NONE', 'NONE', '나', '안녕', 'drawn', '$drawing')")
            db.execSQL("INSERT INTO pending (body, face, hat, accessory, nickname, greeting) VALUES ('GREEN', 'SMILE', 'NONE', 'NONE', '대기', '하이')")
            db.execSQL("INSERT INTO friend (isNew, body, face, hat, accessory, nickname, greeting, sticker) VALUES (1, 'RED', 'WINK', 'CAP', 'NONE', '친구', '반가워요', 'HEART')")
            db.version = 4
        }

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .allowMainThreadQueries().build()
        val repo = RoomParkRepository(db)
        val me = repo.getMyCharacter()!!
        assertEquals("나", me.nickname)
        assertEquals(drawing, me.drawing)
        assertEquals(EyeColor.GREEN, me.parts.eye)
        val pending = db.parkDao().getPending().single().character.toDomain()
        assertEquals("대기", pending.nickname)
        assertEquals(EyeColor.GREEN, pending.parts.eye)
        val friend = repo.observeFriends().first().single()
        assertEquals("친구", friend.character.nickname)
        assertEquals(EyeColor.GREEN, friend.character.parts.eye)
        assertEquals("HEART", friend.sticker?.name)
        db.close()
    }
}
