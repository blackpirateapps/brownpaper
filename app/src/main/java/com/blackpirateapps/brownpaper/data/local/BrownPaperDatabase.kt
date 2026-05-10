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
        ArticleAnnotationEntity::class,
        PendingWallabagAnnotationOperationEntity::class,
    ],
    version = 5,
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

        val Migration4To5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS article_annotations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        articleId INTEGER NOT NULL,
                        wallabagAnnotationId TEXT,
                        wallabagEntryId INTEGER,
                        quote TEXT NOT NULL,
                        noteText TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        wallabagRangesJson TEXT NOT NULL,
                        startParagraphIndex INTEGER NOT NULL,
                        endParagraphIndex INTEGER NOT NULL,
                        startCharOffset INTEGER NOT NULL,
                        endCharOffset INTEGER NOT NULL,
                        prefixText TEXT,
                        suffixText TEXT,
                        bodyTextHash TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        remoteUpdatedAt INTEGER,
                        lastSyncedAt INTEGER,
                        deletedAt INTEGER,
                        FOREIGN KEY(articleId) REFERENCES articles(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_article_annotations_articleId ON article_annotations(articleId)")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_article_annotations_wallabagAnnotationId
                    ON article_annotations(wallabagAnnotationId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_article_annotations_wallabagEntryId
                    ON article_annotations(wallabagEntryId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_article_annotations_articleId_deletedAt
                    ON article_annotations(articleId, deletedAt)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wallabag_annotation_sync_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        articleId INTEGER NOT NULL,
                        annotationId INTEGER,
                        wallabagAnnotationId TEXT,
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
                    CREATE INDEX IF NOT EXISTS index_wallabag_annotation_sync_operations_articleId
                    ON wallabag_annotation_sync_operations(articleId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_wallabag_annotation_sync_operations_annotationId
                    ON wallabag_annotation_sync_operations(annotationId)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_wallabag_annotation_sync_operations_wallabagAnnotationId
                    ON wallabag_annotation_sync_operations(wallabagAnnotationId)
                    """.trimIndent(),
                )
            }
        }
    }
}
