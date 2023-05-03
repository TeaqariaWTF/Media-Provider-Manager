/*
 * Copyright 2022 Green Mushroom
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

package me.zhanghai.android.fastscroll

import android.graphics.Rect
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal class PreciseRecyclerViewHelper(
    private val list: RecyclerView, popupTextProvider: PopupTextProvider? = null,
    measureAllItemsOnStart: Boolean = true
) : NoInterceptionRecyclerViewHelper(list, popupTextProvider) {
    private val observer: ItemHeightsObserver = ItemHeightsObserver(list, measureAllItemsOnStart)
    private val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder> = list.adapter!!
    private val layoutManager: LinearLayoutManager = list.layoutManager as LinearLayoutManager
    private val mTempRect: Rect = Rect()

    init {
        adapter.registerAdapterDataObserver(observer)
    }

    override fun getScrollRange(): Int =
        list.paddingTop + observer.itemHeights.sum() + list.paddingBottom

    override fun getScrollOffset(): Int {
        val firstItemPosition = layoutManager.getPosition(list.getChildAt(0))
        if (firstItemPosition == RecyclerView.NO_POSITION ||
            firstItemPosition >= observer.itemHeights.size
        ) {
            return 0
        }
        val itemHeightsSum = observer.itemHeights.query(0, firstItemPosition)
        val firstItemTop = getFirstItemOffset()
        return list.paddingTop + itemHeightsSum - firstItemTop
    }

    private fun getFirstItemOffset(): Int {
        if (list.childCount == 0) {
            return 0
        }
        val itemView = list.getChildAt(0)
        list.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.top
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        list.stopScroll()
        val offset = offset - list.paddingTop
        var sum = 0
        var firstItemPosition = 0
        for (i in 0 until adapter.itemCount) {
            val next = sum + observer.itemHeights[i]
            if (next > offset) break
            sum = next
            firstItemPosition++
        }
        val firstItemTop = sum - offset
        scrollToPositionWithOffset(firstItemPosition, firstItemTop)
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int) {
        if (layoutManager is GridLayoutManager && layoutManager.spanCount > 1) {
            throw UnsupportedOperationException("GridLayoutManager is not supported yet.")
        }
        // LinearLayoutManager actually takes offset from paddingTop instead of top of RecyclerView.
        val offset = offset - list.paddingTop
        layoutManager.scrollToPositionWithOffset(position, offset)
    }
}
