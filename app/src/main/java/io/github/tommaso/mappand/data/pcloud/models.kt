package io.github.tommaso.mappand.data.pcloud

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PCloudResponse(
    val result: Int = 0,
    val error: String? = null,
    val auth: String? = null,
    val token: String? = null,
    val authtoken: String? = null,
    val metadata: PCloudMeta? = null,
    val hosts: List<String>? = null,
    val path: String? = null,
    @Json(name = "fileid") val fileId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class PCloudMeta(
    val name: String = "",
    val isfolder: Boolean = false,
    @Json(name = "fileid") val fileId: Long = 0,
    @Json(name = "folderid") val folderId: Long = 0,
    val size: Long = 0,
    val contents: List<PCloudMeta>? = null,
    val hash: String? = null,
)

data class PCloudFile(
    val fileId: Long,
    val folderId: Long,
    val name: String,
    val size: Long,
    val hash: String?,
)
