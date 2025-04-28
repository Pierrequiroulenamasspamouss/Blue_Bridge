package com.wellconnect.wellmonitoring.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.wellconnect.wellmonitoring.data.LoginResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Singleton object to create and provide a Retrofit instance
 */
object RetrofitBuilder {
    private var apiService: EspApiService? = null
    
    @OptIn(ExperimentalSerializationApi::class)
    fun create(baseUrl: String): EspApiService {
        if (apiService == null) {
            val contentType = "application/json".toMediaType()
            
            val json = Json {
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
            
            apiService = retrofit.create(EspApiService::class.java)
        }
        return apiService!!
    }

    /**
     * Creates a fresh API service instance with a new client for each call.
     * This helps avoid issues with stale connections.
     */
    fun createFresh(baseUrl: String): EspApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)  // Increased timeout values
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(EspApiService::class.java)
    }
}




