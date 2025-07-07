package com.example.shutterup

import android.app.Application
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Mapbox 토큰 설정
        if (BuildConfig.MAPBOX_ACCESS_TOKEN.isNotEmpty()) {
            MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        }
    }
}