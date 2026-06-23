package io.github.tommaso.mappand.data.pcloud

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.tommaso.mappand.data.auth.AuthDataStore
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TwoFactorRequired(val tfaToken: String) : Exception("2FA required")

class PCloudClient(private val auth: AuthDataStore) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val responseAdapter = moshi.adapter(PCloudResponse::class.java)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun buildUrl(endpoint: String, params: Map<String, String> = emptyMap()): String {
        val host = auth.getHost()
        val token = auth.getToken()
        val sb = StringBuilder("$host/$endpoint?")
        if (token != null) sb.append("auth=${enc(token)}&")
        params.forEach { (k, v) -> sb.append("$k=${enc(v)}&") }
        return sb.trimEnd('&').toString()
    }

    private fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8")

    private suspend fun get(url: String): PCloudResponse = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).build()
        val body = http.newCall(req).execute().use { it.body?.string() ?: "{}" }
        responseAdapter.fromJson(body) ?: throw Exception("Empty response")
    }

    suspend fun loginWithPassword(email: String, password: String): String {
        val deviceId = auth.getDeviceId()
        val host = auth.getHost()
        val body = FormBody.Builder()
            .add("username", email)
            .add("password", password)
            .add("getauth", "1")
            .add("logout", "1")
            .add("os", "4")
            .add("deviceid", deviceId)
            .build()
        val resp = withContext(Dispatchers.IO) {
            val req = Request.Builder().url("$host/login").post(body).build()
            val json = http.newCall(req).execute().use { it.body?.string() ?: "{}" }
            responseAdapter.fromJson(json) ?: throw Exception("Empty response")
        }
        return when (resp.result) {
            0 -> resp.auth ?: resp.token ?: throw Exception("No token in response")
            2297 -> throw TwoFactorRequired(resp.token ?: throw Exception("No TFA token"))
            else -> throw Exception("pCloud ${resp.result}: ${resp.error}")
        }
    }

    suspend fun loginWithTfa(tfaToken: String, code: String): String {
        val host = auth.getHost()
        val digits = code.filter { it.isDigit() }
        val url = "$host/tfa_login?token=${enc(tfaToken)}&code=${enc(digits)}&trustdevice=false"
        val resp = get(url)
        return when (resp.result) {
            0 -> resp.auth ?: resp.token ?: resp.authtoken ?: throw Exception("No token")
            else -> throw Exception("pCloud ${resp.result}: ${resp.error}")
        }
    }

    suspend fun listAllImages(folderId: Long = 0, excludeFolderId: Long? = null): List<PCloudFile> {
        val result = mutableListOf<PCloudFile>()
        val queue = ArrayDeque<Long>()
        queue.add(folderId)
        val mediaRegex = Regex("""\.(jpe?g|heic|mp4|mov|3gp|3gpp|avi)$""", RegexOption.IGNORE_CASE)
        while (queue.isNotEmpty()) {
            val fid = queue.removeFirst()
            if (excludeFolderId != null && fid == excludeFolderId) continue
            try {
                val url = buildUrl("listfolder", mapOf("folderid" to fid.toString()))
                val resp = get(url)
                if (resp.result != 0) continue
                for (item in resp.metadata?.contents ?: emptyList()) {
                    if (item.isfolder) queue.add(item.folderId)
                    else if (mediaRegex.containsMatchIn(item.name))
                        result.add(PCloudFile(item.fileId, item.folderId, item.name, item.size, item.hash))
                }
            } catch (_: Exception) {}
        }
        return result
    }

    suspend fun getFileLink(fileId: Long): String {
        val url = buildUrl("getfilelink", mapOf("fileid" to fileId.toString()))
        val resp = get(url)
        if (resp.result != 0) throw Exception("pCloud ${resp.result}: ${resp.error}")
        val host = resp.hosts?.firstOrNull() ?: throw Exception("No CDN host")
        return "https://$host${resp.path}"
    }

    suspend fun getThumbnailUrl(fileId: Long, width: Int = 256, height: Int = 256): String {
        val url = buildUrl("getthumb", mapOf(
            "fileid" to fileId.toString(),
            "size" to "${width}x${height}",
            "crop" to "0",
            "type" to "jpeg",
        ))
        val resp = get(url)
        if (resp.result != 0) throw Exception("pCloud ${resp.result}: ${resp.error}")
        val host = resp.hosts?.firstOrNull() ?: throw Exception("No CDN host")
        return "https://$host${resp.path}"
    }

    suspend fun downloadFileHead(fileId: Long, bytes: Int = 131072): ByteArray = withContext(Dispatchers.IO) {
        val cdnUrl = getFileLink(fileId)
        val req = Request.Builder()
            .url(cdnUrl)
            .header("Range", "bytes=0-${bytes - 1}")
            .build()
        http.newCall(req).execute().use { it.body?.bytes() ?: ByteArray(0) }
    }

    suspend fun uploadFile(folderId: Long, name: String, data: ByteArray, overwriteFileId: Long? = null): Long = withContext(Dispatchers.IO) {
        val token = auth.getToken() ?: throw Exception("Not authenticated")
        val host = auth.getHost()
        var urlStr = "$host/uploadfile?auth=${enc(token)}&folderid=${enc(folderId.toString())}&filename=${enc(name)}"
        if (overwriteFileId != null) urlStr += "&fileid=${overwriteFileId}"
        val body = data.toRequestBody("application/octet-stream".toMediaType())
        val req = Request.Builder().url(urlStr).post(body).build()
        val respStr = http.newCall(req).execute().use { it.body?.string() ?: "{}" }
        val resp = responseAdapter.fromJson(respStr) ?: throw Exception("Empty upload response")
        if (resp.result != 0) throw Exception("pCloud upload ${resp.result}: ${resp.error}")
        resp.metadata?.fileId ?: throw Exception("No fileId in upload response")
    }

    suspend fun deleteFile(fileId: Long) {
        val url = buildUrl("deletefile", mapOf("fileid" to fileId.toString()))
        val resp = get(url)
        if (resp.result != 0) throw Exception("pCloud ${resp.result}: ${resp.error}")
    }

    suspend fun createFolderIfNotExists(parentId: Long, name: String): Long {
        val url = buildUrl("createfolderifnotexists", mapOf("folderid" to parentId.toString(), "name" to name))
        val resp = get(url)
        if (resp.result != 0) throw Exception("pCloud ${resp.result}: ${resp.error}")
        return resp.metadata?.folderId ?: throw Exception("No folderId")
    }

    suspend fun getRootFolderId(): Long {
        val url = buildUrl("listfolder", mapOf("folderid" to "0", "nofiles" to "1"))
        val resp = get(url)
        if (resp.result != 0) throw Exception("pCloud ${resp.result}: ${resp.error}")
        return resp.metadata?.folderId ?: 0L
    }
}
