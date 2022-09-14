package io.homeassistant.companion.android.controls

import android.content.Context
import android.os.Build
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.actions.ModeAction
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.TemperatureControlTemplate
import androidx.annotation.RequiresApi
import io.homeassistant.companion.android.common.data.integration.ClimateAttributes
import io.homeassistant.companion.android.common.data.integration.Entity
import io.homeassistant.companion.android.common.data.integration.EntityAttributes
import io.homeassistant.companion.android.common.data.integration.IntegrationRepository
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.R as commonR

@RequiresApi(Build.VERSION_CODES.R)
object ClimateControl : HaControl {
    private const val SUPPORT_TARGET_TEMPERATURE = 1
    private const val SUPPORT_TARGET_TEMPERATURE_RANGE = 2
    private val temperatureControlModes = mapOf(
        "cool" to TemperatureControlTemplate.MODE_COOL,
        "heat" to TemperatureControlTemplate.MODE_HEAT,
        "heat_cool" to TemperatureControlTemplate.MODE_HEAT_COOL,
        "off" to TemperatureControlTemplate.MODE_OFF
    )
    private val temperatureControlModeFlags = mapOf(
        "cool" to TemperatureControlTemplate.FLAG_MODE_COOL,
        "heat" to TemperatureControlTemplate.FLAG_MODE_HEAT,
        "heat_cool" to TemperatureControlTemplate.FLAG_MODE_HEAT_COOL,
        "off" to TemperatureControlTemplate.FLAG_MODE_OFF
    )

    override fun provideControlFeatures(
        context: Context,
        control: Control.StatefulBuilder,
        entity: Entity<EntityAttributes>,
        area: AreaRegistryResponse?,
        baseUrl: String?
    ): Control.StatefulBuilder {
        control.setStatusText(
            when (entity.state) {
                "auto" -> context.getString(commonR.string.state_auto)
                "cool" -> context.getString(commonR.string.state_cool)
                "dry" -> context.getString(commonR.string.state_dry)
                "fan_only" -> context.getString(commonR.string.state_fan_only)
                "heat" -> context.getString(commonR.string.state_heat)
                "heat_cool" -> context.getString(commonR.string.state_heat_cool)
                "off" -> context.getString(commonR.string.state_off)
                "unavailable" -> context.getString(commonR.string.state_unavailable)
                else -> entity.state
            }
        )
        val minValue = (entity.attributes as ClimateAttributes).minTemp?.toFloat() ?: 0f
        val maxValue = (entity.attributes as ClimateAttributes).maxTemp?.toFloat() ?: 100f
        var currentValue = (entity.attributes as ClimateAttributes).temperature?.toFloat() ?: (
            (entity.attributes as ClimateAttributes).currentTemperature
            )?.toFloat() ?: 0f
        // Ensure the current value is never lower than the minimum or higher than the maximum
        if (currentValue < minValue)
            currentValue = minValue
        if (currentValue > maxValue)
            currentValue = maxValue

        val temperatureUnit = (entity.attributes as ClimateAttributes).temperatureUnit ?: ""
        val temperatureStepSize = (entity.attributes as ClimateAttributes).targetTemperatureStep?.toFloat()
            ?: when (temperatureUnit) {
                "°C" -> 0.5f
                else -> 1f
            }
        val temperatureFormatSize = if (temperatureStepSize < 1f) "1" else "0"
        val rangeTemplate = RangeTemplate(
            entity.entityId,
            minValue,
            maxValue,
            currentValue,
            temperatureStepSize,
            "%.${temperatureFormatSize}f $temperatureUnit"
        )
        if (entityShouldBePresentedAsThermostat(entity)) {
            var modesFlag = 0
            (entity.attributes as ClimateAttributes).hvacModes?.forEach {
                modesFlag = modesFlag or temperatureControlModeFlags[it]!!
            }
            control.setControlTemplate(
                TemperatureControlTemplate(
                    entity.entityId,
                    rangeTemplate,
                    temperatureControlModes[entity.state]!!,
                    temperatureControlModes[entity.state]!!,
                    modesFlag
                )
            )
        } else {
            control.setControlTemplate(rangeTemplate)
        }

        return control
    }

    override fun getDeviceType(entity: Entity<EntityAttributes>): Int =
        if (entityShouldBePresentedAsThermostat(entity))
            DeviceTypes.TYPE_THERMOSTAT
        else
            DeviceTypes.TYPE_AC_HEATER

    override fun getDomainString(context: Context, entity: Entity<EntityAttributes>): String =
        context.getString(commonR.string.domain_climate)

    override suspend fun performAction(
        integrationRepository: IntegrationRepository,
        action: ControlAction
    ): Boolean {
        return when (action) {
            is FloatAction -> {
                integrationRepository.callService(
                    action.templateId.split(".")[0],
                    "set_temperature",
                    hashMapOf(
                        "entity_id" to action.templateId,
                        "temperature" to (action as? FloatAction)?.newValue.toString()
                    )
                )
                true
            }
            is ModeAction -> {
                integrationRepository.callService(
                    action.templateId.split(".")[0],
                    "set_hvac_mode",
                    hashMapOf(
                        "entity_id" to action.templateId,
                        "hvac_mode" to (
                            temperatureControlModes.entries.find {
                                it.value == ((action as? ModeAction)?.newMode ?: -1)
                            }?.key ?: ""
                            )
                    )
                )
                true
            }
            else -> {
                false
            }
        }
    }

    private fun entityShouldBePresentedAsThermostat(entity: Entity<EntityAttributes>): Boolean {
        return temperatureControlModes.containsKey(entity.state) &&
            ((entity.attributes as ClimateAttributes).hvacModes?.isNotEmpty() == true) &&
            ((entity.attributes as ClimateAttributes).hvacModes?.any { it == entity.state } == true) &&
            ((entity.attributes as ClimateAttributes).hvacModes?.all { temperatureControlModes.containsKey(it) } == true) &&
            (
                (entity.attributes as ClimateAttributes).supportedFeatures != null &&
                    (
                        ((entity.attributes as ClimateAttributes).supportedFeatures!! and SUPPORT_TARGET_TEMPERATURE == SUPPORT_TARGET_TEMPERATURE) ||
                            ((entity.attributes as ClimateAttributes).supportedFeatures!! and SUPPORT_TARGET_TEMPERATURE_RANGE == SUPPORT_TARGET_TEMPERATURE_RANGE)
                        )

                )
    }
}
