package io.homeassistant.companion.android.home.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.util.STATE_UNKNOWN
import io.homeassistant.companion.android.home.MainViewModel
import io.homeassistant.companion.android.theme.WearAppTheme
import io.homeassistant.companion.android.theme.getFilledTonalButtonColors
import io.homeassistant.companion.android.theme.getPrimaryButtonColors
import io.homeassistant.companion.android.theme.wearColorScheme
import io.homeassistant.companion.android.util.getIcon
import io.homeassistant.companion.android.util.onEntityClickedFeedback
import io.homeassistant.companion.android.views.ExpandableListHeader
import io.homeassistant.companion.android.views.ListHeader
import io.homeassistant.companion.android.views.ThemeLazyColumn

@Composable
fun MainView(
    mainViewModel: MainViewModel,
    favoriteEntityIds: List<String>,
    onEntityClicked: (String, String) -> Unit,
    onEntityLongClicked: (String) -> Unit,
    onRetryLoadEntitiesClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onNavigationClicked: (entityLists: Map<String, List<String>>) -> Unit,
    isHapticEnabled: Boolean,
    isToastEnabled: Boolean
) {
    var expandedFavorites: Boolean by rememberSaveable { mutableStateOf(true) }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    WearAppTheme {
        ThemeLazyColumn {
            if (favoriteEntityIds.isNotEmpty()) {
                item {
                    ExpandableListHeader(
                        string = stringResource(commonR.string.favorites),
                        expanded = expandedFavorites,
                        onExpandChanged = { expandedFavorites = it }
                    )
                }
                if (expandedFavorites) {
                    items(count = favoriteEntityIds.size,
                            key = { favoriteEntityIds[it].split(",")[0] }) { index ->
                        val favoriteEntityID = favoriteEntityIds[index].split(",")[0]
                        mainViewModel.entities[favoriteEntityID]?.let {
                            EntityUi(
                                    it,
                                    onEntityClicked,
                                    isHapticEnabled,
                                    isToastEnabled
                                ) { entityId -> onEntityLongClicked(entityId) }
                            } ?: run {
                            val cached = mainViewModel.getCachedEntity(favoriteEntityID)
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                icon = {
                                    Image(
                                        asset = getIcon(cached?.icon, favoriteEntityID.split(".")[0], context),
                                        colorFilter = ColorFilter.tint(wearColorScheme.onSurface)
                                    )
                                },
                                label = {
                                    Text(
                                        text = cached?.friendlyName ?: favoriteEntityID,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    onEntityClicked(favoriteEntityID, STATE_UNKNOWN)
                                    onEntityClickedFeedback(isToastEnabled, isHapticEnabled, context, favoriteEntityID, haptic)
                                },
                                colors = getFilledTonalButtonColors()

                                )
                        }
                    }
                }
            }

            if (!mainViewModel.isFavoritesOnly) {
                when (mainViewModel.loadingState.value) {
                    MainViewModel.LoadingState.LOADING -> {
                        if (favoriteEntityIds.isEmpty()) {
                            // Add a Spacer to prevent settings being pushed to the screen center
                            item { Spacer(modifier = Modifier.fillMaxWidth()) }
                        }
                        item {
                            val minHeight =
                                if (favoriteEntityIds.isEmpty()) {
                                    LocalConfiguration.current.screenHeightDp - 64
                                } else {
                                    0
                                }
                            Column(
                                modifier = Modifier
                                    .heightIn(min = minHeight.dp)
                                    .fillMaxSize()
                                    .padding(vertical = if (favoriteEntityIds.isEmpty()) 0.dp else 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ListHeader(id = commonR.string.loading)
                                CircularProgressIndicator()
                            }
                        }
                    }
                    MainViewModel.LoadingState.ERROR -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ListHeader(id = commonR.string.error_loading_entities)
                                Button(
                                    label = {
                                        Text(
                                            text = stringResource(commonR.string.retry),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    onClick = onRetryLoadEntitiesClicked,
                                    colors = ButtonDefaults.buttonColors()
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                    MainViewModel.LoadingState.READY -> {
                        if (mainViewModel.entities.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(commonR.string.no_supported_entities),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 32.dp)
                                    )
                                    Text(
                                        text = stringResource(commonR.string.no_supported_entities_summary),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                            if (mainViewModel.areasWithEntities.any()) {
                                item {
                                    ListHeader(id = commonR.string.areas)
                                }
                                items(
                                    items = mainViewModel.orderedAreaIds,
                                    key = { it }
                                ) { areaId ->
                                    val areaName by rememberSaveable { mutableStateOf(mainViewModel.areas.first { it.areaId == areaId }.name) }
                                    Chip(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = {
                                            Text(text = areaName)
                                        },
                                        onClick = {
                                            onNavigationClicked(
                                                mapOf(areaName to mainViewModel.areasWithEntities[areaId].orEmpty())
                                            )
                                        },
                                        colors = ChipDefaults.primaryChipColors()
                                    )
                                }
                            }


                        if (mainViewModel.domainsWithEntitiesNoArea.any()) {
                            item {
                                ListHeader(id = commonR.string.more_entities)
                            }
                        }
                        // Buttons for each existing category
                        for ((domain, entities) in mainViewModel.domainsWithEntitiesNoArea) {
                                item {
                                    Button(
                                        modifier = Modifier.fillMaxWidth(),
                                        icon = {
                                            getIcon(
                                                "",
                                                domain,
                                                context
                                            ).let { Image(asset = it) }
                                        },
                                        label = { Text(mainViewModel.stringForDomain(domain)!!) },
                                        onClick = {
                                            onNavigationClicked(
                                                mapOf(
                                                    mainViewModel.stringForDomain(domain)!! to entities)
                                            )
                                        },
                                        colors = getPrimaryButtonColors()
                                    )
                                }
                            }


                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        // All entities regardless of area
                        if (mainViewModel.entities.isNotEmpty()) {
                            item {
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    icon = {
                                        Image(
                                            asset = CommunityMaterial.Icon.cmd_animation,
                                            colorFilter = ColorFilter.tint(Color.White)
                                        )
                                    },
                                    label = {
                                        Text(text = stringResource(commonR.string.all_entities))
                                    },
                                    onClick = {
                                        onNavigationClicked(
                                            mainViewModel.domainsWithEntities
                                                    )
                                    },
                                    colors = getFilledTonalButtonColors()
                                )
                            }
                        }
                    }
                }
            }

            if (mainViewModel.isFavoritesOnly) {
                item {
                    Spacer(Modifier.padding(32.dp))
                }
            }

            // Settings
            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    icon = {
                        Image(
                            asset = CommunityMaterial.Icon.cmd_cog,
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    },
                    label = { Text(stringResource(commonR.string.settings)) },
                    onClick = onSettingsClicked,
                    colors = getFilledTonalButtonColors()
                )
            }
        }
    }
}
