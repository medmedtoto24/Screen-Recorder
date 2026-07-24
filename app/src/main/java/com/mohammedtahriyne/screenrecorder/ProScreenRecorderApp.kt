package com.mohammedtahriyne.screenrecorder

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

class ProScreenRecorderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = ConfigManager(this)

        AppCompatDelegate.setDefaultNightMode(config.getThemeModeValue())
        DynamicColors.applyToActivitiesIfAvailable(
            this,
            DynamicColorsOptions.Builder()
                .setPrecondition { _, _ ->
                    config.isDynamicColorsEnabled
                }
                .build()
        )
    }
}
