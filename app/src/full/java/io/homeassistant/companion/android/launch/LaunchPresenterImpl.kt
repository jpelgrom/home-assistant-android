package io.homeassistant.companion.android.launch

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import io.homeassistant.companion.android.BuildConfig
import io.homeassistant.companion.android.common.data.integration.DeviceRegistration
import io.homeassistant.companion.android.common.data.prefs.PrefsRepository
import io.homeassistant.companion.android.common.data.prefs.impl.entities.CloudPushProvider
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.onboarding.getFirebaseMessagingToken
import javax.inject.Inject
import kotlinx.coroutines.launch

@ActivityScoped
class LaunchPresenterImpl @Inject constructor(
    @ActivityContext context: Context,
    serverManager: ServerManager,
    prefsRepository: PrefsRepository
) : LaunchPresenterBase(context as LaunchView, serverManager, prefsRepository) {
    override fun resyncRegistration() {
        if (!serverManager.isRegistered()) return
        ioScope.launch {
            val pushConfig = prefsRepository.getCloudPushConfig()
            val token = if (pushConfig.provider == null || pushConfig.provider == CloudPushProvider.FCM.name) {
                getFirebaseMessagingToken()
            } else {
                pushConfig.token
            }
            serverManager.defaultServers.forEach {
                launch {
                    try {
                        serverManager.integrationRepository(it.id).updateRegistration(
                            DeviceRegistration(
                                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                pushToken = token
                            )
                        )
                        serverManager.integrationRepository(it.id).getConfig() // Update cached data
                        serverManager.webSocketRepository(it.id).getCurrentUser() // Update cached data
                    } catch (e: Exception) {
                        Log.e(TAG, "Issue updating Registration", e)
                    }
                }
            }
        }
    }
}
