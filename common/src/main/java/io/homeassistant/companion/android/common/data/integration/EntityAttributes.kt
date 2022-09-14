package io.homeassistant.companion.android.common.data.integration

import com.fasterxml.jackson.core.type.TypeReference

sealed interface EntityAttributes {
    val friendlyName: String?
    val icon: String?
    val state: String?
}

sealed interface SliderAttributes {
    val min: Number?
    val max: Number?
    val step: Number?
}

fun entityAttributesClassForDomain(domain: String): TypeReference<out EntityAttributes> = when (domain) {
    "button" -> object : TypeReference<ButtonAttributes>() {}
    "camera" -> object : TypeReference<CameraAttributes>() {}
    "climate" -> object : TypeReference<ClimateAttributes>() {}
    "cover" -> object : TypeReference<CoverAttributes>() {}
    "fan" -> object : TypeReference<FanAttributes>() {}
    "input_datetime" -> object : TypeReference<InputDateTimeAttributes>() {}
    "input_number" -> object : TypeReference<InputNumberAttributes>() {}
    "light" -> object : TypeReference<LightAttributes>() {}
    "media_player" -> object : TypeReference<InputNumberAttributes>() {}
    "switch" -> object : TypeReference<SwitchAttributes>() {}
    "vacuum" -> object : TypeReference<VacuumAttributes>() {}
    "zone" -> object : TypeReference<ZoneAttributes>() {}
    else -> object : TypeReference<DefaultEntityAttributes>() {}
}

/**
 * A data class to use for entities which do not have a domain-specific data class.
 */
data class DefaultEntityAttributes(
    override val friendlyName: String? = null,
    override val icon: String? = null,
    override val state: String? = null
) : EntityAttributes

data class ButtonAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val deviceClass: String?
) : EntityAttributes

data class CameraAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val entityPicture: String?
) : EntityAttributes

data class ClimateAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val temperature: Number?,
    val currentTemperature: Number?,
    val minTemp: Number?,
    val maxTemp: Number?,
    val temperatureUnit: String?,
    val targetTemperatureStep: Number?,
    val hvacModes: List<String>?,
    val supportedFeatures: Int?
) : EntityAttributes

data class CoverAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val currentPosition: Number?,
    val supportedFeatures: Int?,
    val deviceClass: String?
) : EntityAttributes

data class FanAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val percentage: Number?,
    val percentageStep: Double?,
    val supportedFeatures: Int?
) : EntityAttributes

data class InputDateTimeAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val hasDate: Boolean?,
    val hasTime: Boolean?
) : EntityAttributes

data class InputNumberAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    override val min: Number?,
    override val max: Number?,
    override val step: Number?
) : EntityAttributes, SliderAttributes

data class LightAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val brightness: Number?,
    val rgbColor: List<Int>?,
    val supportedFeatures: Int?,
    val supportedColorModes: List<String>?
) : EntityAttributes

data class MediaPlayerAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val mediaArtist: String?,
    val mediaAlbumArtist: String?,
    val mediaAlbumName: String?,
    val mediaTitle: String?,
    val mediaPosition: Double?,
    val entityPicture: String?
) : EntityAttributes

data class SwitchAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val deviceClass: String?
) : EntityAttributes

data class VacuumAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val supportedFeatures: Int?
) : EntityAttributes

data class ZoneAttributes(
    override val friendlyName: String?,
    override val icon: String?,
    override val state: String?,
    val hidden: Boolean,
    val latitude: Double,
    val longitude: Double,
    val radius: Float
) : EntityAttributes
