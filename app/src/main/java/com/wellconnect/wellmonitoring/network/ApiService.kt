@GET("users/nearby")
suspend fun getNearbyUsers(): ApiResponse<List<NearbyUser>> 