package io.homeassistant.companion.android.common.data.integration.impl.entities

import com.fasterxml.jackson.databind.JsonNode
import java.util.Calendar

data class EntityResponse(
    val entityId: String,
    val state: String,
    val attributes: JsonNode,
    val lastChanged: Calendar,
    val lastUpdated: Calendar,
    val context: Map<String, Any>
) {
    val domain: String
        get() = this.entityId.split(".")[0]
}
