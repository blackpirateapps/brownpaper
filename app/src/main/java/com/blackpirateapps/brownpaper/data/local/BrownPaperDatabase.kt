package com.blackpirateapps.brownpaper.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ArticleEntity::class,
        ArticleFtsEntity::class,
        FolderEntity::class,
        TagEntity::class,
        ArticleTagCrossRef::class,
        PendingSyncOperationEntity::class,
        PendingWallabagDeleteOperationEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class BrownPaperDatabase : RoomDatabase() {
    abstract fun brownPaperDao(): BrownPaperDao

    companion object {
        val Migration2To3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN wallabagEntryId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE articles ADD COLUMN remoteUpdatedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE articles ADD COLUMN lastSyncedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE articles ADD COLUMN localModifiedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE articles SET localModifiedAt = dateAdded WHERE localModifiedAt = 0")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_articles_wallabagEntryId
                    ON articles(wallabagEntryId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wallabag_sync_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        articleId INTEGER NOT NULL,
                        operationType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        attemptCount INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT,
                        FOREIGN KEY(articleId) REFERENCES articles(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_wallabag_sync_operations_articleId
                    ON wallabag_sync_operations(articleId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_wallabag_sync_operations_articleId_operationType
                    ON wallabag_sync_operations(articleId, operationType)
                    """.trimIndent(),
                )
            }
        }

        val Migration3To4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wallabag_delete_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        wallabagEntryId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        attemptCount INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_wallabag_delete_operations_wallabagEntryId
                    ON wallabag_delete_operations(wallabagEntryId)
                    """.trimIndent(),
                )
            }
        }
    }
}
