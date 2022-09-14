package io.homeassistant.companion.android.common.data.integration

import android.graphics.Color
import android.util.Log
import java.util.Calendar
import kotlin.math.round

data class Entity<T>(
    val entityId: String,
    val state: String,
    val attributes: T,
    val lastChanged: Calendar,
    val lastUpdated: Calendar,
    val context: Map<String, Any>?
)

data class EntityPosition(
    val value: Float,
    val min: Float,
    val max: Float
)

object EntityExt {
    const val TAG = "EntityExt"

    const val FAN_SUPPORT_SET_SPEED = 1
    const val LIGHT_MODE_COLOR_TEMP = "color_temp"
    val LIGHT_MODE_NO_BRIGHTNESS_SUPPORT = listOf("unknown", "onoff")
    const val LIGHT_SUPPORT_BRIGHTNESS_DEPR = 1
    const val LIGHT_SUPPORT_COLOR_TEMP_DEPR = 2
}

val <T> Entity<T>.domain: String
    get() = this.entityId.split(".")[0]

fun Entity<CoverAttributes>.getCoverPosition(): EntityPosition? {
    // https://github.com/home-assistant/frontend/blob/dev/src/dialogs/more-info/controls/more-info-cover.ts#L33
    return try {
        if (domain != "cover" || attributes.currentPosition == null) return null

        val minValue = 0f
        val maxValue = 100f
        val currentValue = attributes.currentPosition.toFloat()

        EntityPosition(
            value = currentValue.coerceAtLeast(minValue).coerceAtMost(maxValue),
            min = minValue,
            max = maxValue
        )
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get getCoverPosition", e)
        null
    }
}

fun Entity<FanAttributes>.supportsFanSetSpeed(): Boolean {
    return try {
        if (domain != "fan") return false
        attributes.supportedFeatures != null &&
            attributes.supportedFeatures and EntityExt.FAN_SUPPORT_SET_SPEED == EntityExt.FAN_SUPPORT_SET_SPEED
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get supportsFanSetSpeed", e)
        false
    }
}

fun Entity<FanAttributes>.getFanSpeed(): EntityPosition? {
    // https://github.com/home-assistant/frontend/blob/dev/src/dialogs/more-info/controls/more-info-fan.js#L48
    return try {
        if (!supportsFanSetSpeed()) return null

        val minValue = 0f
        val maxValue = 100f
        val currentValue = attributes.percentage?.toFloat() ?: 0f

        EntityPosition(
            value = currentValue.coerceAtLeast(minValue).coerceAtMost(maxValue),
            min = minValue,
            max = maxValue
        )
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get getFanSpeed", e)
        null
    }
}

fun Entity<FanAttributes>.getFanSteps(): Int? {
    return try {
        if (!supportsFanSetSpeed()) return null

        fun calculateNumStep(percentageStep: Double): Int {
            val numSteps = round(100 / percentageStep).toInt()
            if (numSteps <= 10) return numSteps
            if (numSteps % 10 == 0) return 10
            return calculateNumStep(percentageStep * 2)
        }

        return calculateNumStep(attributes.percentageStep ?: 1.0) - 1
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get getFanSteps")
        null
    }
}

fun Entity<LightAttributes>.supportsLightBrightness(): Boolean {
    return try {
        if (domain != "light") return false

        // On HA Core 2021.5 and later brightness detection has changed
        // to simplify things in the app lets use both methods for now
        val supportsBrightness =
            if (attributes.supportedColorModes == null) false
            else (attributes.supportedColorModes - EntityExt.LIGHT_MODE_NO_BRIGHTNESS_SUPPORT).isNotEmpty()
        supportsBrightness ||
            (
                attributes.supportedFeatures != null &&
                    attributes.supportedFeatures and EntityExt.LIGHT_SUPPORT_BRIGHTNESS_DEPR == EntityExt.LIGHT_SUPPORT_BRIGHTNESS_DEPR
                )
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get supportsLightBrightness", e)
        false
    }
}

fun Entity<LightAttributes>.getLightBrightness(): EntityPosition? {
    // https://github.com/home-assistant/frontend/blob/dev/src/dialogs/more-info/controls/more-info-light.ts#L90
    return try {
        if (!supportsLightBrightness()) return null

        when (state) {
            "on" -> {
                val minValue = 0f
                val maxValue = 100f
                val currentValue =
                    attributes.brightness?.toFloat()?.div(255f)?.times(100)
                        ?: 0f

                EntityPosition(
                    value = currentValue.coerceAtLeast(minValue).coerceAtMost(maxValue),
                    min = minValue,
                    max = maxValue
                )
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get getLightBrightness", e)
        null
    }
}

fun Entity<LightAttributes>.supportsLightColorTemperature(): Boolean {
    return try {
        if (domain != "light") return false

        val supportsColorTemp = attributes.supportedColorModes?.contains(EntityExt.LIGHT_MODE_COLOR_TEMP) ?: false
        supportsColorTemp ||
            (
                attributes.supportedFeatures != null &&
                    attributes.supportedFeatures and EntityExt.LIGHT_SUPPORT_COLOR_TEMP_DEPR == EntityExt.LIGHT_SUPPORT_COLOR_TEMP_DEPR
                )
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get supportsLightColorTemperature", e)
        false
    }
}

fun Entity<LightAttributes>.getLightColor(): Int? {
    // https://github.com/home-assistant/frontend/blob/dev/src/panels/lovelace/cards/hui-light-card.ts#L243
    return try {
        if (domain != "light") return null

        when {
            state != "off" && attributes.rgbColor != null -> {
                val (r, g, b) = attributes.rgbColor
                Color.rgb(r, g, b)
            }
            else -> null
        }
    } catch (e: Exception) {
        Log.e(EntityExt.TAG, "Unable to get getLightColor", e)
        null
    }
}
