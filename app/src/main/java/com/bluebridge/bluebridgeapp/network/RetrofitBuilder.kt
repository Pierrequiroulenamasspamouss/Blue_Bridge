package com.bluebridge.bluebridgeapp.network

import android.content.Context
import android.util.Log
import com.bluebridge.bluebridgeapp.R
import com.bluebridge.bluebridgeapp.data.model.DeleteAccountRequest
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersRequest
import com.bluebridge.bluebridgeapp.data.model.NotificationTokenRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateLocationRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateProfileRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateWaterNeedsRequest
import com.bluebridge.bluebridgeapp.data.model.WeatherRequest
import com.bluebridge.bluebridgeapp.data.model.WeatherResponse
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.model.WellStatsResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Singleton object to create and provide a Retrofit instance
 */
object RetrofitBuilder {
    private var serverApi: ServerApi? = null
    private var devServerApi: ServerApi? = null

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun createOkHttpClient(isDev: Boolean = false): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply {
                if (isDev) {
                    val trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                    val sslContext = SSLContext.getInstance("TLS").apply {
                        init(null, arrayOf<TrustManager>(trustManager), null)
                    }
                    sslSocketFactory(sslContext.socketFactory, trustManager)
                    hostnameVerifier { _, _ -> true }
                }
            }
            .build()
    }

    private fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    fun getServerApi(context: Context): ServerApi {
        if (serverApi == null || devServerApi == null) {
            val prodUrl = context.getString(R.string.ProductionServerUrl)
            val devUrl = context.getString(R.string.DevelopmentServerUrl)

            serverApi = createRetrofit(prodUrl, createOkHttpClient()).create(ServerApi::class.java)
            devServerApi = createRetrofit(devUrl, createOkHttpClient(true)).create(ServerApi::class.java)
        }

        return FallbackServerApi(serverApi!!, devServerApi!!)
    }

    fun getSmsApi(context: Context): SmsApi {
        return SmsApi(context)
    }
}

/**
 * Wrapper service that implements automatic fallback from production to development
 */
class FallbackServerApi(
    private val prodApi: ServerApi,
    private val devApi: ServerApi
) : ServerApi {
    private suspend fun <T> withFallback(block: suspend (ServerApi) -> T): T {
        return try {
            block(prodApi)
        } catch (e: Exception) {
            Log.w("FallbackServerApi", "Production API failed, falling back to development", e)
            block(devApi)
        }
    }
    override suspend fun editWell(wellData: WellData, email: String, token: String) = withFallback { it.editWell(wellData, email, token) }
    override suspend fun getServerStatus() = withFallback { it.getServerStatus() }
    override suspend fun getServerCertificate() = withFallback { it.getServerCertificate() }
    override suspend fun getWellDataById(espId: String) = withFallback { it.getWellDataById(espId) }
    override suspend fun createWell(wellData: WellData, email: String, token: String) = withFallback { it.editWell(wellData, email, token) }
    override suspend fun deleteWell(espId: String, email: String, token: String) = withFallback { it.deleteWell(espId, email, token) }
    override suspend fun login(request: LoginRequest) = withFallback { it.login(request) }
    override suspend fun register(request: RegisterRequest) = withFallback { it.register(request) }
    override suspend fun getWellsWithFilters(page: Int, limit: Int, wellName: String?, wellStatus: String?, wellWaterType: String?, wellOwner: String?, espId: String?, minWaterLevel: Int?, maxWaterLevel: Int?) = 
        withFallback { it.getWellsWithFilters(page, limit, wellName, wellStatus, wellWaterType, wellOwner, espId, minWaterLevel, maxWaterLevel) }
    override suspend fun getWellStats(espId: String): Response<WellStatsResponse> = withFallback { it.getWellStats(espId) }
    override suspend fun getNearbyUsers(request: NearbyUsersRequest) = withFallback { it.getNearbyUsers(request) }
    override suspend fun updateLocation(request: UpdateLocationRequest) = withFallback { it.updateLocation(request) }
    override suspend fun updateWaterNeeds(request: UpdateWaterNeedsRequest) = withFallback { it.updateWaterNeeds(request) }
    override suspend fun updateProfile(request: UpdateProfileRequest) = withFallback { it.updateProfile(request) }
    override suspend fun deleteAccount(request: DeleteAccountRequest) = withFallback { it.deleteAccount(request) }
    override suspend fun registerNotificationToken(request: NotificationTokenRequest) = withFallback { it.registerNotificationToken(request) }
    override suspend fun unregisterNotificationToken(request: NotificationTokenRequest) = withFallback { it.unregisterNotificationToken(request) }
    override suspend fun getWeather(request: WeatherRequest): Response<WeatherResponse> = withFallback { it.getWeather(request) }
}
