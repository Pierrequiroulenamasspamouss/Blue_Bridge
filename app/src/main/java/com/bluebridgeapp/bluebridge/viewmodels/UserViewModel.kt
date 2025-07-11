package com.bluebridgeapp.bluebridge.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.model.DeleteAccountRequest
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.LoginRequest
import com.bluebridgeapp.bluebridge.data.model.RegisterRequest
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WaterNeed
import com.bluebridgeapp.bluebridge.events.UserEvent
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {
    // UI State
    private val _state = mutableStateOf<UiState<UserData>>(UiState.Empty)
    val state = _state
    private val _currentTheme = MutableStateFlow(0)
    private val _currentLanguage = MutableStateFlow("en")
    val currentTheme: StateFlow<Int> = _currentTheme.asStateFlow()
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    private val _notificationsEnabled = mutableStateOf(false)

    init {
        loadLanguagePreference()
        loadThemePreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            try {
                val savedTheme = repository.getTheme()
                _currentTheme.value = savedTheme
            } catch (_: Exception) {
                _currentTheme.value = 0
            }
        }
    }
    private fun loadLanguagePreference() {
        viewModelScope.launch {
            try {
                val savedLanguage = repository.getLanguage()
                _currentLanguage.value = savedLanguage
            } catch (_: Exception) {
                _currentLanguage.value = "en"
            }
        }
    }
    fun updateLanguage(language: String) {
        viewModelScope.launch {
            repository.setLanguage(language)
            loadLanguagePreference()
        }
    }
    fun updateTheme(themeValue: Int) {
        viewModelScope.launch {
            try {
                repository.saveThemePreference(themeValue)
                _currentTheme.value = themeValue
            } catch (_: Exception) {}
        }
    }
    suspend fun handleEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Login -> login(event.request)
            is UserEvent.Register -> register(event.request)
            is UserEvent.LoadUser -> loadUser()
            is UserEvent.UpdateProfile -> updateProfile(event.userData)
            is UserEvent.UpdateLocation -> updateLocation(event.location)
            is UserEvent.UpdateWaterNeeds -> updateWaterNeeds(event.waterNeeds)
            is UserEvent.UpdateNotificationsEnabled -> setNotificationsEnabled(event.enabled)
            UserEvent.Logout -> logout()
            UserEvent.LoginAsGuest -> loginAsGuest()
        }
    }
    private fun updateLocation(location: Location) {
        viewModelScope.launch {
            repository.updateLocation(location)
            loadUser()
        }
    }
    fun loadUser() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val userData = repository.getUserData().first()
                if (userData != null) {
                    _state.value = UiState.Success(userData)
                    checkNotificationsEnabled()
                } else {
                    _state.value = UiState.Error("No user data available")
                }
            } catch (e: Exception) {
                _state.value = UiState.Error("Failed to load user: ${e.message}")
            }
        }
    }
    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val success = repository.register(request)
                if (success == "true") {
                    loadUser()
                    registerForNotifications()
                } else {
                    _state.value = UiState.Error(success)
                }
            } catch (e: Exception) {
                _state.value = UiState.Error("Registration failed. Please try again later.")
            }
        }
    }
    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val success = repository.login(request)
                if (success) {
                    loadUser()
                    registerForNotifications()
                } else {
                    _state.value = UiState.Error("Login failed. Please check your credentials.")
                }
            } catch (e: Exception) {
                _state.value = UiState.Error("Login failed: ${e.message}")
            }
        }
    }
    fun loginAsGuest() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val guestUserData = UserData(
                    email = "guest_${System.currentTimeMillis()}@bluebridge.app",
                    firstName = "Guest",
                    lastName = "User",
                    username = "guest_user",
                    role = "guest",
                    userId = "guest-UID",
                    loginToken = "1234567890",
                    themePreference = 0,
                    languageCode = "en"
                )
                repository.saveUserData(guestUserData)
                _state.value = UiState.Success(guestUserData)
            } catch (e: Exception) {
                _state.value = UiState.Error("Guest login error: ${e.message}")
            }
        }
    }
    fun logout() {
        viewModelScope.launch {
            try {
                repository.logout()
                _state.value = UiState.Error("Logged out")
            } catch (e: Exception) {
                _state.value = UiState.Error("Logout failed: ${e.message}")
            }
        }
    }
    suspend fun deleteAccount(email: String, password: String): Boolean {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val token = repository.getLoginToken() ?: ""
                val deleteRequest = DeleteAccountRequest(
                    userId = repository.getUserId() ?: "",
                    password = password,
                    email = email,
                    loginToken = token
                )
                if (_notificationsEnabled.value) {
                    unregisterFromNotifications()
                }
                val success = repository.deleteAccount(deleteRequest)
                if (success) {
                    repository.logout()
                    _state.value = UiState.Empty
                } else {
                    _state.value = UiState.Error("Account deletion failed. Please check your credentials and try again.")
                }
            } catch (e: Exception) {
                _state.value = UiState.Error("Account deletion error: ${e.message}")
            }
        }.join()
        return _state.value is UiState.Empty
    }
    private fun updateProfile(userData: UserData) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                repository.updateProfileOnServer(userData)
                repository.saveUserData(userData)
                loadUser()
            } catch (e: Exception) {
                _state.value = UiState.Error("Failed to update profile: ${e.message}")
            }
        }
    }
    suspend fun getUserEmail(): String? {
        return repository.getUserEmail()
    }
    fun setNotificationsEnabled(enable: Boolean) {
        viewModelScope.launch {
            try {
                repository.setNotificationsEnabled(enable)
                _notificationsEnabled.value = enable
                if (enable) {
                    registerForNotifications()
                } else {
                    unregisterFromNotifications()
                }
            } catch (_: Exception) {}
        }
    }
    private fun checkNotificationsEnabled() {
        viewModelScope.launch {
            try {
                val enabled = repository.areNotificationsEnabled()
                _notificationsEnabled.value = enabled
            } catch (_: Exception) {}
        }
    }
    fun registerForNotifications() {
        viewModelScope.launch {
            try {
                var token = repository.getNotificationToken()
                if (token.isNullOrEmpty()) {
                    token = getFirebaseToken()
                    if (token.isNullOrEmpty()) return@launch
                }
                repository.saveNotificationToken(token)
                val email = repository.getUserEmail()
                val authToken = repository.getLoginToken()
                val userId = repository.getUserId()
                if (email != null && authToken != null) {
                    repository.registerNotificationToken(userId, authToken, token)
                }
            } catch (_: Exception) {}
        }
    }
    private fun unregisterFromNotifications() {
        viewModelScope.launch {
            try {
                val token = repository.getNotificationToken()
                if (token.isNullOrEmpty()) return@launch
                val email = repository.getUserEmail()
                val authToken = repository.getLoginToken()
                val userId = repository.getUserId()
                if (email != null && authToken != null) {
                    repository.unregisterNotificationToken(userId, authToken, token)
                }
                repository.clearNotificationToken()
            } catch (_: Exception) {}
        }
    }
    private suspend fun getFirebaseToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            null
        }
    }
    suspend fun updateWaterNeeds(waterNeeds: List<WaterNeed>) {
        try {
            val userData = repository.getUserData().first()
            if (userData?.role != "guest") {
                repository.updateWaterNeedsOnServer(waterNeeds)
            }
            val userDataFlow = repository.getUserData()
            val currentUserData = userDataFlow.first()
            val updatedUserData = currentUserData?.copy(waterNeeds = waterNeeds) ?: return
            repository.saveUserData(updatedUserData)
        } catch (_: Exception) {}
    }
    suspend fun getLoginToken(): String? = repository.getLoginToken()
    suspend fun getUserId(): String? = repository.getUserId()
    suspend fun getRole(): String? = repository.getRole()
    suspend fun isLoggedIn(): Boolean = repository.isLoggedIn()
    suspend fun getRoleValue(): Int = repository.getRoleValue()
    suspend fun getTheme(): Int = repository.getTheme()
    suspend fun getLanguage(): String = repository.getLanguage()
    suspend fun getUserData(): Flow<UserData?> = repository.getUserData()
}
