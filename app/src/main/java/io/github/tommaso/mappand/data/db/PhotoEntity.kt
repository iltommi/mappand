package io.github.tommaso.mappand.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val fileId: Long,
    val name: String,
    val folderId: Long,
    val lat: Double?,
    val lng: Double?,
    val ts: Long?,
    val hash: String?,
    val ignored: Boolean = false,
    val isVideo: Boolean = false,
)
