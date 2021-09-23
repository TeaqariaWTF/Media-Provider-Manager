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

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.dao.ModulePreferences
import me.gm.cleaner.plugin.util.PreferencesPackageInfo
import java.text.Collator

class AppListViewModel : ViewModel() {
    private val _apps = MutableStateFlow<SourceState>(SourceState.Load(0))
    val apps: StateFlow<SourceState> = _apps
    fun loadApps(pm: PackageManager) {
        viewModelScope.launch {
            val list = AppListLoader().load(pm, object : AppListLoader.ProgressListener {
                override fun onProgress(progress: Int) {
                    _apps.value = SourceState.Load(progress)
                }
            })
            /* @VisibleForTesting */ delay(1000)
            _apps.emit(SourceState.Ready(list))
        }
    }

    fun updateApps() {
        viewModelScope.launch {
            if (_apps.value is SourceState.Ready) {
                _apps.emit(SourceState.Ready(AppListLoader().update((_apps.value as SourceState.Ready).source)))
            }
        }
    }

    private val _isSearching = MutableStateFlow(false)
    var isSearching: Boolean
        get() = _isSearching.value
        set(value) {
            if (isSearching == value) {
                return
            }
            _isSearching.value = value
        }
    private val _queryText = MutableStateFlow("")
    var queryText: String
        get() = _queryText.value
        set(value) {
            if (queryText == value) {
                return
            }
            _queryText.value = value
        }
    val showingList = combine(apps, _isSearching, _queryText) { apps, isSearching, queryText ->
        if (apps is SourceState.Load) {
            return@combine emptyList<PreferencesPackageInfo>()
        }
        (apps as SourceState.Ready).source.toMutableList().apply {
            if (ModulePreferences.isHideSystemApp) {
                removeIf {
                    it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                }
            }
            if (ModulePreferences.isHideNoStoragePermissionApp) {
                removeIf {
                    val requestedPermissions = it.requestedPermissions
                    requestedPermissions == null || !requestedPermissions.run {
                        contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                                || contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                && contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    }
                }
            }
            when (ModulePreferences.sortBy) {
                ModulePreferences.SORT_BY_NAME ->
                    sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
                        Collator.getInstance().compare(o1?.label, o2?.label)
                    }
                ModulePreferences.SORT_BY_UPDATE_TIME ->
                    sortWith(Comparator.comparingLong {
                        -it.lastUpdateTime
                    })
            }
            if (ModulePreferences.ruleCount) {
//                    sortWith { o1: PreferencesPackageInfo?, o2: PreferencesPackageInfo? ->
//                        when (mTitle) {
//                            R.string.storage_redirect_title -> return@sortWith o2!!.srCount - o1!!.srCount
//                            R.string.foreground_activity_observer_title -> return@sortWith o2!!.faInfo.size - o1!!.faInfo.size
//                            else -> return@sortWith 0
//                        }
//                    }
            }
            if (isSearching) {
                val lowerQuery = queryText.lowercase()
                removeIf {
                    !it.label.lowercase().contains(lowerQuery) &&
                            !it.applicationInfo.packageName.lowercase().contains(lowerQuery)
                }
            }
        }
    }
}

sealed class SourceState {
    data class Load(var progress: Int) : SourceState()
    data class Ready(val source: List<PreferencesPackageInfo>) : SourceState()
}
