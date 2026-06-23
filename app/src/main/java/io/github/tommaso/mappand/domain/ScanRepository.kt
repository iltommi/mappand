package io.github.tommaso.mappand.domain

import io.github.tommaso.mappand.data.db.PhotoDao
import io.github.tommaso.mappand.data.db.PhotoEntity
import io.github.tommaso.mappand.data.pcloud.PCloudClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ScanProgress(
    val scanned: Int = 0,
    val geotagged: Int = 0,
    val dated: Int = 0,
    val total: Int? = null,
    val running: Boolean = false,
    val error: String? = null,
)

class ScanRepository(
    private val dao: PhotoDao,
    private val client: PCloudClient,
) {
    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress

    @Volatile private var cancelled = false

    fun cancel() { cancelled = true }

    suspend fun scan(rootFolderId: Long = 0) {
        cancelled = false
        _progress.value = ScanProgress(running = true)
        try {
            val files = client.listAllImages(rootFolderId)
            val total = files.size
            var scanned = 0; var geotagged = 0; var dated = 0
            val videoRegex = Regex(""".+\.(mp4|mov|3gp|3gpp|avi)$""", RegexOption.IGNORE_CASE)

            for (file in files) {
                if (cancelled) break
                val existing = dao.getById(file.fileId)
                val isVideo = videoRegex.matches(file.name)

                val meta: PhotoMeta = when {
                    existing != null && existing.hash == file.hash ->
                        PhotoMeta(existing.lat, existing.lng, existing.ts)
                    !isVideo -> try {
                        ExifUtil.extractFromBytes(client.downloadFileHead(file.fileId))
                    } catch (_: Exception) {
                        PhotoMeta(null, null, ExifUtil.parseDateFromFilename(file.name))
                    }
                    else -> PhotoMeta(null, null, ExifUtil.parseDateFromFilename(file.name))
                }

                dao.upsert(PhotoEntity(
                    fileId = file.fileId,
                    name = file.name,
                    folderId = file.folderId,
                    lat = meta.lat,
                    lng = meta.lng,
                    ts = meta.ts,
                    hash = file.hash,
                    isVideo = isVideo,
                ))

                scanned++
                if (meta.lat != null) geotagged++
                if (meta.ts != null) dated++
                _progress.value = ScanProgress(scanned, geotagged, dated, total, running = true)
            }
            _progress.value = ScanProgress(scanned, geotagged, dated, total, running = false)
        } catch (e: Exception) {
            _progress.value = ScanProgress(error = e.message, running = false)
        }
    }
}
