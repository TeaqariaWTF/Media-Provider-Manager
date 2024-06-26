/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import me.gm.cleaner.plugin.dao.RootPreferences
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        }
    }

    override fun onCreate() {
        super.onCreate()
        RootPreferences.init(createDeviceProtectedStorageContext())
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        DynamicColors.applyToActivitiesIfAvailable(this)
        reduceScaledMinimumScalingSpan()
    }

    @SuppressLint("BlockedPrivateApi")
    private fun reduceScaledMinimumScalingSpan() {
        // Reduce ScaledMinimumScalingSpan due to the following reasons:
        // 1) I think it significantly reduces the user experience.
        // 2) Many Google apps do not respect this value.
        // 3) The creator of pinch-to-zoom did not specify this value.
        val field = ViewConfiguration::class.java.getDeclaredField("mMinScalingSpan")
        field.isAccessible = true
        val viewConfiguration = ViewConfiguration.get(this)
        field.set(viewConfiguration, viewConfiguration.scaledTouchSlop * 2)
    }
}
