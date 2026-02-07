package com.bluebridgeapp.bluebridge.data

import android.content.Context
import com.bluebridgeapp.bluebridge.data.local.UserPreferences
import com.bluebridgeapp.bluebridge.data.local.WellPreferences
import com.bluebridgeapp.bluebridge.data.repository.*
import com.bluebridgeapp.bluebridge.network.RetrofitBuilder

object RepositoryProvider {
    private var initialized = false
    lateinit var userRepository: UserRepositoryImpl
    lateinit var chatRepository: ChatRepositoryImpl

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        val api = RetrofitBuilder.getServerApi(appContext)
        val userPrefs = UserPreferences(appContext)
        userRepository = UserRepositoryImpl(api, userPrefs)
        chatRepository = ChatRepositoryImpl(api, userRepository, appContext)
        initialized = true
    }
}