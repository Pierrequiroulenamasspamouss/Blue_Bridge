package com.jowell.wellmonitoring.data

package com.jowell.wellmonitoring.data

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

object WellDataSerializer : KSerializer<WellData> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WellData")

    override fun deserialize(decoder: Decoder): WellData {
        val input = decoder as? JsonDecoder
            ?: throw IllegalStateException("Expected JsonDecoder")

        val jsonObject = input.decodeJsonElement().jsonObject

        val knownFields = listOf(
            "id", "wellName", "wellOwner", "wellLocation", "wellWaterType",
            "wellCapacity", "wellWaterLevel", "wellWaterConsumption",
            "espId", "ipAddress"
        )

        val well = WellData(
            id = jsonObject["id"]?.jsonPrimitive?.intOrNull ?: 0,
            wellName = jsonObject["wellName"]?.jsonPrimitive?.contentOrNull ?: "",
            wellOwner = jsonObject["wellOwner"]?.jsonPrimitive?.contentOrNull ?: "",
            wellLocation = jsonObject["wellLocation"]?.jsonPrimitive?.contentOrNull ?: "",
            wellWaterType = jsonObject["wellWaterType"]?.jsonPrimitive?.contentOrNull ?: "",
            wellCapacity = jsonObject["wellCapacity"]?.jsonPrimitive?.intOrNull ?: 0,
            wellWaterLevel = jsonObject["wellWaterLevel"]?.jsonPrimitive?.intOrNull ?: 0,
            wellWaterConsumption = jsonObject["wellWaterConsumption"]?.jsonPrimitive?.intOrNull ?: 0,
            espId = jsonObject["espId"]?.jsonPrimitive?.contentOrNull ?: "",
            ipAddress = jsonObject["ipAddress"]?.jsonPrimitive?.contentOrNull ?: "",
            extraData = jsonObject.filterKeys { it !in knownFields }
        )
        return well
    }

    override fun serialize(encoder: Encoder, value: WellData) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalStateException("Expected JsonEncoder")

        val obj = buildJsonObject {
            put("id", JsonPrimitive(value.id))
            put("wellName", JsonPrimitive(value.wellName))
            put("wellOwner", JsonPrimitive(value.wellOwner))
            put("wellLocation", JsonPrimitive(value.wellLocation))
            put("wellWaterType", JsonPrimitive(value.wellWaterType))
            put("wellCapacity", JsonPrimitive(value.wellCapacity))
            put("wellWaterLevel", JsonPrimitive(value.wellWaterLevel))
            put("wellWaterConsumption", JsonPrimitive(value.wellWaterConsumption))
            put("espId", JsonPrimitive(value.espId))
            put("ipAddress", JsonPrimitive(value.ipAddress))
            value.extraData.forEach { put(it.key, it.value) }
        }

        jsonEncoder.encodeJsonElement(obj)
    }
}
