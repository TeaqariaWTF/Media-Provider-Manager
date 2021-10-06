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

package me.gm.cleaner.plugin.xposed.hooker

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import de.robv.android.xposed.XC_MethodHook
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.xposed.ManagerService

class QueryHooker(private val service: ManagerService) : XC_MethodHook(), MediaProviderHooker {
    @Throws(Throwable::class)
    override fun beforeHookedMethod(param: MethodHookParam) {
        val uri = param.args[0] as? Uri
        val projection = param.args[1] as? Array<String>
        val queryArgs = param.args[2] as? Bundle
        val signal = param.args[3] as? CancellationSignal
    }

    // for interaction
    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        if (param.callingPackage == BuildConfig.APPLICATION_ID) {
            val c = param.result as? Cursor ?: MatrixCursor(arrayOf("binder"))
            c.extras = c.extras.apply {
                putBinder("me.gm.cleaner.plugin.intent.extra.BINDER", service)
            }
            param.result = c
        }
    }
}