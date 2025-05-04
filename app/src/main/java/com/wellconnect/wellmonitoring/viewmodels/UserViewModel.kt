package com.wellconnect.wellmonitoring.viewmodels

import UserData
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.UserEvent
import com.wellconnect.wellmonitoring.data.`interface`.UserRepository
import com.wellconnect.wellmonitoring.data.model.DeleteAccountRequest
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.WaterNeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Top-level DataStore declaration
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object PreferencesKeys {
    val EMAIL = stringPreferencesKey("email")
    val FIRST_NAME = stringPreferencesKey("first_name")
    val LAST_NAME = stringPreferencesKey("last_name")
    val USERNAME = stringPreferencesKey("username")
    val ROLE = stringPreferencesKey("role")
    val THEME = intPreferencesKey("theme")
    val LOCATION = stringPreferencesKey("location")
    val WATER_NEEDS = stringPreferencesKey("water_needs")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val LOGIN_TOKEN = stringPreferencesKey("login_token")
    val IS_WELL_OWNER = booleanPreferencesKey("is_well_owner")
}

class UserViewModel(
    val repository: UserRepository
) : ViewModel() {
    companion object;

    // State management
    private val _state = mutableStateOf<UiState<UserData>>(UiState.Empty)
    val state = _state

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
                repository.updateThemePreference(theme)
                loadUser() // Refresh user data to get updated theme
            } catch (e: Exception) {
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

    fun logout() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
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
                listOf(WaterNeed(
                    amount = amount, 
                    usageType = "General", 
                    priority = 3,
                    description = description
                ))
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
                        Log.w("UserViewModel", "Failed to update water needs on server, saving locally only")
                    }
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Error updating water needs on server", e)
                }
            }
            
            // Always update locally regardless of server result
            val updatedUserData = userData.copy(waterNeeds = waterNeeds)
            repository.saveUserData(updatedUserData)
            Log.d("UserViewModel", "Water needs updated locally")
            
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating water needs", e)
        }
    }
}

