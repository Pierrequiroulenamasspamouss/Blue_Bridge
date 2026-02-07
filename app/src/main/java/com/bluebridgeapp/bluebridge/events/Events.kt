package com.bluebridgeapp.bluebridge.events

import android.util.Log
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.LoginRequest
import com.bluebridgeapp.bluebridge.data.model.RegisterRequest
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WaterNeed
import com.bluebridgeapp.bluebridge.data.model.WellData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventChannel {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 50)
    val events = _events.asSharedFlow()

    private var eventHandler: AppEventHandler? = null

    fun initialize(handler: AppEventHandler) {
        this.eventHandler = handler
        Log.d("AppEventChannel", "EventHandler initialized")
    }

    fun setChatEventListener(listener: AppEventHandler.ChatEventListener) {
        eventHandler?.setChatEventListener(listener)
    }

    fun removeChatEventListener() {
        eventHandler?.removeChatEventListener()
    }

    suspend fun sendEvent(event: AppEvent) {
        Log.d("AppEventChannel", "Sending event: $event")
        try {
            // Emit to the shared flow for general subscribers
            _events.emit(event)

            // Also handle immediately with the event handler
            eventHandler?.handleEvent(event)
        } catch (e: Exception) {
            Log.e("AppEventChannel", "Error sending event: ${e.message}", e)
        }
    }
}
open class WellEvents {
    data class SaveWell(val wellData: WellData) : WellEvents()
    data class WellNameEntered(val wellName: String) : WellEvents()
    data class OwnerEntered(val wellOwner: String) : WellEvents()
    data class WellLocationEntered(val wellLocation: Location) : WellEvents()
    data class WaterTypeEntered(val wellWaterType: String) : WellEvents()
    data class WellCapacityEntered(val wellCapacity: String) : WellEvents()
    data class WaterLevelEntered(val wellWaterLevel: String) : WellEvents()
    data class ConsumptionEntered(val wellWaterConsumption: String) : WellEvents()
    data class WellIdEntered(val wellId: String) : WellEvents()
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

    // Chat-related events
    data class NewMessageReceived(val conversationId: String) : AppEvent()
    data class ConversationUpdated(val conversationId: String) : AppEvent()
    object RefreshAllConversations : AppEvent()
}