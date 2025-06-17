package com.bluebridge.bluebridgeapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.UserEvent
import com.bluebridge.bluebridgeapp.data.`interface`.UserRepository
import com.bluebridge.bluebridgeapp.data.model.DeleteAccountRequest
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(
    val repository: UserRepository,

) : ViewModel() {
    companion object;

    // State management
    private val _state = mutableStateOf<UiState<UserData>>(UiState.Empty)
    val state = _state
    private val _currentTheme = MutableStateFlow(0) // Default theme
    val currentTheme: StateFlow<Int> = _currentTheme.asStateFlow()

    // Push notification state
    private val _notificationsEnabled = mutableStateOf(false)

    init {
        // Load theme preference immediately on ViewModel creation
        loadThemePreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            try {
                // Load theme from SharedPreferences (not from user data)
                val savedTheme = repository.getTheme()
                _currentTheme.value = savedTheme
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to load theme preference", e)
                _currentTheme.value = 0 // Default to system theme
            }
        }
    }

    fun updateTheme(themeValue: Int) {
        viewModelScope.launch {
            try {
                // Save theme to SharedPreferences immediately
                repository.saveThemePreference(themeValue) // You'll need to implement this

                // Update the StateFlow to trigger recomposition
                _currentTheme.value = themeValue

                Log.d("UserViewModel", "Theme updated to: $themeValue")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to update theme", e)
            }
        }
    }
    suspend fun handleEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Login -> {
                login(event.request)
            }

            is UserEvent.Register -> {
                register(event.request)
            }

            is UserEvent.LoadUser -> {
                loadUser()
            }

            is UserEvent.UpdateProfile -> {
                updateProfile(event.userData)
            }

            is UserEvent.UpdateLocation -> {
                updateLocation(event.location)
            }

            is UserEvent.UpdateWaterNeeds -> {
                updateWaterNeeds(event.waterNeeds)
            }


            is UserEvent.UpdateNotificationsEnabled -> {
                setNotificationsEnabled(event.enabled)
            }

            UserEvent.Logout -> {
                logout()
            }

            UserEvent.LoginAsGuest -> {
                loginAsGuest()
            }
        }
    }


    private fun updateLocation(location: Location) {
        viewModelScope.launch {
            repository.updateLocation(location)
            loadUser()
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Attempting to load user data from preferences")
                val userData = repository.getUserData().first()
                if (userData != null) {
                    Log.d(
                        "UserViewModel",
                        "Successfully loaded user data: ${userData.email}, userId: ${userData.userId}"
                    )
                    _state.value = UiState.Success(userData)

                    // Check if notifications are enabled
                    checkNotificationsEnabled()
                } else {
                    Log.e("UserViewModel", "User data is null from preferences")
                    _state.value = UiState.Error("No user data available")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to load user: ${e.message}", e)
                _state.value = UiState.Error("Failed to load user: ${e.message}")
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Attempting registration for: ${request.email}")
                val success = repository.register(request)

                if (success) {
                    Log.d("UserViewModel", "Registration successful for: ${request.email}")
                    loadUser()
                    //navController.navigate(Routes.LOGIN_SCREEN)//TODO: pop the user to the homescreen
                    // Always register FCM token after successful registration regardless of notification preferences
                    // This ensures the token is always up-to-date on the server
                    registerForNotifications()
                } else {
                    Log.e("UserViewModel", "Registration failed for: ${request.email}")
                    _state.value = UiState.Error("Registration failed. Please try again.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Registration error: ${e.message}", e)
                when {
                    e.message?.contains("Unable to resolve host") == true -> {
                        _state.value =
                            UiState.Error("No internet connection. Please check your network and try again.")
                    }

                    e.message?.contains("timeout") == true -> {
                        _state.value =
                            UiState.Error("Connection timed out. Please check your network and try again.")
                    }

                    else -> {
                        _state.value = UiState.Error("Registration failed. Please try again later.")
                    }
                }
            }
        }
    }

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Attempting login for: ${request.email}")
                val success = repository.login(request)

                if (success) {
                    Log.d("UserViewModel", "Login successful for: ${request.email}")
                    loadUser()
                    // Register FCM token after successful login
                    registerForNotifications()
                } else {
                    Log.e("UserViewModel", "Login failed for: ${request.email}")
                    _state.value = UiState.Error("Login failed. Please check your credentials.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Login failed with exception", e)
                _state.value = UiState.Error("Login failed: ${e.message}")
            }
        }
    }

    /**
     * Login as a guest user with limited permissions
     */
    fun loginAsGuest() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Logging in as guest")

                // Create a guest user data object
                val guestUserData = UserData(
                    email = "guest_${System.currentTimeMillis()}@bluebridge.app",
                    firstName = "Guest",
                    lastName = "User",
                    username = "guest_user",
                    role = "guest",
                    userId = "guest-UID" ,
                    loginToken = "1234567890",
                    themePreference = 0

                )

                // Save the guest user locally
                repository.saveUserData(guestUserData)


                // Update state
                _state.value = UiState.Success(guestUserData)

                Log.d("UserViewModel", "Guest login successful")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Guest login error: ${e.message}", e)
                _state.value = UiState.Error("Guest login error: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d("UserViewModel", "Attempting to logout user")
                repository.logout()
                Log.d("UserViewModel", "Logout successful")
                _state.value = UiState.Error("Logged out")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Logout failed", e)
                _state.value = UiState.Error("Logout failed: ${e.message}")
            }
        }
    }

    /**
     * Deletes the user's account
     * @param email The email of the account to delete
     * @param password The password for verification
     */
    fun deleteAccount(email: String, password: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Attempting to delete account for: $email")

                // Get the user's token
                val token = repository.getLoginToken() ?: ""

                // Create delete request
                val deleteRequest = DeleteAccountRequest(
                    email = email,
                    password = password,
                    token = token
                )

                // Unregister from notifications if they were enabled
                if (_notificationsEnabled.value) {
                    unregisterFromNotifications()
                }

                // Call the repository to delete the account
                val success = repository.deleteAccount(deleteRequest)

                if (success) {
                    Log.d("UserViewModel", "Account deleted successfully for: $email")
                    // Clear local user data after successful deletion
                    repository.logout()
                    _state.value = UiState.Empty
                } else {
                    Log.e("UserViewModel", "Failed to delete account for: $email")
                    _state.value =
                        UiState.Error("Account deletion failed. Please check your credentials and try again.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Account deletion error: ${e.message}", e)
                _state.value = UiState.Error("Account deletion error: ${e.message}")
            }
        }
    }

    private fun updateProfile(userData: UserData) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                // First update on server
                val serverUpdateSuccess = repository.updateProfileOnServer(userData)
                if (serverUpdateSuccess) {
                    Log.d("UserViewModel", "Profile updated on server")
                } else {
                    Log.w(
                        "UserViewModel",
                        "Failed to update profile on server, saving locally only"
                    )
                }

                // Always save locally regardless of server result to avoid data loss
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

    /**
     * Enable or disable push notifications
     * @param enable Whether to enable push notifications
     */
    fun setNotificationsEnabled(enable: Boolean) {
        viewModelScope.launch {
            try {
                Log.d("UserViewModel", "Setting notifications enabled: $enable")

                repository.setNotificationsEnabled(enable)
                _notificationsEnabled.value = enable

                if (enable) {
                    // Register for notifications
                    registerForNotifications()
                } else {
                    // Unregister from notifications
                    unregisterFromNotifications()
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error setting notifications enabled: ${e.message}", e)
            }
        }
    }

    /**
     * Check if notifications are enabled
     */
    private fun checkNotificationsEnabled() {
        viewModelScope.launch {
            try {
                val enabled = repository.areNotificationsEnabled()
                _notificationsEnabled.value = enabled

                Log.d("UserViewModel", "Notifications enabled: $enabled")
            } catch (e: Exception) {
                Log.e(
                    "UserViewModel",
                    "Error checking if notifications are enabled: ${e.message}",
                    e
                )
            }
        }
    }

    /**
     * Register for push notifications with Firebase
     */
    private fun registerForNotifications() {
        viewModelScope.launch {
            try {
                val userData = repository.getUserData().first()
                // Skip for guest users
                if (userData?.role == "guest") {
                    Log.d("UserViewModel", "Skipping notification registration for guest user")
                    return@launch
                }

                // Get the current FCM token
                val token = getFirebaseToken()
                if (token.isNullOrEmpty()) {
                    Log.e("UserViewModel", "Failed to get Firebase token")
                    return@launch
                }

                Log.d("UserViewModel", "Got Firebase token: $token")

                // Save the token in preferences
                repository.saveNotificationToken(token)

                // Send the token to the server
                val email = repository.getUserEmail()
                val authToken = repository.getLoginToken()
                val userId = repository.getUserId()
                if (email != null && authToken != null) {
                    Log.d("UserViewModel", "Sending token to server for user $userId")
                    val success = repository.registerNotificationToken(userId, authToken, token)

                    if (success) {
                        Log.d("UserViewModel", "Successfully registered notification token")
                    } else {
                        Log.e("UserViewModel", "Failed to register notification token with server")
                    }
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error registering for notifications: ${e.message}", e)
            }
        }
    }

    /**
     * Unregister from push notifications
     */
    private fun unregisterFromNotifications() {
        viewModelScope.launch {
            try {
                val token = repository.getNotificationToken()
                if (token.isNullOrEmpty()) {
                    Log.e("UserViewModel", "No notification token to unregister")
                    return@launch
                }

                // Send unregister request to server
                val email = repository.getUserEmail()
                val authToken = repository.getLoginToken()

                if (email != null && authToken != null) {
                    Log.d("UserViewModel", "Unregistering token from server for $email")
                    val success = repository.unregisterNotificationToken(email, authToken, token)

                    if (success) {
                        Log.d("UserViewModel", "Successfully unregistered notification token")
                    } else {
                        Log.e(
                            "UserViewModel",
                            "Failed to unregister notification token with server"
                        )
                    }
                }

                // Clear the token from preferences
                repository.clearNotificationToken()
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error unregistering from notifications: ${e.message}", e)
            }
        }
    }

    /**
     * Get the Firebase Cloud Messaging token
     */
    private suspend fun getFirebaseToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error getting Firebase token: ${e.message}", e)
            null
        }
    }

    suspend fun updateWaterNeeds(waterNeeds: List<WaterNeed>) {
        try {
            val userData = repository.getUserData().first()
            // First update water needs on server if not in guest mode
            if (userData?.role != "guest") {
                val serverUpdateSuccess = repository.updateWaterNeedsOnServer(waterNeeds)
                if (serverUpdateSuccess) {
                    Log.d("UserViewModel", "Water needs updated on server")
                } else {
                    Log.w(
                        "UserViewModel",
                        "Failed to update water needs on server, saving locally only"
                    )
                }
            }

            // Get current user data
            val userDataFlow = repository.getUserData()
            val currentUserData = userDataFlow.first()

            // Update user data with new water needs
            val updatedUserData = currentUserData?.copy(waterNeeds = waterNeeds) ?: return

            // Save updated user data locally
            repository.saveUserData(updatedUserData)
            Log.d("UserViewModel", "Water needs updated locally")

        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating water needs", e)
        }
    }

    suspend fun getLoginToken(): String? {
        return repository.getLoginToken()
    }

}
