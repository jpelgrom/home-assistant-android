package io.homeassistant.companion.android.notifications

import android.content.Context
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.data.integration.DeviceRegistration
import io.homeassistant.companion.android.common.data.servers.ServerManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.unifiedpush.android.connector.MessagingReceiver

@AndroidEntryPoint
class UnifiedPushReceiver : MessagingReceiver() {
    companion object {
        private const val TAG = "UPReceiver"
        private const val SOURCE = "UnifiedPush (%s)"
    }

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var messagingManager: MessagingManager

    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        val asString = message.toString(Charsets.UTF_8)
        Log.d(TAG, "Received $asString")
        val asJson = JSONObject(asString)

        val flattened = mutableMapOf<String, String>()
        // TODO try to share with WebsocketManager
        if (asJson.has("data")) {
            val data = try {
                asJson.getJSONObject("data")
            } catch (e: Exception) {
                JSONObject()
            }
            for (key in data.keys()) {
                if (key == "actions" && data.get(key) is JSONArray) {
                    val actions = data.getJSONArray("actions")
                    for (i in 0 until actions.length()) {
                        if (actions.get(i) is JSONObject) {
                            val action = actions.getJSONObject(i)
                            flattened["action_${i + 1}_key"] = if (action.has("action")) action.get("action").toString() else ""
                            flattened["action_${i + 1}_title"] = if (action.has("title")) action.get("title").toString() else ""
                            flattened["action_${i + 1}_uri"] = if (action.has("uri")) action.get("uri").toString() else ""
                        }
                    }
                } else {
                    flattened[key] = data.get(key).toString()
                }
            }
        }
        // Message and title are in the root unlike all the others.
        listOf("message", "title").forEach { key ->
            if (asJson.has(key)) {
                flattened[key] = asJson.getString(key)
            }
        }
        if (asJson.has("registration_info")) {
            flattened["webhook_id"] = asJson.getJSONObject("registration_info").getString("webhook_id")
        }

        messagingManager.handleMessage(flattened, SOURCE)
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        mainScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Refreshed endpoint: $endpoint")
            // TODO store endpoint for future reference
            if (!serverManager.isRegistered()) {
                Log.d(TAG, "Not trying to update registration since we aren't authenticated.")
                return@launch
            }
            serverManager.defaultServers.forEach {
                launch {
                    try {
                        serverManager.integrationRepository(it.id).updateRegistration(
                            deviceRegistration = DeviceRegistration(pushUrl = endpoint, pushToken = ""),
                            allowReregistration = false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Issue updating registration", e)
                    }
                }
            }
        }
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        // TODO
    }

    override fun onUnregistered(context: Context, instance: String) {
        // TODO
    }
}
