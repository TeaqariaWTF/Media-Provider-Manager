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

package me.gm.cleaner.plugin.xposed

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.IBinder
import android.os.IInterface
import androidx.room.Room
import de.robv.android.xposed.XposedHelpers
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.IManagerService
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderDeleteRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderInsertRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderQueryRecord
import me.gm.cleaner.plugin.dao.mediaprovider.MediaProviderRecordDatabase
import me.gm.cleaner.plugin.model.ParceledListSlice
import me.gm.cleaner.plugin.module.settings.BinderSpImpl
import java.io.File

abstract class ManagerService : IManagerService.Stub() {
    lateinit var classLoader: ClassLoader
        protected set
    lateinit var resources: Resources
        protected set
    lateinit var context: Context
        private set
    lateinit var database: MediaProviderRecordDatabase
        private set
    val defaultSp by lazy { JsonFileSpImpl(File(context.filesDir, "default")) }

    protected fun onCreate(context: Context) {
        this.context = context
        database = Room.databaseBuilder(
            context.applicationContext, MediaProviderRecordDatabase::class.java,
            MEDIA_PROVIDER_USAGE_RECORD_DATABASE_NAME
        ).enableMultiInstanceInvalidation().build()
    }

    private val packageManagerService: IInterface by lazy {
        val binder = XposedHelpers.callStaticMethod(
            XposedHelpers.findClass("android.os.ServiceManager", classLoader),
            "getService", "package"
        ) as IBinder
        XposedHelpers.callStaticMethod(
            XposedHelpers.findClass(
                "android.content.pm.IPackageManager\$Stub", classLoader
            ), "asInterface", binder
        ) as IInterface
    }

    override fun getModuleVersion() = BuildConfig.VERSION_CODE

    override fun getInstalledPackages(userId: Int): ParceledListSlice<PackageInfo> {
        val parceledListSlice = XposedHelpers.callMethod(
            packageManagerService, "getInstalledPackages", PackageManager.GET_PERMISSIONS, userId
        )
        val list = XposedHelpers.callMethod(parceledListSlice, "getList") as List<PackageInfo>
        return ParceledListSlice(list)
    }

    override fun getPackageInfo(packageName: String, flags: Int, userId: Int) =
        XposedHelpers.callMethod(
            packageManagerService, "getPackageInfo", packageName, 0, userId
        ) as? PackageInfo

    override fun readSp(who: Int) = when (who) {
        BinderSpImpl.WHO -> defaultSp.read()
        else -> throw IllegalArgumentException()
    }

    override fun writeSp(who: Int, what: String) {
        when (who) {
            BinderSpImpl.WHO -> defaultSp.write(what)
        }
    }

    override fun clearAllTables() {
        database.clearAllTables()
    }

    override fun packageUsageTimes(table: String, packageNames: List<String>) = when (table) {
        MediaProviderQueryRecord::class.simpleName ->
            database.MediaProviderQueryRecordDao().packageUsageTimes(*packageNames.toTypedArray())
        MediaProviderInsertRecord::class.simpleName ->
            database.MediaProviderInsertRecordDao().packageUsageTimes(*packageNames.toTypedArray())
        MediaProviderDeleteRecord::class.simpleName ->
            database.MediaProviderDeleteRecordDao().packageUsageTimes(*packageNames.toTypedArray())
        else -> throw IllegalArgumentException()
    }

    companion object {
        const val MEDIA_PROVIDER_USAGE_RECORD_DATABASE_NAME = "media_provider.db"
    }
}
