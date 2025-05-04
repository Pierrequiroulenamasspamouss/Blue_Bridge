package com.wellconnect.wellmonitoring.network

import ShortenedWellData
import WellData
import android.content.Context
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.model.BasicResponse
import com.wellconnect.wellmonitoring.data.model.DeleteAccountRequest
import com.wellconnect.wellmonitoring.data.model.DeleteAccountResponse
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.LoginResponse
import com.wellconnect.wellmonitoring.data.model.NearbyUsersResponse
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.RegisterResponse
import com.wellconnect.wellmonitoring.data.model.UpdateLocationRequest
import com.wellconnect.wellmonitoring.data.model.UpdateProfileRequest
import com.wellconnect.wellmonitoring.data.model.UpdateWaterNeedsRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Singleton object to create and provide a Retrofit instance
 */
object RetrofitBuilder {
    private var espApiService: ServerApi? = null
    private var serverApiService: ServerApi? = null
    private var devServerApiService: ServerApi? = null

    @OptIn(ExperimentalSerializationApi::class)
    fun create(baseUrl: String): ServerApi {
        if (espApiService == null) {
            val contentType = "application/json".toMediaType()
            
            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            }
            
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
            
            espApiService = retrofit.create(ServerApi::class.java)
        }
        return espApiService!!
    }

    /**
     * Creates a fresh API service instance with a new client for each call.
     * This helps avoid issues with stale connections.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun createFresh(baseUrl: String): ServerApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(ServerApi::class.java)
    }

    private fun getDevSSLConfiguration(context: Context): Pair<SSLContext, X509TrustManager> {
        // Load the development certificate from raw resources
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificateInputStream = context.resources.openRawResource(context.resources.getIdentifier("dev_cert", "raw", context.packageName))

        val certificate = certificateFactory.generateCertificate(certificateInputStream) as X509Certificate
        certificateInputStream.close()
        Log.d("SSL", "Loaded development certificate: ${certificate.subjectDN}")

        // Create a KeyStore containing our development certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("dev_cert", certificate)

        // Create a TrustManager that trusts both our dev cert and the system certs
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        val devTrustManager = trustManagerFactory.trustManagers[0] as X509TrustManager

        // Create an SSLContext that uses our TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(devTrustManager), SecureRandom())

        return Pair(sslContext, devTrustManager)
    }

    private fun createProductionClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .build()
    }

    private fun createDevelopmentClient(context: Context, loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val (sslContext, trustManager) = getDevSSLConfiguration(context)
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true } // Only for development
            .build()
    }

    /**
     * Get an instance of the server_crt API service with production/development fallback
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun getServerApi(context: Context): ServerApi {
        if (serverApiService == null || devServerApiService == null) {
            val contentType = "application/json".toMediaType()
            val prodServerUrl = context.getString(R.string.ProductionServerUrl)
            val devServerUrl = context.getString(R.string.DevelopmentServerUrl)
            
            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            }
            
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Create production service
            val prodClient = createProductionClient(loggingInterceptor)
            val prodRetrofit = Retrofit.Builder()
                .baseUrl(prodServerUrl)
                .client(prodClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
            serverApiService = prodRetrofit.create(ServerApi::class.java)

            // Create development service
            val devClient = createDevelopmentClient(context, loggingInterceptor)
            val devRetrofit = Retrofit.Builder()
                .baseUrl(devServerUrl)
                .client(devClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
            devServerApiService = devRetrofit.create(ServerApi::class.java)
        }

        return FallbackEspApiService(serverApiService!!, devServerApiService!!)
    }
}

/**
 * Wrapper service that implements automatic fallback from production to development
 */
class FallbackEspApiService(
    private val prodService: ServerApi,
    private val devService: ServerApi
) : ServerApi {


    override suspend fun updateProfile(request: UpdateProfileRequest): Response<BasicResponse> =
        try {
            prodService.updateProfile(request)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.updateProfile(request)
                }
                else -> throw e
            }
        }
    override suspend fun getWellDataById(espId: String): WellData =
        try {
            prodService.getWellDataById(espId)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.getWellDataById(espId)
                }
                else -> throw e
            }
        }

    override suspend fun login(request: LoginRequest): Response<LoginResponse> =
        try {
            prodService.login(request)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.login(request)
                }
                else -> throw e
            }
        }

    override suspend fun register(request: RegisterRequest): Response<RegisterResponse> =
        try {
            prodService.register(request)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.register(request)
                }
                else -> throw e
            }
        }

    override suspend fun getAllWells(): List<ShortenedWellData> =
        try {
            prodService.getAllWells()
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.getAllWells()
                }
                else -> throw e
            }
        }

    override suspend fun getNearbyUsers(latitude: Double, longitude: Double, radius: Double, email: String): Response<NearbyUsersResponse> =
        try {
            prodService.getNearbyUsers(latitude, longitude, radius, email)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.getNearbyUsers(latitude, longitude, radius, email)
                }
                else -> throw e
            }
        }

    override suspend fun updateLocation(request: UpdateLocationRequest): Response<BasicResponse> =
        try {
            prodService.updateLocation(request)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.updateLocation(request)
                }
                else -> throw e
            }
        }

    override suspend fun updateWaterNeeds(request: UpdateWaterNeedsRequest): Response<BasicResponse> =
        try {
            prodService.updateWaterNeeds(request)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.updateWaterNeeds(request)
                }
                else -> throw e
            }
        }

    override suspend fun getWellStats() =
        try {
            prodService.getWellStats()
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.getWellStats()
                }
                else -> throw e
            }
        }
    override suspend fun deleteAccount(request: DeleteAccountRequest): Response<DeleteAccountResponse> =
        try {
            prodService.deleteAccount(request)
        } catch (e: Exception) {
            when (e) {
                is IOException, is HttpException -> {
                    Log.w("FallbackEspApiService", "Production API failed, falling back to development", e)
                    devService.deleteAccount(request)
                }
                else -> throw e
            }
        }
}
