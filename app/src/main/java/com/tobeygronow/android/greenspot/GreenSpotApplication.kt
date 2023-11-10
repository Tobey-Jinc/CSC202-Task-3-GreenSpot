package com.tobeygronow.android.greenspot

import android.app.Application

/**
 * Initializes the PlantRepository
 */
class GreenSpotApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlantRepository.initialize(this)
    }
}