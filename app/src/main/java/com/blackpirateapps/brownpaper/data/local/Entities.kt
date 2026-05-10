package com.blackpirateapps.brownpaper.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded
import kotlinx.serialization.Serializable

@Entity(
    tableName = "folders",
    indices = [Index(value = ["name"], unique = true)],
)
@Serializable
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
@Serializable
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
)

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["originalUrl"], unique = true),
        Index(value = ["wallabagEntryId"], unique = true),
        Index(value = ["folderId"]),
    ],
)
@Serializable
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalUrl: String,
    val dateAdded: Long,
    val isLiked: Boolean = false,
    val isArchived: Boolean = false,
    val folderId: Long? = null,
    val extractedTextContent: String,
    val extractedHeroImageUrl: String? = null,
    @ColumnInfo(defaultValue = "0") val isVideo: Boolean = false,
    val youtubeVideoId: String? = null,
    val videoRuntimeText: String? = null,
    val channelName: String? = null,
    @ColumnInfo(defaultValue = "0") val viewCount: Long = 0L,
    @ColumnInfo(defaultValue = "0.0") val videoPositionSeconds: Float = 0f,
    val wallabagEntryId: Long? = null,
    val remoteUpdatedAt: Long? = null,
    val lastSyncedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val localModifiedAt: Long = 0L,
)

@Entity(
    tableName = "wallabag_sync_operations",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["articleId"]),
        Index(value = ["articleId", "operationType"], unique = true),
    ],
)
data class PendingSyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long,
    val operationType: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val attemptCount: Int = 0,
    val lastError: String? = null,
)

@Entity(
    tableName = "article_tag_cross_ref",
    primaryKeys = ["articleId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tagId"])],
)
@Serializable
data class ArticleTagCrossRef(
    val articleId: Long,
    val tagId: Long,
)

@Fts4(contentEntity = ArticleEntity::class)
@Entity(tableName = "article_fts")
data class ArticleFtsEntity(
    val title: String,
    @ColumnInfo(name = "extractedTextContent") val extractedTextContent: String,
)

data class ArticleWithRelations(
    @Embedded val article: ArticleEntity,
    @Relation(
        parentColumn = "folderId",
        entityColumn = "id",
    )
    val folder: FolderEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ArticleTagCrossRef::class,
            parentColumn = "articleId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)

@Serializable
data class BackupData(
    val articles: List<ArticleEntity>,
    val folders: List<FolderEntity>,
    val tags: List<TagEntity>,
    val tagCrossRefs: List<ArticleTagCrossRef>
)
