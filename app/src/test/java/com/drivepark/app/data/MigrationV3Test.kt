package com.drivepark.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.drivepark.app.domain.CharacterKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 버전 2(그림 칸 없음) DB를 버전 3으로 열어도 캐릭터·친구가 그대로이고 그림은 비어 있는지. */
@RunWith(RobolectricTestRunner::class)
class MigrationV3Test {
    private val name = "migration-v3-test.db"

    @Test
    fun migrate2to3_keepsRowsAndAddsNullDrawing() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase(name)
        val extra = "`kind` TEXT NOT NULL DEFAULT 'PERSON', `shape` TEXT NOT NULL DEFAULT 'wolf', `skin` TEXT NOT NULL DEFAULT 'LIGHT', `hair` TEXT NOT NULL DEFAULT 'BLACK', `bottom` TEXT NOT NULL DEFAULT 'NAVY'"
        val base = "`body` TEXT NOT NULL, `face` TEXT NOT NULL, `hat` TEXT NOT NULL, `accessory` TEXT NOT NULL, `nickname` TEXT NOT NULL, `greeting` TEXT NOT NULL"
        // 버전 2 스키마: Room이 버전 2에서 만들던 SQL 그대로(AppDatabase_Impl).
        SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `my_character` (`id` INTEGER NOT NULL, $base, $extra, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `pending` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, $base, $extra)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `friend` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isNew` INTEGER NOT NULL, $base, $extra)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `collected_region` (`regionId` TEXT NOT NULL, PRIMARY KEY(`regionId`))")
            db.execSQL("INSERT INTO my_character VALUES (0, 'BLUE', 'SMILE', 'NONE', 'NONE', '사람나', '반가워요', 'PERSON', 'person', 'DEEP', 'BROWN', 'DENIM')")
            db.execSQL("INSERT INTO pending (body, face, hat, accessory, nickname, greeting, kind, shape, skin, hair, bottom) VALUES ('RED', 'WINK', 'CAP', 'NONE', '대기', '곧 봐요', 'PET', 'wolf', 'LIGHT', 'BLACK', 'NAVY')")
            db.execSQL("INSERT INTO friend (isNew, body, face, hat, accessory, nickname, greeting, kind, shape, skin, hair, bottom) VALUES (0, 'PURPLE', 'SMILE', 'NONE', 'SCARF', '늑대친구', '안녕', 'PET', 'wolf', 'LIGHT', 'BLACK', 'NAVY')")
            db.version = 2
        }

        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5).allowMainThreadQueries().build()
        val repo = RoomParkRepository(db)
        val me = repo.getMyCharacter()!!
        assertEquals("사람나", me.nickname)
        assertEquals("person", me.shape)
        assertEquals("DENIM", me.look.bottom.name)
        assertNull(me.drawing)
        val friend = repo.observeFriends().first().single().character
        assertEquals("늑대친구", friend.nickname)
        assertEquals(CharacterKind.PET, friend.kind)
        assertNull(friend.drawing)
        val pending = repo.deliverPending().single()
        assertEquals("대기", pending.nickname)
        assertNull(pending.drawing)
        db.close()
    }
}
