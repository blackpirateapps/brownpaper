package com.blackpirateapps.brownpaper.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArticleEntity::class,
        ArticleFtsEntity::class,
        FolderEntity::class,
        TagEntity::class,
        ArticleTagCrossRef::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true,
)
abstract class BrownPaperDatabase : RoomDatabase() {
    abstract fun brownPaperDao(): BrownPaperDao
}

