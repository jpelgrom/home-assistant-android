package io.homeassistant.companion.android.common.data.prefs.impl.entities

data class CloudPushConfig(
    /** Cloud push provider or `null` if the default should be used */
    val provider: String?,
    /** URL for cloud push provider or `null` if the value for the default provider should be used */
    val url: String?,
    /** Saved token for cloud push provider */
    val token: String?
) {
    val isUnifiedPush
        get() = provider != null && provider != CloudPushProvider.NONE.name && provider != CloudPushProvider.FCM.name
}

enum class CloudPushProvider {
    NONE,
    FCM
}
