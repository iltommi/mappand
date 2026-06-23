package io.github.tommaso.mappand.domain

import io.github.tommaso.mappand.data.db.PhotoDao
import io.github.tommaso.mappand.data.pcloud.PCloudClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SyncManager(private val dao: PhotoDao, private val client: PCloudClient) {

    suspend fun flushIndex(rootFolderId: Long) {
        val all = dao.observeAll().first()
        val entries = JSONArray()
        for (p in all) {
            entries.put(JSONObject().apply {
                put("fileid", p.fileId)
                put("name", p.name)
                if (p.lat != null) put("lat", p.lat) else put("lat", JSONObject.NULL)
                if (p.lng != null) put("lng", p.lng) else put("lng", JSONObject.NULL)
                if (p.ts != null) put("ts", p.ts) else put("ts", JSONObject.NULL)
                if (p.hash != null) put("hash", p.hash) else put("hash", JSONObject.NULL)
            })
        }
        val json = JSONObject().apply {
            put("version", 1)
            put("entries", entries)
        }.toString()
        client.uploadFile(rootFolderId, "index.json", json.toByteArray())
    }
}
