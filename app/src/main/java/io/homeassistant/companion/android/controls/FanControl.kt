package io.homeassistant.companion.android.controls

import android.content.Context
import android.os.Build
import android.service.controls.Control
import android.service.controls.DeviceTypes
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.actions.FloatAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.RangeTemplate
import android.service.controls.templates.ToggleRangeTemplate
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import io.homeassistant.companion.android.common.data.integration.*
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AreaRegistryResponse
import io.homeassistant.companion.android.common.R as commonR

@Suppress("UNCHECKED_CAST")
@RequiresApi(Build.VERSION_CODES.R)
object FanControl : HaControl {
    override fun provideControlFeatures(
        context: Context,
        control: Control.StatefulBuilder,
        entity: Entity<EntityAttributes>,
        area: AreaRegistryResponse?,
        baseUrl: String?
    ): Control.StatefulBuilder {
        if ((entity as Entity<FanAttributes>).supportsFanSetSpeed()) {
            val position = entity.getFanSpeed()
            control.setControlTemplate(
                ToggleRangeTemplate(
                    entity.entityId,
                    entity.state == "on",
                    "",
                    RangeTemplate(
                        entity.entityId,
                        position?.min ?: 0f,
                        position?.max ?: 100f,
                        position?.value ?: 0f,
                        1f,
                        "%.0f%%"
                    )
                )
            )
        } else {
            control.setControlTemplate(
                ToggleTemplate(
                    entity.entityId,
                    ControlButton(
                        entity.state == "on",
                        ""
                    )
                )
            )
        }
        return control
    }

    override fun getDeviceType(entity: Entity<EntityAttributes>): Int =
        DeviceTypes.TYPE_FAN

    override fun getDomainString(context: Context, entity: Entity<EntityAttributes>): String =
        context.getString(commonR.string.domain_fan)

    override suspend fun performAction(
        integrationRepository: IntegrationRepository,
        action: ControlAction
    ): Boolean {
        when (action) {
            is BooleanAction -> {
                integrationRepository.callService(
                    action.templateId.split(".")[0],
                    if (action.newState) "turn_on" else "turn_off",
                    hashMapOf("entity_id" to action.templateId)
                )
            }
            is FloatAction -> {
                val convertPercentage = action.newValue
                integrationRepository.callService(
                    action.templateId.split(".")[0],
                    "set_percentage",
                    hashMapOf(
                        "entity_id" to action.templateId,
                        "percentage" to convertPercentage.toInt()
                    )
                )
            }
        }
        return true
    }
}
