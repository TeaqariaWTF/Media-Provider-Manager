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

package me.gm.cleaner.plugin.module.appmanagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.AppFragmentBinding
import me.gm.cleaner.plugin.ktx.*
import me.gm.cleaner.plugin.module.ModuleFragment
import rikka.recyclerview.fixEdgeEffect

class AppFragment : ModuleFragment() {
    private val viewModel: AppViewModel by viewModels()
    private val args: AppFragmentArgs by navArgs()
    private val navController by lazy { findNavController() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = AppFragmentBinding.inflate(layoutInflater)

        val adapter = AppHeaderAdapter(this)
        val list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                supportActionBar?.title = if (recyclerView.isItemCompletelyInvisible(0)) {
                    navController.currentDestination?.label
                } else {
                    args.label
                }
            }
        })
        val paddingStart = list.paddingStart
        val paddingTop = list.paddingTop
        val paddingEnd = list.paddingEnd
        val paddingBottom = list.paddingBottom
        list.setOnApplyWindowInsetsListener { view, insets ->
            view.setPaddingRelative(
                paddingStart, paddingTop, paddingEnd, paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }

        prepareSharedElementTransition(list)
        return binding.root
    }

    private fun prepareSharedElementTransition(list: RecyclerView) {
        setFragmentResult(
            AppFragment::class.java.simpleName, bundleOf(KEY_PACKAGENAME to args.pi.packageName)
        )
        list.transitionName = args.pi.packageName

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host
            setAllContainerColors(requireContext().colorSurface)
            interpolator = FastOutSlowInInterpolator()
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = requireContext().mediumAnimTime
        }

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                if (names.isNotEmpty()) {
                    sharedElements[names[0]] = list
                }
            }
        })
    }

    companion object {
        const val KEY_PACKAGENAME = "me.gm.cleaner.plugin.key.packageName"
    }
}
