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

package me.gm.cleaner.plugin.settings

import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.gm.cleaner.plugin.BinderReceiver
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import me.gm.cleaner.plugin.util.PreferencesPackageInfo.Companion.copy
import java.util.concurrent.atomic.AtomicInteger

class AppListLoader(private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default) {
    suspend fun load(pm: PackageManager, l: ProgressListener?): List<PreferencesPackageInfo> {
        return withContext(defaultDispatcher) {
            val installedPackages = BinderReceiver.installedPackages.filter {
                it.applicationInfo.enabled
            }
            val size = installedPackages.size
            val count = AtomicInteger(0)
            installedPackages.map {
                ensureActive()
                l?.onProgress(100 * count.incrementAndGet() / size)
                PreferencesPackageInfo.newInstance(it, pm)
            }
        }
    }

    suspend fun update(old: List<PreferencesPackageInfo>): List<PreferencesPackageInfo> {
        return withContext(defaultDispatcher) {
            mutableListOf<PreferencesPackageInfo>().apply {
                old.forEach {
                    add(it.copy())
                }
            }
        }
    }

    interface ProgressListener {
        fun onProgress(progress: Int)
    }
}