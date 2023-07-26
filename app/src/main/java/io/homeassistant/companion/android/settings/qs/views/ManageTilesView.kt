package io.homeassistant.companion.android.settings.qs.views

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.settings.qs.ManageTilesViewModel
import io.homeassistant.companion.android.util.compose.ServerExposedDropdownMenu
import io.homeassistant.companion.android.util.compose.SingleEntityPicker
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun ManageTilesView(
    viewModel: ManageTilesViewModel,
    onShowIconDialog: (tag: String?) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var expandedTile by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect("snackbar") {
        viewModel.tileInfoSnackbar.onEach {
            if (it != 0) {
                snackbarHostState.showSnackbar(context.getString(it))
            }
        }.launchIn(this)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { contentPadding ->
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.padding(all = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tile_select),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Box {
                        OutlinedButton(onClick = { expandedTile = true }) {
                            Text(viewModel.selectedTile.name)
                        }

                        DropdownMenu(expanded = expandedTile, onDismissRequest = { expandedTile = false }) {
                            for ((index, slot) in viewModel.slots.withIndex()) {
                                DropdownMenuItem(
                                    text = { Text(slot.name) },
                                    onClick = {
                                        viewModel.selectTile(index)
                                        expandedTile = false
                                    }
                                )
                            }
                        }
                    }
                }

                Divider()
                TextField(
                    value = viewModel.tileLabel,
                    onValueChange = { viewModel.tileLabel = it },
                    label = {
                        Text(text = stringResource(id = R.string.tile_label))
                    },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    TextField(
                        value = viewModel.tileSubtitle.orEmpty(),
                        onValueChange = { viewModel.tileSubtitle = it },
                        label = {
                            Text(text = stringResource(id = R.string.tile_subtitle))
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                    )
                }

                if (viewModel.servers.size > 1 || viewModel.servers.none { it.id == viewModel.selectedServerId }) {
                    ServerExposedDropdownMenu(
                        servers = viewModel.servers,
                        current = viewModel.selectedServerId,
                        onSelected = viewModel::selectServerId,
                        title = R.string.tile_server,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                SingleEntityPicker(
                    entities = viewModel.sortedEntities,
                    currentEntity = viewModel.selectedEntityId,
                    onEntityCleared = { viewModel.selectEntityId("") },
                    onEntitySelected = {
                        viewModel.selectEntityId(it)
                        return@SingleEntityPicker true
                    },
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    label = { Text(stringResource(R.string.tile_entity)) }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.tile_icon),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedButton(
                        onClick = { onShowIconDialog(viewModel.selectedTile.id) }
                    ) {
                        viewModel.selectedIcon?.let { icon ->
                            com.mikepenz.iconics.compose.Image(
                                icon,
                                contentDescription = stringResource(id = R.string.tile_icon),
                                colorFilter = ColorFilter.tint(colorResource(R.color.colorAccent)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (viewModel.selectedIconId != null && viewModel.selectedEntityId.isNotBlank()) {
                        TextButton(
                            modifier = Modifier.padding(start = 4.dp),
                            onClick = { viewModel.selectIcon(null) }
                        ) {
                            Text(text = stringResource(R.string.tile_icon_original))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tile_vibrate),
                        fontSize = 15.sp
                    )
                    Switch(
                        checked = viewModel.selectedShouldVibrate,
                        onCheckedChange = { viewModel.selectedShouldVibrate = it },
                        colors = SwitchDefaults.colors(uncheckedThumbColor = colorResource(R.color.colorSwitchUncheckedThumb))
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tile_auth_required),
                        fontSize = 15.sp
                    )
                    Switch(
                        checked = viewModel.tileAuthRequired,
                        onCheckedChange = { viewModel.tileAuthRequired = it },
                        colors = SwitchDefaults.colors(uncheckedThumbColor = colorResource(R.color.colorSwitchUncheckedThumb))
                    )
                }

                Button(
                    onClick = { viewModel.addTile() },
                    enabled = viewModel.tileLabel.isNotBlank() &&
                        viewModel.selectedServerId in viewModel.servers.map { it.id } &&
                        viewModel.selectedEntityId in viewModel.sortedEntities.map { it.entityId }
                ) {
                    Text(stringResource(viewModel.submitButtonLabel))
                }
            }
        }
    }
}
