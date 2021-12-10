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

package me.gm.cleaner.plugin.module.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialContainerTransform
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.TemplatesFragmentBinding
import me.gm.cleaner.plugin.ktx.*
import me.gm.cleaner.plugin.module.ModuleFragment
import rikka.recyclerview.fixEdgeEffect
import kotlin.collections.set

class TemplatesFragment : ModuleFragment() {
    var enterRuleLabel: String? = null

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = TemplatesFragmentBinding.inflate(layoutInflater)

        val templatesAdapter = TemplatesAdapter(this)
        val adapters = ConcatAdapter(TemplatesHeaderAdapter(this), templatesAdapter)
        val list = binding.list
        list.adapter = adapters
        list.layoutManager = GridLayoutManager(requireContext(), 1)
        list.setHasFixedSize(true)
        list.fixEdgeEffect(false)
        list.overScrollIfContentScrollsPersistent()
        list.addLiftOnScrollListener { appBarLayout.isLifted = it }
        list.fitsSystemBottomInset()
        list.addItemDecoration(DividerDecoration(list).apply {
            setDivider(resources.getDrawable(R.drawable.list_divider_material, null))
            setAllowDividerAfterLastItem(false)
        })

        binderViewModel.remoteSpCacheLiveData.observe(viewLifecycleOwner) {
            templatesAdapter.submitList(binderViewModel.readTemplates().toList())
        }

        setFragmentResultListener(CreateTemplateFragment::class.java.simpleName) { _, bundle ->
            enterRuleLabel = bundle.getString(CreateTemplateFragment.KEY_LABEL)
            postponeEnterTransition()
        }
        prepareSharedElementTransition(list)
        return binding.root
    }

    private fun prepareSharedElementTransition(list: RecyclerView) {
        val key = getString(R.string.template_management_key) /* hardcoded */
        setFragmentResult(TemplatesFragment::class.java.simpleName, bundleOf(KEY to key))
        list.transitionName = key

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
        const val KEY = "me.gm.cleaner.plugin.key"
    }
}
