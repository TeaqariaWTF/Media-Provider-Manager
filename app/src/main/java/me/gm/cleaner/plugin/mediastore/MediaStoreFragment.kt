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

package me.gm.cleaner.plugin.mediastore

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.app.BaseFragment

abstract class MediaStoreFragment : BaseFragment() {
    override val requiredPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onRequestPermissions(permissions: Array<String>, savedInstanceState: Bundle?) {
        // TODO: show rationale
        Log.i(javaClass.simpleName, "useful overriding method")
        super.onRequestPermissions(permissions, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.mediastore_toolbar, menu)
    }
}
