package com.bluebridge.bluebridgeapp.data

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import com.bluebridge.bluebridgeapp.viewmodels.WellPickerViewModel.WellFilters

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

open class WellPickerEvent {
    data class Refresh(val context: Context, val snackbarHostState: SnackbarHostState) : WellPickerEvent()
    data class UpdateSearchQuery(val query: String) : WellPickerEvent()
    data class UpdateWaterTypeFilter(val waterType: String?) : WellPickerEvent()
    data class UpdateStatusFilter(val status: String?) : WellPickerEvent()
    object ResetFilters : WellPickerEvent()
}

open class UserEvent {
    data class UpdateTheme(val theme: Int) : UserEvent()
    data class LoadUser(val userId: String) : UserEvent()
    data class Login(val request: LoginRequest) : UserEvent()
    data class Register(val request: RegisterRequest) : UserEvent()
    data class UpdateProfile(val userData: UserData) : UserEvent()
    data class UpdateLocation(val location: Location) : UserEvent()
    data class UpdateWaterNeeds(val waterNeeds: List<WaterNeed>) : UserEvent()
    data class UpdateThemePreference(val themePreference: Int) : UserEvent()
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