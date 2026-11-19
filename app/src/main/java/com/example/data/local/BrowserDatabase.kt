package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        SiteRuleEntity::class,
        BlockedDomainStatEntity::class,
        SitePermissionEntity::class,
        BrowserExtensionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `site_permissions` (
                        `domain` TEXT NOT NULL,
                        `camera` TEXT NOT NULL,
                        `microphone` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`domain`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `saved_media`")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extensions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `matchPatterns` TEXT NOT NULL,
                        `jsCode` TEXT NOT NULL,
                        `cssCode` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "aegis_browser.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val defaultBookmarks: List<BookmarkEntity> = listOf(
            BookmarkEntity(
                title = "Wikipedia",
                url = "https://ja.wikipedia.org/",
                accentIndex = 0,
                createdAt = 6000L
            ),
            BookmarkEntity(
                title = "NHK NEWS",
                url = "https://www3.nhk.or.jp/news/",
                accentIndex = 1,
                createdAt = 5000L
            ),
            BookmarkEntity(
                title = "Yahoo! JAPAN",
                url = "https://www.yahoo.co.jp/",
                accentIndex = 2,
                createdAt = 4000L
            ),
            BookmarkEntity(
                title = "DuckDuckGo",
                url = "https://duckduckgo.com/",
                accentIndex = 3,
                createdAt = 3000L
            ),
            BookmarkEntity(
                title = "GitHub",
                url = "https://github.com/",
                accentIndex = 4,
                createdAt = 2000L
            ),
            BookmarkEntity(
                title = "MDN Docs",
                url = "https://developer.mozilla.org/ja/",
                accentIndex = 5,
                createdAt = 1000L
            )
        )
    }
}
