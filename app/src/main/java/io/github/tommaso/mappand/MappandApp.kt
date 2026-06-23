package io.github.tommaso.mappand

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import io.github.tommaso.mappand.data.auth.AuthDataStore
import io.github.tommaso.mappand.data.db.MappandDatabase
import io.github.tommaso.mappand.data.pcloud.PCloudClient

class MappandApp : Application(), Configuration.Provider {

    val db by lazy { MappandDatabase.getInstance(this) }
    val authDataStore by lazy { AuthDataStore(this) }
    val pCloudClient by lazy { PCloudClient(authDataStore) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    companion object {
        fun from(context: Context) = context.applicationContext as MappandApp
    }
}
