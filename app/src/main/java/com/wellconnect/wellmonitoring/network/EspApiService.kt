package com.wellconnect.wellmonitoring.network

import com.wellconnect.wellmonitoring.data.WellData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val username: String
)

@Serializable
data class RegisterResponse(
    val status: String,
    val timestamp: String,
    val message: String
)

@Serializable
data class LoginResponse(
    val status: String,
    val timestamp: String,
    val message: String,
    @SerialName("data")
    val userData: UserResponseData? = null
)

@Serializable
data class UserResponseData(
    val user: UserData
)

@Serializable
data class UserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val lastLogin: String? = null
)

interface EspApiService {
    @GET("/data")
    suspend fun getWellData(): WellData

    @POST("/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>
}
