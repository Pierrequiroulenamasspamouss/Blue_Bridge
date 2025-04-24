package com.wellconnect.wellmonitoring.data

sealed class NearbyUsersState {
    data object Loading : NearbyUsersState()
    data class Success(val users: List<NearbyUser>) : NearbyUsersState()
    data class Error(val message: String) : NearbyUsersState()
    data object NoUsers : NearbyUsersState()
    data object LocationPermissionDenied : NearbyUsersState()
} 