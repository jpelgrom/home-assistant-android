package io.homeassistant.companion.android.settings.ssid.views

import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.homeassistant.companion.android.common.data.wifi.WifiHelper
import io.homeassistant.companion.android.common.R as commonR

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SsidView(
    wifiSsids: List<String>,
    prioritizeInternal: Boolean,
    usingWifi: Boolean,
    activeSsid: String?,
    activeBssid: String?,
    onAddWifiSsid: (String) -> Boolean,
    onRemoveWifiSsid: (String) -> Unit,
    onSetPrioritize: (Boolean) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
        item("ssid.intro") {
            Column {
                Text(
                    text = stringResource(commonR.string.manage_ssids_introduction),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    var ssidInput by remember { mutableStateOf("") }
                    var ssidError by remember { mutableStateOf(false) }

                    TextField(
                        value = ssidInput,
                        singleLine = true,
                        onValueChange = {
                            ssidInput = it
                            ssidError = false
                        },
                        label = { Text(stringResource(commonR.string.manage_ssids_input)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                ssidError = !onAddWifiSsid(ssidInput)
                                if (!ssidError) ssidInput = ""
                            }
                        ),
                        isError = ssidError,
                        trailingIcon = if (ssidError) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = stringResource(commonR.string.manage_ssids_input_exists)
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        modifier = Modifier
                            .height(56.dp) // align with TextField: 56
                            .padding(start = 8.dp, top = 0.dp),
                        onClick = {
                            keyboardController?.hide()
                            ssidError = !onAddWifiSsid(ssidInput)
                            if (!ssidError) ssidInput = ""
                        }
                    ) {
                        Text(stringResource(commonR.string.add_ssid))
                    }
                }
            }
        }

        if (
            activeSsid?.isNotBlank() == true &&
            wifiSsids.none { it == activeSsid } &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || activeSsid !== WifiManager.UNKNOWN_SSID)
        ) {
            item("ssid.suggestion") {
                AssistChip(
                    label = { Text(stringResource(commonR.string.add_ssid_name_suggestion, activeSsid)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { onAddWifiSsid(activeSsid) }
                )
            }
        }
        items(wifiSsids, key = { "ssid.item.$it" }) {
            val connected = remember(it, activeSsid, activeBssid, usingWifi) {
                usingWifi &&
                    (
                        it == activeSsid ||
                            (it.startsWith(WifiHelper.BSSID_PREFIX) && it.removePrefix(WifiHelper.BSSID_PREFIX).equals(activeBssid, ignoreCase = true))
                        )
            }
            Row(
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateItemPlacement(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint =
                    if (connected) {
                        colorResource(commonR.color.colorAccent)
                    } else {
                        LocalContentColor.current
                    }
                )
                Text(
                    text =
                    if (it.startsWith(WifiHelper.BSSID_PREFIX)) {
                        it.removePrefix(WifiHelper.BSSID_PREFIX)
                    } else {
                        it
                    },
                    fontFamily =
                    if (it.startsWith(WifiHelper.BSSID_PREFIX)) {
                        FontFamily.Monospace
                    } else {
                        null
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(commonR.string.remove_ssid),
                    tint = colorResource(commonR.color.colorWarning),
                    modifier = Modifier
                        .clickable { onRemoveWifiSsid(it) }
                        .size(48.dp)
                        .padding(all = 12.dp)
                )
            }
        }

        item("prioritize") {
            var prioritizeDropdown by remember { mutableStateOf(false) }

            Column {
                Spacer(modifier = Modifier.height(48.dp))
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                Box {
                    Column(
                        modifier = Modifier
                            .clickable { prioritizeDropdown = true }
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        Text(
                            text = stringResource(commonR.string.prioritize_internal_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                            Text(
                                text = stringResource(
                                    if (prioritizeInternal) {
                                        commonR.string.prioritize_internal_on
                                    } else {
                                        commonR.string.prioritize_internal_off
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (prioritizeDropdown) {
                        DropdownMenu(
                            expanded = prioritizeDropdown,
                            onDismissRequest = { prioritizeDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(commonR.string.prioritize_internal_off)) },
                                onClick = {
                                    onSetPrioritize(false)
                                    prioritizeDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(commonR.string.prioritize_internal_on_expanded)) },
                                onClick = {
                                    onSetPrioritize(true)
                                    prioritizeDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSsidViewEmpty() {
    SsidView(
        wifiSsids = emptyList(),
        prioritizeInternal = false,
        activeSsid = "home-assistant-wifi",
        activeBssid = "02:00:00:00:00:00",
        usingWifi = true,
        onAddWifiSsid = { true },
        onRemoveWifiSsid = {},
        onSetPrioritize = {}
    )
}

@Preview
@Composable
private fun PreviewSsidViewItems() {
    SsidView(
        wifiSsids = listOf("home-assistant-wifi", "wifi-one", "BSSID:1A:2B:3C:4D:5E:6F"),
        prioritizeInternal = false,
        activeSsid = "home-assistant-wifi",
        activeBssid = "02:00:00:00:00:00",
        usingWifi = true,
        onAddWifiSsid = { true },
        onRemoveWifiSsid = {},
        onSetPrioritize = {}
    )
}
