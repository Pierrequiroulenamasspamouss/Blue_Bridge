package com.bluebridge.bluebridgeapp.events

import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import kotlinx.coroutines.channels.Channel

open class WellEvents {
    data class SaveWell(val wellId: Int) : WellEvents()
    data class WellNameEntered(val wellName: String) : WellEvents()
    data class OwnerEntered(val wellOwner: String) : WellEvents()
    data class WellLocationEntered(val wellLocation: Location) : WellEvents()
    data class WaterTypeEntered(val wellWaterType: String) : WellEvents()
    data class WellCapacityEntered(val wellCapacity: String) : WellEvents()
    data class WaterLevelEntered(val wellWaterLevel: String) : WellEvents()
    data class ConsumptionEntered(val wellWaterConsumption: String) : WellEvents()
    data class EspIdEntered(val espId: String) : WellEvents()
}

sealed class BrowseWellsEvent {
    data class UpdateSearchQuery(val query: String) : BrowseWellsEvent()
    data class UpdateWaterTypeFilter(val waterType: String?) : BrowseWellsEvent()
    data class UpdateStatusFilter(val status: String?) : BrowseWellsEvent()
    object ResetFilters : BrowseWellsEvent()
    object RefreshFilteredWells : BrowseWellsEvent()
}


open class UserEvent {
    data class LoadUser(val userId: String) : UserEvent()
    data class Login(val request: LoginRequest) : UserEvent()
    data class Register(val request: RegisterRequest) : UserEvent()
    data class UpdateProfile(val userData: UserData) : UserEvent()
    data class UpdateLocation(val location: Location) : UserEvent()
    data class UpdateWaterNeeds(val waterNeeds: List<WaterNeed>) : UserEvent()
    data class UpdateNotificationsEnabled(val enabled: Boolean) : UserEvent()
    object Logout : UserEvent()
    object LoginAsGuest : UserEvent()
}

open class NearbyUserEvent {
    data class Refresh(val latitude: Double, val longitude: Double, val radius: Double) : NearbyUserEvent()
    data class SearchUser(val latitude: Double, val longitude: Double, val radius: Double): NearbyUserEvent()
    data class UpdateRadius(val radius: Double): NearbyUserEvent()
    data class ApplyFilters(val filters: Map<String, String>): NearbyUserEvent()
    object ResetFilters: NearbyUserEvent()
}
sealed class AppEvent {
    data class ShowSuccess(val message: String): AppEvent()
    data class ShowError(val message: String): AppEvent()
    data class ShowInfo(val message: String): AppEvent()
    data class LogError(val message: String): AppEvent()
    data class LogInfo(val message: String): AppEvent()
    data class LogSuccess(val message: String): AppEvent()
    data class SubmitBugReport(
        val name: String,
        val description: String,
        val category: String,
        val extra: Map<String, String> = emptyMap()
    ) : AppEvent()
}

object AppEventChannel {
    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    private lateinit var eventHandler: AppEventHandler

    fun initialize(handler: AppEventHandler) {
        this.eventHandler = handler
    }

    suspend fun sendEvent(event: AppEvent) {
        _events.send(event)
        eventHandler.handleEvent(event)
    }
}