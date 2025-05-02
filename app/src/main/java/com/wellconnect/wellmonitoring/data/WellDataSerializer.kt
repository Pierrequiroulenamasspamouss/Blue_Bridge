package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object WellDataSerializer : KSerializer<WellData> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WellData")

    override fun deserialize(decoder: Decoder): WellData {
        val input = decoder as? JsonDecoder
            ?: throw IllegalStateException("Expected JsonDecoder")

        val jsonObject = input.decodeJsonElement().jsonObject

        val knownFields = listOf(
            "id", "wellName", "wellOwner", "wellLocation", "wellWaterType",
            "wellCapacity", "wellWaterLevel", "wellWaterConsumption",
            "espId", "lastRefreshTime", "wellStatus", "waterQuality"
        )
        val wellLocationJson = jsonObject["waterLocation"]?.jsonObject
        val wellLocation = if (wellLocationJson != null) {
            Location(
                latitude = wellLocationJson["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                longitude = wellLocationJson["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            )

        } else {
            // Try to get from extraData if it exists there
            val extraData = jsonObject.filterKeys { it !in knownFields }.toMap()
            if (extraData.containsKey("waterQuality")) {
                val extraWaterLocationJson = extraData["waterQuality"]?.jsonObject
                Location(
                    latitude = extraWaterLocationJson?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    longitude = extraWaterLocationJson?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                )

            } else {
                Location(
                    latitude = 0.0,
                    longitude = 0.0
                )
            }
        }
        // Parse water quality data
            val waterQualityJson = jsonObject["waterQuality"]?.jsonObject
        val waterQuality = if (waterQualityJson != null) {
            WaterQuality(
                ph = waterQualityJson["ph"]?.jsonPrimitive?.doubleOrNull ?: 7.0,
                turbidity = waterQualityJson["turbidity"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                tds = waterQualityJson["tds"]?.jsonPrimitive?.intOrNull ?: 0
            )
        } else {
            // Try to get from extraData if it exists there
            val extraData = jsonObject.filterKeys { it !in knownFields }.toMap()
            if (extraData.containsKey("waterQuality")) {
                val extraWaterQuality = extraData["waterQuality"]?.jsonObject
                WaterQuality(
                    ph = extraWaterQuality?.get("ph")?.jsonPrimitive?.doubleOrNull ?: 7.0,
                    turbidity = extraWaterQuality?.get("turbidity")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    tds = extraWaterQuality?.get("tds")?.jsonPrimitive?.intOrNull ?: 0
                )
            } else {
                WaterQuality()
            }
        }

        val well = WellData(
            id = jsonObject["id"]?.jsonPrimitive?.intOrNull ?: 0,
            wellName = jsonObject["wellName"]?.jsonPrimitive?.contentOrNull ?: "",
            wellOwner = jsonObject["wellOwner"]?.jsonPrimitive?.contentOrNull ?: "",
            wellLocation = wellLocation,
            wellWaterType = jsonObject["wellWaterType"]?.jsonPrimitive?.contentOrNull ?: "",
            wellCapacity = jsonObject["wellCapacity"]?.jsonPrimitive?.contentOrNull ?: "",
            wellWaterLevel = jsonObject["wellWaterLevel"]?.jsonPrimitive?.contentOrNull ?: "",
            wellWaterConsumption = jsonObject["wellWaterConsumption"]?.jsonPrimitive?.contentOrNull ?: "",
            espId = jsonObject["espId"]?.jsonPrimitive?.contentOrNull ?: "",
            lastRefreshTime = jsonObject["lastRefreshTime"]?.jsonPrimitive?.longOrNull ?: 0L,
            wellStatus = jsonObject["wellStatus"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
            waterQuality = waterQuality,
            extraData = jsonObject.filterKeys { it !in knownFields }.toMap()
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
            put("wellLocation", buildJsonObject {
                put("longitude", JsonPrimitive(value.wellLocation.longitude))
                put("latitude", JsonPrimitive(value.wellLocation.latitude))
            })
            put("wellWaterType", JsonPrimitive(value.wellWaterType))
            put("wellCapacity", JsonPrimitive(value.wellCapacity))
            put("wellWaterLevel", JsonPrimitive(value.wellWaterLevel))
            put("wellWaterConsumption", JsonPrimitive(value.wellWaterConsumption))
            put("lastRefreshTime", JsonPrimitive(value.lastRefreshTime))
            put("espId", JsonPrimitive(value.espId))
            put("wellStatus", JsonPrimitive(value.wellStatus))
            
            // Serialize water quality
            put("waterQuality", buildJsonObject {
                put("ph", JsonPrimitive(value.waterQuality.ph))
                put("turbidity", JsonPrimitive(value.waterQuality.turbidity))
                put("tds", JsonPrimitive(value.waterQuality.tds))
            })
            
            value.extraData.forEach { put(it.key, it.value) }
        }

        jsonEncoder.encodeJsonElement(obj)
    }
}
