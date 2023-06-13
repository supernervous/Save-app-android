package net.opendasharchive.openarchive.util.extensions

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt

fun Drawable.scaled(factor: Double, context: Context): Drawable {
    if (factor == 1.0) return this

    val width = (intrinsicWidth * factor).roundToInt()
    val height = (intrinsicHeight * factor).roundToInt()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        LayerDrawable(arrayOf(this)).also {
            it.setLayerSize(0, width, height)
        }
    }
    else {
        BitmapDrawable(context.resources, toBitmap(width, height))
    }
}

fun Drawable.tint(color: Int): Drawable {
    colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

    return this
}