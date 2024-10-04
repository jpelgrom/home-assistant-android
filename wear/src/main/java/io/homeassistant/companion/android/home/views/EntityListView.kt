package io.homeassistant.companion.android.home.views

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.theme.WearAppTheme
import io.homeassistant.companion.android.theme.getPrimaryButtonColors
import io.homeassistant.companion.android.util.playPreviewEntityScene1
import io.homeassistant.companion.android.util.playPreviewEntityScene2
import io.homeassistant.companion.android.util.playPreviewEntityScene3
import io.homeassistant.companion.android.util.previewEntity1
import io.homeassistant.companion.android.util.previewEntity2
import io.homeassistant.companion.android.views.ExpandableListHeader
import io.homeassistant.companion.android.views.ListHeader
import io.homeassistant.companion.android.views.ThemeLazyColumn
import io.homeassistant.companion.android.views.rememberExpandedStates

@Composable
fun EntityViewList(
    allEntities: Map<String, Entity<*>>,
    entityLists: Map<String, List<String>>,
    entityListsOrder: List<String>,
    onEntityClicked: (String, String) -> Unit,
    onEntityLongClicked: (String) -> Unit,
    isHapticEnabled: Boolean,
    isToastEnabled: Boolean
) {
    // Remember expanded state of each header
    val expandedStates = rememberExpandedStates(entityLists.keys.map { it.hashCode() })

    WearAppTheme {
        ThemeLazyColumn {
            for (header in entityListsOrder) {
                val entities = entityLists[header].orEmpty()
                if (entities.isNotEmpty()) {
                    item {
                        if (entityLists.size > 1) {
                            ExpandableListHeader(
                                string = header,
                                key = header.hashCode(),
                                expandedStates = expandedStates
                            )
                        } else {
                            ListHeader(header)
                        }
                    }
                    if (expandedStates[header.hashCode()] == true) {
                        items(entities, key = { it }) { entityId ->
                            allEntities[entityId]?.let {
                                EntityUi(
                                    it,
                                    onEntityClicked,
                                    isHapticEnabled,
                                    isToastEnabled
                                ) { entityId -> onEntityLongClicked(entityId) }
                            }
                        }

                        if (entities.isEmpty()) {
                            item {
                                Column {
                                    Button(
                                        label = {
                                            Text(
                                                text = stringResource(commonR.string.loading_entities),
                                                textAlign = TextAlign.Center
                                            )
                                        },
                                        onClick = { /* No op */ },
                                        colors = getPrimaryButtonColors()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewEntityListView() {
    EntityViewList(
        allEntities = mapOf(previewEntity1.entityId to previewEntity1, previewEntity2.entityId to previewEntity2),
        entityLists = mapOf(stringResource(commonR.string.lights) to listOf(previewEntity1.entityId, previewEntity2.entityId)),
        entityListsOrder = listOf(stringResource(commonR.string.lights)),
        onEntityClicked = { _, _ -> },
        onEntityLongClicked = { },
        isHapticEnabled = false,
        isToastEnabled = false
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewEntityListScenes() {
    EntityViewList(
        allEntities = mapOf(playPreviewEntityScene1.entityId to playPreviewEntityScene1, playPreviewEntityScene2.entityId to playPreviewEntityScene2),
        entityLists = mapOf(stringResource(commonR.string.scenes) to listOf(playPreviewEntityScene1.entityId, playPreviewEntityScene2.entityId)),
        entityListsOrder = listOf(stringResource(commonR.string.scenes)),
        onEntityClicked = { _, _ -> },
        onEntityLongClicked = { },
        isHapticEnabled = false,
        isToastEnabled = false
    )
}

@Preview(device = WearDevices.LARGE_ROUND)
@Composable
private fun PreviewEntityListEmpty() {
    EntityViewList(
        entityLists = mapOf(stringResource(commonR.string.scenes) to listOf(playPreviewEntityScene1)),
        entityListsOrder = listOf(stringResource(commonR.string.scenes)),
        entityListFilter = { false },
        onEntityClicked = { _, _ -> },
        onEntityLongClicked = { },
        isHapticEnabled = false,
        isToastEnabled = false
    )
}
