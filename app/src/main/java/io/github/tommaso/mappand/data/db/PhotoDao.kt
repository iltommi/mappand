package io.github.tommaso.mappand.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Upsert
    suspend fun upsert(photo: PhotoEntity)

    @Upsert
    suspend fun upsertAll(photos: List<PhotoEntity>)

    @Query("SELECT * FROM photos WHERE ignored = 0")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE lat IS NOT NULL AND lng IS NOT NULL AND ignored = 0")
    fun observeGeotagged(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE lat IS NULL AND ignored = 0")
    fun observeOrphans(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE lat IS NULL AND ignored = 0 ORDER BY ts DESC LIMIT :limit OFFSET :offset")
    suspend fun getOrphansPage(offset: Int, limit: Int): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos WHERE ignored = 0")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE lat IS NOT NULL AND ignored = 0")
    suspend fun countGeotagged(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE lat IS NULL AND ignored = 0")
    suspend fun countOrphans(): Int

    @Query("SELECT COUNT(*) FROM photos WHERE ignored = 1")
    suspend fun countIgnored(): Int

    @Query("UPDATE photos SET lat = :lat, lng = :lng WHERE fileId = :fileId")
    suspend fun setLocation(fileId: Long, lat: Double, lng: Double)

    @Query("UPDATE photos SET ts = :ts WHERE fileId = :fileId")
    suspend fun setTimestamp(fileId: Long, ts: Long)

    @Query("UPDATE photos SET ignored = 1 WHERE fileId = :fileId")
    suspend fun ignore(fileId: Long)

    @Query("DELETE FROM photos WHERE fileId = :fileId")
    suspend fun delete(fileId: Long)

    @Query("DELETE FROM photos")
    suspend fun deleteAll()

    @Query("SELECT * FROM photos WHERE fileId = :fileId")
    suspend fun getById(fileId: Long): PhotoEntity?
}
