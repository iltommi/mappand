package io.github.tommaso.mappand.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.data.auth.AuthDataStore
import io.github.tommaso.mappand.data.db.PhotoEntity
import io.github.tommaso.mappand.domain.ScanProgress
import io.github.tommaso.mappand.domain.ScanRepository
import io.github.tommaso.mappand.workers.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MapViewModel(private val app: MappandApp) : ViewModel() {

    private val dao = app.db.photoDao()
    private val scanRepo = ScanRepository(dao, app.pCloudClient)

    val geotaggedPhotos: StateFlow<List<PhotoEntity>> = dao.observeGeotagged()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val orphanCount: StateFlow<Int> = dao.observeOrphans()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val scanProgress: StateFlow<ScanProgress> = scanRepo.progress

    val selectedFolder: StateFlow<AuthDataStore.SelectedFolder?> =
        app.authDataStore.selectedFolderFlow
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _showFolderPicker = MutableStateFlow(false)
    val showFolderPicker: StateFlow<Boolean> = _showFolderPicker

    fun openFolderPicker() { _showFolderPicker.value = true }
    fun closeFolderPicker() { _showFolderPicker.value = false }

    fun selectFolder(id: Long, name: String) {
        viewModelScope.launch {
            app.authDataStore.setSelectedFolder(id, name)
            _showFolderPicker.value = false
        }
    }

    fun startScan() {
        val folder = selectedFolder.value
        if (folder == null) {
            _showFolderPicker.value = true
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            scanRepo.scan(folder.id)
            SyncWorker.schedule(app)
        }
    }

    fun stopScan() = scanRepo.cancel()

    fun eraseCache() {
        viewModelScope.launch { dao.deleteAll() }
    }

    fun logout() {
        viewModelScope.launch { app.authDataStore.clearToken() }
    }
}
