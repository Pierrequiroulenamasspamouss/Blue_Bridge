package com.bluebridgeapp.bluebridge.data.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    var role: String = "",
    val location: Location = Location(0.0, 0.0),
    val phoneNumber: String? = null,
    val themePreference: Int = 0,
    val lastLogin: String? = null,
    val waterNeeds: List<WaterNeed> = emptyList(),
    val movementSpeeds: MovementSpeeds = MovementSpeeds(),
    val loginToken: String? = null, // Used for ensuring a proper login validity, set on login/register and checked for session validity
    val userId: String = "",
    val languageCode: String = "en"
) : Flow<UserData?> {
    override suspend fun collect(collector: FlowCollector<UserData?>) {
        collector.emit(this)
    }
}


