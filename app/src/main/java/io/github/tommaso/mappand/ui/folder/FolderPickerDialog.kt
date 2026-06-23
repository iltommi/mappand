package io.github.tommaso.mappand.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tommaso.mappand.data.pcloud.PCloudClient
import io.github.tommaso.mappand.data.pcloud.PCloudMeta
import kotlinx.coroutines.launch

private data class BreadcrumbEntry(val id: Long, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
    client: PCloudClient,
    onSelect: (id: Long, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var breadcrumb by remember { mutableStateOf(listOf(BreadcrumbEntry(0L, "pCloud"))) }
    val current = breadcrumb.last()

    var folders by remember(current.id) { mutableStateOf<List<PCloudMeta>>(emptyList()) }
    var loading by remember(current.id) { mutableStateOf(true) }
    var error by remember(current.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(current.id) {
        loading = true
        error = null
        try {
            folders = client.listFolders(current.id)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (breadcrumb.size > 1) {
                    IconButton(onClick = { breadcrumb = breadcrumb.dropLast(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Text(
                    text = breadcrumb.joinToString(" › ") { it.name },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
            }

            HorizontalDivider()

            // "Scan this folder" button
            Button(
                onClick = { onSelect(current.id, current.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Scan \"${current.name}\"")
            }

            HorizontalDivider()

            // Folder list
            when {
                loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                error != null -> Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )

                folders.isEmpty() -> Text(
                    "No subfolders",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )

                else -> LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(folders, key = { it.folderId }) { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            leadingContent = {
                                Icon(Icons.Default.Folder, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            },
                            modifier = Modifier.clickable {
                                breadcrumb = breadcrumb + BreadcrumbEntry(folder.folderId, folder.name)
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
