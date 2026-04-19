package com.example.myecomapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = mapOf(
            "cloud_name" to "dknt0thb6",
            "api_key" to "569385867159738",
            "api_secret" to "Gos2IQrBQOhNbUr100AhBjhgsBI"
        )

        com.cloudinary.android.MediaManager.init(this, config)
    }
}