package io.github.tommaso.mappand.ui.grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.data.db.PhotoEntity
import io.github.tommaso.mappand.data.pcloud.PCloudClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val app = MappandApp.from(context)
    val vm: GridViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            GridViewModel(app) as T
    })
    val photos by vm.orphans.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ungeotagged (${photos.size})") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                },
            )
        }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("All photos are geotagged!")
            }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            contentPadding = padding,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(photos, key = { it.fileId }) { photo ->
                PhotoThumbnail(photo = photo, client = app.pCloudClient)
            }
        }
    }
}

@Composable
fun PhotoThumbnail(photo: PhotoEntity, client: PCloudClient) {
    var thumbUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photo.fileId) {
        try { thumbUrl = client.getThumbnailUrl(photo.fileId, 256, 256) } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable { },
    ) {
        AsyncImage(
            model = thumbUrl,
            contentDescription = photo.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (photo.isVideo) {
            Text("▶", modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineMedium)
        }
    }
}
