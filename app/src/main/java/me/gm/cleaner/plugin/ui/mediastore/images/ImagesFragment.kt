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

package me.gm.cleaner.plugin.ui.mediastore.images

import android.Manifest
import android.os.Build
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.dao.RootPreferences
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_DATE_TAKEN
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_PATH
import me.gm.cleaner.plugin.dao.RootPreferences.SORT_BY_SIZE
import me.gm.cleaner.plugin.databinding.MediaStoreFragmentBinding
import me.gm.cleaner.plugin.ktx.buildSpannableString
import me.gm.cleaner.plugin.ktx.fitsSystemWindowInsets
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreAdapter
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreFragment
import me.gm.cleaner.plugin.ui.mediastore.MediaStoreHeader
import me.gm.cleaner.plugin.ui.mediastore.imagepager.ImagePagerFragment
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyle

class ImagesFragment : MediaStoreFragment() {
    override val viewModel: ImagesViewModel by activityViewModels()
    override val requesterFragmentClass: Class<out MediaPermissionsRequesterFragment> =
        ImagesPermissionsRequesterFragment::class.java
    var lastPosition: Int = 0

    override fun onCreateAdapter(): MediaStoreAdapter = ImagesAdapter(this)

    override fun onBindView(
        binding: MediaStoreFragmentBinding,
        list: RecyclerView,
        adapter: MediaStoreAdapter
    ) {
        val layoutManager =
            ProgressionGridLayoutManager(
                requireContext(),
                RootPreferences.spanCountFlowable.value
            ).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                        if (adapter.currentList[position] is MediaStoreHeader) {
                            spanCount
                        } else {
                            1
                        }
                }
            }
        list.layoutManager = layoutManager
        val viewHelper = MediaStoreRecyclerViewHelper(list) { adapter.currentList }
        // Build FastScroller after SelectionTracker so that we can intercept SelectionTracker's OnItemTouchListener.
        val fastScroller = FastScrollerBuilder(list)
            .useMd2Style()
            .setPopupStyle(PopupStyle.MD3)
            .setViewHelper(viewHelper)
            .build()
        list.fitsSystemWindowInsets(fastScroller)
        list.addOnItemTouchListener(
            ScaleGestureListener(requireContext(), layoutManager, viewHelper)
        )

        prepareTransitions(list, adapter)
        setFragmentResultListener(ImagePagerFragment::class.java.name) { _, bundle ->
            lastPosition = bundle.getInt(ImagePagerFragment.KEY_POSITION)
            postponeEnterTransition()
            list.post {
                val lastUriPosition = adapter.getHolderPositionForUriPosition(lastPosition)
                if (lastUriPosition != null) {
                    scrollToPosition(list, lastUriPosition)
                } else {
                    startPostponedEnterTransition()
                }
            }
        }
    }

    /**
     * Prepares the shared element transition to the pager fragment, as well as the other transitions
     * that affect the flow.
     */
    private fun prepareTransitions(list: RecyclerView, adapter: MediaStoreAdapter) {
        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the ViewHolder for the clicked position.
                val lastUriPosition = adapter.getHolderPositionForUriPosition(lastPosition)
                    ?: return
                val selectedViewHolder = list.findViewHolderForAdapterPosition(lastUriPosition)
                    ?: return

                // Map the first shared element name to the child ImageView.
                val image = selectedViewHolder.itemView.findViewById<ImageView>(R.id.image)
                    ?: return
                sharedElements[names[0]] = image
            }
        })
    }

    /**
     * Scrolls the recycler view to show the last viewed item in the grid. This is important when
     * navigating back from the grid.
     */
    private fun scrollToPosition(list: RecyclerView, position: Int) {
        list.doOnPreDraw {
            val layoutManager = list.layoutManager as? LinearLayoutManager ?: return@doOnPreDraw
            val viewAtPosition = layoutManager.findViewByPosition(position)
            // Scroll to position if the view for the current position is null (not currently part of
            // layout manager children), or it's not completely visible.
            if (viewAtPosition == null ||
                layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)
            ) {
                val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                if (position >= lastPosition && lastPosition - layoutManager.findFirstCompletelyVisibleItemPosition() > 0) {
                    layoutManager.scrollToPosition(position)
                } else {
                    layoutManager.scrollToPositionWithOffset(position, list.paddingTop)
                }
            }
        }
    }

    class ImagesPermissionsRequesterFragment : MediaPermissionsRequesterFragment() {
        override val requiredPermissions: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (selectionTracker.hasSelection()) {
            return
        }
        inflater.inflate(R.menu.images_toolbar, menu)

        when (RootPreferences.sortMediaByFlowable.value) {
            SORT_BY_PATH ->
                menu.findItem(R.id.menu_sort_by_path).isChecked = true

            SORT_BY_DATE_TAKEN, SORT_BY_SIZE ->
                menu.findItem(R.id.menu_sort_by_date_taken).isChecked = true
        }
        arrayOf(menu.findItem(R.id.menu_header_sort)).forEach {
            it.title = requireContext().buildSpannableString(it.title!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_by_path -> {
                item.isChecked = true
                RootPreferences.sortMediaByFlowable.value = SORT_BY_PATH
            }

            R.id.menu_sort_by_date_taken -> {
                item.isChecked = true
                RootPreferences.sortMediaByFlowable.value = SORT_BY_DATE_TAKEN
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
