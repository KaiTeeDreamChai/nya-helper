package com.nya.helper

import android.app.Application
import com.google.android.material.color.DynamicColors

class NyaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 开启 Android 12+ 官方 Monet (Material You / Dynamic Colors) 动态壁纸取色引擎
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
