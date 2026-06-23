package io.github.tommaso.mappand.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.data.db.PhotoEntity
import io.github.tommaso.mappand.ui.folder.FolderPickerDialog
import io.github.tommaso.mappand.ui.grid.GridScreen
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = MappandApp.from(context)
    val vm: MapViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            MapViewModel(app) as T
    })

    val photos by vm.geotaggedPhotos.collectAsStateWithLifecycle()
    val orphanCount by vm.orphanCount.collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val selectedFolder by vm.selectedFolder.collectAsStateWithLifecycle()
    val showFolderPicker by vm.showFolderPicker.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }

    if (showGrid) {
        GridScreen(onClose = { showGrid = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(modifier = Modifier.fillMaxSize(), photos = photos)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), MaterialTheme.shapes.medium)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (selectedFolder != null) {
                    Text(
                        "📁 ${selectedFolder!!.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "${photos.size} geotagged · $orphanCount to tag",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (scanProgress.running) {
                    Text(
                        "Scanning ${scanProgress.scanned}/${scanProgress.total ?: "?"}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                scanProgress.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (orphanCount > 0) {
                    IconButton(onClick = { showGrid = true }) {
                        Icon(Icons.Default.GridView, "Grid", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = { if (scanProgress.running) vm.stopScan() else vm.startScan() }) {
                    if (scanProgress.running)
                        Icon(Icons.Default.Stop, "Stop")
                    else
                        Icon(Icons.Default.Refresh, "Scan")
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Choose folder") },
                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                            onClick = { showMenu = false; vm.openFolderPicker() },
                        )
                        DropdownMenuItem(text = { Text("Erase cache") }, onClick = {
                            showMenu = false; vm.eraseCache()
                        })
                        DropdownMenuItem(text = { Text("Disconnect") }, onClick = {
                            showMenu = false; vm.logout(); onLogout()
                        })
                    }
                }
            }
        }

        if (scanProgress.running) {
            val total = scanProgress.total
            if (total != null && total > 0) {
                LinearProgressIndicator(
                    progress = { scanProgress.scanned.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter))
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            client = app.pCloudClient,
            onSelect = { id, name -> vm.selectFolder(id, name) },
            onDismiss = { vm.closeFolderPicker() },
        )
    }
}

@Composable
fun OsmMapView(modifier: Modifier = Modifier, photos: List<PhotoEntity>) {
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = "Mappand/1.0"

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(20.0, 0.0))
        }
    }

    LaunchedEffect(photos) {
        mapView.overlays.removeIf { it is Marker }
        for (photo in photos) {
            val lat = photo.lat ?: continue
            val lng = photo.lng ?: continue
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lng)
                title = photo.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    DisposableEffect(Unit) { onDispose { mapView.onDetach() } }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { it.onResume() },
    )
}
