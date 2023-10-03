package io.homeassistant.companion.android.common.data.servers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.data.wifi.WifiHelper
import javax.inject.Inject

/**
 * A receiver to update the active server based on network changes, for servers that have enabled
 * [io.homeassistant.companion.android.database.server.ServerConnectionInfo.activateOnInternal].
 */
@AndroidEntryPoint
class ServerBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var wifiHelper: WifiHelper

    override fun onReceive(context: Context?, intent: Intent?) {
        if (isInitialStickyBroadcast) return // Ignore as the app might otherwise switch on restart
        if (intent?.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return
        if (!serverManager.isRegistered()) return

        val servers = serverManager.defaultServers
        val autoPref = servers.firstOrNull { it.connection.activateOnInternal && it.connection.isHomeWifiSsid() }?.id ?: return
        val activePref = serverManager.getServer()?.id ?: return

        if (autoPref != activePref) {
            serverManager.activateServer(autoPref)
        }
    }
}
