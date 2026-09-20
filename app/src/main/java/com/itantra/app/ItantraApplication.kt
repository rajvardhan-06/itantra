package com.itantra.app

import android.app.Application
import com.itantra.app.di.AppContainer

/**
 * Application class for iTantra.
 *
 * Serves as the root host for the dependency injection container [AppContainer].
 * Provides access to shared singletons for communication, repositories, and AI engines.
 */
class ItantraApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
