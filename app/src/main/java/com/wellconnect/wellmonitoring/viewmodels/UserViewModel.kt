suspend fun getNearbyUsers(): List<NearbyUser> {
    return try {
        val response = apiService.getNearbyUsers()
        if (response.status == "success") {
            response.users ?: emptyList()
        } else {
            throw Exception(response.message ?: "Failed to fetch nearby users")
        }
    } catch (e: Exception) {
        throw Exception("Error fetching nearby users: ${e.message}")
    }
} 