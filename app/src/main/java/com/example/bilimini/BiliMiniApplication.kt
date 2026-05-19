package com.example.bilimini

import android.app.Application
import com.example.bilimini.data.api.BiliApiClient
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.session.SessionManager

class BiliMiniApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val sessionManager = SessionManager(application)
    val apiClient = BiliApiClient(sessionManager)
    val repository = BiliRepository(apiClient)
}
