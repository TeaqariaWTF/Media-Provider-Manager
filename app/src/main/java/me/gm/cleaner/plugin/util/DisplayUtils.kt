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

package me.gm.cleaner.plugin.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.icu.text.ListFormatter
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.view.ContextMenu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatDrawableManager
import kotlin.math.roundToInt

fun Collection<*>.listFormat(delimiter: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ListFormatter.getInstance().format(this)
    } else {
        joinToString(delimiter)
    }

fun ContextMenu.setOnMenuItemClickListener(menuItemClickListener: (MenuItem) -> Boolean) {
    for (i in 0 until size()) {
        getItem(i).setOnMenuItemClickListener(menuItemClickListener)
    }
}

@SuppressLint("RestrictedApi")
fun Context.buildStyledTitle(text: CharSequence) = SpannableStringBuilder(text).apply {
    setSpan(
        TextAppearanceSpan(
            this@buildStyledTitle, androidx.appcompat.R.style.TextAppearance_AppCompat_Body2
        ),
        0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    setSpan(ForegroundColorSpan(colorPrimary), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
}

@SuppressLint("RestrictedApi")
fun Context.getBitmapFromVectorDrawable(@DrawableRes drawableId: Int): Bitmap {
    val drawable = AppCompatDrawableManager.get().getDrawable(this, drawableId)
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.setTint(colorPrimary)
    drawable.draw(canvas)
    return bitmap
}

fun Context.getDimenByAttr(attr: Int): Float {
    val a = obtainStyledAttributes(intArrayOf(attr))
    val dimen = a.getDimension(0, 0f)
    a.recycle()
    return dimen
}

@ColorInt
fun Context.getColorByAttr(attr: Int): Int {
    val a = obtainStyledAttributes(intArrayOf(attr))
    val color = a.getColorStateList(0)!!.defaultColor
    a.recycle()
    return color
}

fun Context.dipToPx(dipValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dipValue * scale + 0.5f).roundToInt()
}

fun Context.pxToDip(pxValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (pxValue / scale + 0.5f).roundToInt()
}
