package com.wellconnect.wellmonitoring.viewmodels

import UserData
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.wellconnect.wellmonitoring.data.UserEvent
import com.wellconnect.wellmonitoring.data.`interface`.UserRepository
import com.wellconnect.wellmonitoring.data.model.DeleteAccountRequest
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.WaterNeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel(
    val repository: UserRepository
) : ViewModel() {
    companion object;

    // State management
    private val _state = mutableStateOf<UiState<UserData>>(UiState.Empty)
    val state = _state
    
    // Push notification state
    private val _notificationsEnabled = mutableStateOf(false)
    val notificationsEnabled = _notificationsEnabled

    fun handleEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Register -> register(event.registerRequest)
            is UserEvent.Login -> login(event.loginRequest)
            is UserEvent.UpdateTheme -> updateTheme(event.theme)
            is UserEvent.UpdateProfile -> updateProfile(event.userData)
            is UserEvent.Logout -> logout()
            is UserEvent.LoadUser -> loadUser()
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Attempting to load user data from preferences")
                repository.getUserData().collect { userData ->
                    userData?.let {
                        Log.d("UserViewModel", "Successfully loaded user data: ${it.email}, is logged in: ${repository.isLoggedIn()}")
                        _state.value = UiState.Success(it)
                        
                        // Check if notifications are enabled
                        checkNotificationsEnabled()
                    } ?: run {
                        Log.e("UserViewModel", "User data is null from preferences")
                        _state.value = UiState.Error("No user data available")
                    }
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to load user: ${e.message}", e)
                _state.value = UiState.Error("Failed to load user: ${e.message}")
            }
        }
    }

    private fun updateTheme(theme: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                // Save theme preference for both guest and regular users
                repository.updateThemePreference(theme)
                
                // Determine if we're in guest mode
                val isGuest = repository.isGuestMode()
                
                // For guest users, just update the current user data with the new theme
                if (isGuest) {
                    // Get current user data or create a new guest user data object
                    val currentData = (_state.value as? UiState.Success<UserData>)?.data ?: UserData(
                        email = "guest@wellconnect.app",
                        firstName = "Guest",
                        lastName = "User",
                        username = "guest_user",
                        role = "guest",
                        isWellOwner = false,
                        loginToken = null,
                        themePreference = theme  // Set the theme preference
                    )
                    
                    // Create updated user data with new theme
                    val updatedData = currentData.copy(themePreference = theme)
                    
                    // Save updated data and update state
                    repository.saveUserData(updatedData)
                    _state.value = UiState.Success(updatedData)
                    Log.d("UserViewModel", "Updated theme for guest user to: $theme")
                } else {
                    // For regular users, just reload user data
                    loadUser() 
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Failed to update theme: ${e.message}", e)
                _state.value = UiState.Error("Failed to update theme: ${e.message}")
            }
        }
    }

    private fun register(registerRequest: RegisterRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val success = repository.register(registerRequest)
                if (success) {
                    loadUser()

                    // Register for notifications if user allows
                    if (_notificationsEnabled.value) {
                        registerForNotifications()
                    }
                } else {
                    _state.value = UiState.Error("Registration failed")
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun login(request: LoginRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                Log.d("UserViewModel", "Attempting login for: ${request.email}")
                val success = repository.login(request)

                if (success) {
                    Log.d("UserViewModel", "Login successful for: ${request.email}")
                    loadUser()

                    // Always register FCM token after successful login regardless of notification preferences
                    // This ensures the token is always up-to-date on the server
                    registerForNotifications() 
                } else {
                    Log.e("UserViewModel", "Login failed for: ${request.email}")
                    _state.value = UiState.Error("Login failed. Please check your credentials and try again.")
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Login error: ${e.message}", e)
                _state.value = UiState.Error("Login error: ${e.message}")
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
                    email = "guest_${System.currentTimeMillis()}@wellconnect.app",
                    firstName = "Guest",
                    lastName = "User",
                    username = "guest_user",
                    role = "guest",
                    isWellOwner = false,
                    loginToken = null
                )

                // Save the guest user locally
                repository.saveUserData(guestUserData)

                // Set the guest flag in preferences
                repository.setGuestMode(true)

                // Update state
                _state.value = UiState.Success(guestUserData)

                Log.d("UserViewModel", "Guest login successful")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Guest login error: ${e.message}", e)
                _state.value = UiState.Error("Guest login error: ${e.message}")
            }
        }
    }

    /**
     * Check if the current user is in guest mode
     */
    suspend fun isGuestMode(): Boolean {
        return repository.isGuestMode()
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                // Unregister from notifications if they were enabled
                if (_notificationsEnabled.value) {
                    unregisterFromNotifications()
                }

                repository.logout()
                _state.value = UiState.Empty
            } catch (e: Exception) {
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
                val token = repository.getAuthToken() ?: ""

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
                    _state.value = UiState.Error("Account deletion failed. Please check your credentials and try again.")
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
                    Log.w("UserViewModel", "Failed to update profile on server, saving locally only")
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

    suspend fun getUserWaterNeeds(): String? {
        return repository.getUserWaterNeeds()
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
                Log.e("UserViewModel", "Error checking if notifications are enabled: ${e.message}", e)
            }
        }
    }

    /**
     * Register for push notifications with Firebase
     */
    private fun registerForNotifications() {
        viewModelScope.launch {
            try {
                // Skip for guest users
                if (repository.isGuestMode()) {
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
                val authToken = repository.getAuthToken()

                if (email != null && authToken != null) {
                    Log.d("UserViewModel", "Sending token to server for $email")
                    val success = repository.registerNotificationToken(email, authToken, token)

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
                val authToken = repository.getAuthToken()

                if (email != null && authToken != null) {
                    Log.d("UserViewModel", "Unregistering token from server for $email")
                    val success = repository.unregisterNotificationToken(email, authToken, token)

                    if (success) {
                        Log.d("UserViewModel", "Successfully unregistered notification token")
                    } else {
                        Log.e("UserViewModel", "Failed to unregister notification token with server")
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
            // First update water needs on server if not in guest mode
            if (!repository.isGuestMode()) {
                val serverUpdateSuccess = repository.updateWaterNeedsOnServer(waterNeeds)
                if (serverUpdateSuccess) {
                    Log.d("UserViewModel", "Water needs updated on server")
                } else {
                    Log.w("UserViewModel", "Failed to update water needs on server, saving locally only")
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
    suspend fun setUserWaterNeeds(needs: String) {
        try {
            Log.d("UserViewModel", "Setting water needs: $needs")

            // First get current userData
            val userData = repository.getUserData().first()
            if (userData == null) {
                Log.e("UserViewModel", "Cannot update water needs: User data is null")
                return
            }

            // Create WaterNeed list
            val waterNeeds = if (needs.isNotBlank()) {
                val amount = needs.toIntOrNull() ?: 0
                val description = "General water need"
                listOf(
                    WaterNeed(
                        amount = amount,
                        usageType = "General",
                        priority = 3,
                        description = description
                    )
                )
            } else {
                emptyList()
            }

            Log.d("UserViewModel", "Created water needs: $waterNeeds")

            // Update local preferences string value
            repository.setUserWaterNeeds(needs)

            // Try to update on server
            viewModelScope.launch {
                try {
                    val serverUpdateSuccess = repository.updateWaterNeedsOnServer(waterNeeds)
                    if (serverUpdateSuccess) {
                        Log.d("UserViewModel", "Water needs updated on server")
                    } else {
                        Log.w(
                            "UserViewModel",
                            "Failed to update water needs on server, saving locally only"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Error updating water needs on server", e)
                }
            }
        }
        catch (e: Exception) {
            Log.e("UserViewModel", "Error setting water needs", e)
        }
    }

    suspend fun getLoginToken(): String? {
        return repository.getAuthToken()
    }

}

