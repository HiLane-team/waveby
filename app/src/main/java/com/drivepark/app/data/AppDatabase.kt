package com.drivepark.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MyCharacterEntity::class, PendingEntity::class, FriendEntity::class, CollectedRegionEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parkDao(): ParkDao

    companion object {
        /**
         * 버전 1 → 2: 캐릭터를 담는 세 표에 누구·모양·사람 외모 칸을 더한다.
         * 기존 캐릭터는 "나·늑대·기본 외모"가 되어 업데이트 후에도 그대로 보인다. 데이터는 지우지 않는다.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = listOf(
                    "kind" to "PERSON", "shape" to "wolf", "skin" to "LIGHT", "hair" to "BLACK", "bottom" to "NAVY",
                )
                for (table in listOf("my_character", "pending", "friend")) {
                    for ((col, def) in columns) {
                        db.execSQL("ALTER TABLE `$table` ADD COLUMN `$col` TEXT NOT NULL DEFAULT '$def'")
                    }
                }
            }
        }

        /**
         * 버전 2 → 3: 캐릭터를 담는 세 표에 직접 그린 그림 칸(비어 있어도 됨)을 더한다.
         * 기존 캐릭터는 그림 없이 그대로 남는다. 친구·대기함 칸은 나중에 다른 차의 그림을 받을 자리다.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("my_character", "pending", "friend")) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `drawing` TEXT")
                }
            }
        }

        /** 버전 3 → 4: 친구 표에 답례 스티커 칸(비어 있어도 됨)을 더한다. 기존 친구는 스티커 없이 그대로. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `friend` ADD COLUMN `sticker` TEXT")
            }
        }

        /** 버전 4 → 5: 캐릭터 세 표에 눈 색 칸을 더한다. 기존 캐릭터·친구는 초록 눈(예전 색). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("my_character", "pending", "friend")) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `eye` TEXT NOT NULL DEFAULT 'GREEN'")
                }
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "drivepark.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
