package io.homeassistant.companion.android.data.integration

import io.homeassistant.companion.android.data.LocalStorage
import io.homeassistant.companion.android.domain.authentication.AuthenticationRepository
import io.homeassistant.companion.android.domain.integration.DeviceRegistration
import io.homeassistant.companion.android.domain.integration.Entity
import io.homeassistant.companion.android.domain.integration.IntegrationRepository
import io.homeassistant.companion.android.domain.integration.UpdateLocation
import io.homeassistant.companion.android.domain.integration.ZoneAttributes
import javax.inject.Inject
import javax.inject.Named
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class IntegrationRepositoryImpl @Inject constructor(
    private val integrationService: IntegrationService,
    private val authenticationRepository: AuthenticationRepository,
    @Named("integration") private val localStorage: LocalStorage
) : IntegrationRepository {

    companion object {
        private const val PREF_CLOUD_URL = "cloud_url"
        private const val PREF_REMOTE_UI_URL = "remote_ui_url"
        private const val PREF_SECRET = "secret"
        private const val PREF_WEBHOOK_ID = "webhook_id"

        private const val PREF_ZONE_ENABLED = "zone_enabled"
        private const val PREF_BACKGROUND_ENABLED = "background_enabled"
    }

    override suspend fun registerDevice(deviceRegistration: DeviceRegistration) {
        val response =
            integrationService.registerDevice(
                authenticationRepository.buildBearerToken(),
                createRegisterDeviceRequest(deviceRegistration)
            )
        persistDeviceRegistrationResponse(response)
    }

    override suspend fun updateRegistration(deviceRegistration: DeviceRegistration) {
        val request = IntegrationRequest("update_registration", createRegisterDeviceRequest(deviceRegistration))
        for (it in getUrls()) {
            try {
                if (integrationService.updateRegistration(it, request).isSuccessful) {
                    return
                }
            } catch (e: Exception) {
                // Ignore failure until we are out of URLS to try!
            }
        }

        throw IntegrationException()
    }

    private suspend fun persistDeviceRegistrationResponse(response: RegisterDeviceResponse) {
        localStorage.putString(PREF_CLOUD_URL, response.cloudhookUrl)
        localStorage.putString(PREF_REMOTE_UI_URL, response.remoteUiUrl)
        localStorage.putString(PREF_SECRET, response.secret)
        localStorage.putString(PREF_WEBHOOK_ID, response.webhookId)
    }

    override suspend fun isRegistered(): Boolean {
        return localStorage.getString(PREF_WEBHOOK_ID) != null
    }

    override suspend fun updateLocation(updateLocation: UpdateLocation) {
        val updateLocationRequest = createUpdateLocation(updateLocation)
        for (it in getUrls()) {
            var wasSuccess = false
            try {
                wasSuccess =
                    integrationService.updateLocation(it, updateLocationRequest).isSuccessful
            } catch (e: Exception) {
                // Ignore failure until we are out of URLS to try!
            }
            // if we had a successful call we can return
            if (wasSuccess)
                return
        }

        throw IntegrationException()
    }

    override suspend fun getZones(): Array<Entity<ZoneAttributes>> {
        val getZonesRequest = IntegrationRequest("get_zones", null)
        var zones: Array<EntityResponse<ZoneAttributes>>? = null
        for (it in getUrls()) {
            try {
                zones = integrationService.getZones(it, getZonesRequest)
            } catch (e: Exception) {
                // Ignore failure until we are out of URLS to try!
            }

            if (zones != null) {
                return createZonesResponse(zones)
            }
        }

        throw IntegrationException()
    }

    override suspend fun setZoneTrackingEnabled(enabled: Boolean) {
        localStorage.putBoolean(PREF_ZONE_ENABLED, enabled)
    }

    override suspend fun isZoneTrackingEnabled(): Boolean {
        return localStorage.getBoolean(PREF_ZONE_ENABLED)
    }

    override suspend fun setBackgroundTrackingEnabled(enabled: Boolean) {
        localStorage.putBoolean(PREF_BACKGROUND_ENABLED, enabled)
    }

    override suspend fun isBackgroundTrackingEnabled(): Boolean {
        return localStorage.getBoolean(PREF_BACKGROUND_ENABLED)
    }

    // https://developers.home-assistant.io/docs/en/app_integration_sending_data.html#short-note-on-instance-urls
    private suspend fun getUrls(): Array<HttpUrl> {
        val retVal = ArrayList<HttpUrl>()
        val webhook = localStorage.getString(PREF_WEBHOOK_ID)

        localStorage.getString(PREF_CLOUD_URL)?.let {
            retVal.add(it.toHttpUrl())
        }

        localStorage.getString(PREF_REMOTE_UI_URL)?.let {
            retVal.add(
                it.toHttpUrl().newBuilder()
                    .addPathSegments("api/webhook/$webhook")
                    .build()
            )
        }

        authenticationRepository.getUrl().toString().let {
            retVal.add(
                it.toHttpUrl().newBuilder()
                    .addPathSegments("api/webhook/$webhook")
                    .build()
            )
        }

        return retVal.toTypedArray()
    }

    private fun createRegisterDeviceRequest(deviceRegistration: DeviceRegistration): RegisterDeviceRequest {
        return RegisterDeviceRequest(
            deviceRegistration.appId,
            deviceRegistration.appName,
            deviceRegistration.appVersion,
            deviceRegistration.deviceName,
            deviceRegistration.manufacturer,
            deviceRegistration.model,
            deviceRegistration.osName,
            deviceRegistration.osVersion,
            deviceRegistration.supportsEncryption,
            deviceRegistration.appData
        )
    }

    private fun createUpdateLocation(updateLocation: UpdateLocation): IntegrationRequest {
        return IntegrationRequest(
            "update_location",
            UpdateLocationRequest(
                updateLocation.locationName,
                updateLocation.gps,
                updateLocation.gpsAccuracy,
                updateLocation.battery,
                updateLocation.speed,
                updateLocation.altitude,
                updateLocation.course,
                updateLocation.verticalAccuracy
            )
        )
    }

    private fun createZonesResponse(zones: Array<EntityResponse<ZoneAttributes>>): Array<Entity<ZoneAttributes>> {
        val retVal = ArrayList<Entity<ZoneAttributes>>()
        zones.forEach {
            retVal.add(
                Entity<ZoneAttributes>(
                    it.entityId,
                    it.state,
                    it.attributes,
                    it.lastChanged,
                    it.lastUpdated,
                    it.context
                )
            )
        }

        return retVal.toTypedArray()
    }
}
