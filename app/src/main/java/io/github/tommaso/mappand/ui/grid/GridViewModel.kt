package io.github.tommaso.mappand.ui.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.data.db.PhotoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GridViewModel(private val app: MappandApp) : ViewModel() {

    private val dao = app.db.photoDao()

    val orphans: StateFlow<List<PhotoEntity>> = dao.observeOrphans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun ignore(fileId: Long) {
        viewModelScope.launch { dao.ignore(fileId) }
    }
}
