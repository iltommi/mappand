package io.github.tommaso.mappand.domain

import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoMeta(
    val lat: Double?,
    val lng: Double?,
    val ts: Long?,
)

object ExifUtil {

    private val DATE_FMT = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    fun extractFromBytes(bytes: ByteArray): PhotoMeta {
        return try {
            ByteArrayInputStream(bytes).use { stream ->
                val exif = ExifInterface(stream)
                val latLng = exif.latLong
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                val ts = dateStr?.let {
                    try { DATE_FMT.parse(it)?.time } catch (_: Exception) { null }
                }
                PhotoMeta(latLng?.get(0), latLng?.get(1), ts)
            }
        } catch (_: Exception) {
            PhotoMeta(null, null, null)
        }
    }

    fun injectGps(originalBytes: ByteArray, lat: Double, lng: Double): ByteArray {
        val tmp = java.io.File.createTempFile("exif_gps", ".jpg")
        return try {
            tmp.writeBytes(originalBytes)
            val exif = ExifInterface(tmp.absolutePath)
            exif.setLatLong(lat, lng)
            exif.saveAttributes()
            tmp.readBytes()
        } finally {
            tmp.delete()
        }
    }

    fun injectDatetime(originalBytes: ByteArray, ts: Long): ByteArray {
        val tmp = java.io.File.createTempFile("exif_date", ".jpg")
        return try {
            tmp.writeBytes(originalBytes)
            val exif = ExifInterface(tmp.absolutePath)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, DATE_FMT.format(Date(ts)))
            exif.saveAttributes()
            tmp.readBytes()
        } finally {
            tmp.delete()
        }
    }

    fun parseDateFromFilename(name: String): Long? {
        val m = Regex("""(\d{4})(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})""").find(name) ?: return null
        val (y, mo, d, h, mi, s) = m.destructured
        return try { DATE_FMT.parse("$y:$mo:$d $h:$mi:$s")?.time } catch (_: Exception) { null }
    }
}
