package com.example.myecomapp

import android.app.Application
import com.example.myecomapp.utils.Constants.CONFIG_CLOUDARY
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.cloudinary.android.MediaManager.init(this, CONFIG_CLOUDARY)
    }
}