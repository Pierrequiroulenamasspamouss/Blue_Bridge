package com.wellconnect.wellmonitoring.data

open class WellConfigEvents {
    data class SaveWell(val wellId: Int) : WellConfigEvents()
    data class WellNameEntered(val wellName: String) : WellConfigEvents()
    data class OwnerEntered(val wellOwner: String) : WellConfigEvents()
    data class WellLocationEntered(val wellLocation: String) : WellConfigEvents()
    data class WaterTypeEntered(val wellWaterType: String) : WellConfigEvents()
    data class WellCapacityEntered(val wellCapacity: String) : WellConfigEvents()
    data class WaterLevelEntered(val wellWaterLevel: String) : WellConfigEvents()
    data class ConsumptionEntered(val wellWaterConsumption: String) : WellConfigEvents()
    data class EspIdEntered(val espId: String) : WellConfigEvents()


}
