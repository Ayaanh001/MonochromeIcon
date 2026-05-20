// ImageUtils.kt
package com.hussain.monochromeiconmaker

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import java.io.OutputStream

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        src.copy(Bitmap.Config.ARGB_8888, true)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Bitmap.toSmartMonochromeBlack(): Bitmap {
    val w = width
    val h = height
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, 0, 0, w, h)

    val cornerColors = listOf(
        pixels[0],
        pixels[w - 1],
        pixels[(h - 1) * w],
        pixels[w * h - 1]
    )
    val bgColor = averageColor(cornerColors)

    val threshold = 40.0
    for (i in pixels.indices) {
        val c = pixels[i]
        val distance = colorDistance(c, bgColor)
        if (distance < threshold) {
            pixels[i] = Color.TRANSPARENT
        } else {
            val alpha = Color.alpha(c)
            pixels[i] = Color.argb(alpha, 0, 0, 0)
        }
    }

    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return out
}

fun averageColor(colors: List<Int>): Int {
    var rSum = 0
    var gSum = 0
    var bSum = 0
    for (color in colors) {
        rSum += Color.red(color)
        gSum += Color.green(color)
        bSum += Color.blue(color)
    }
    val n = colors.size
    return Color.rgb(rSum / n, gSum / n, bSum / n)
}

fun colorDistance(c1: Int, c2: Int): Double {
    val rDiff = Color.red(c1) - Color.red(c2)
    val gDiff = Color.green(c1) - Color.green(c2)
    val bDiff = Color.blue(c1) - Color.blue(c2)
    return Math.sqrt((rDiff * rDiff + gDiff * gDiff + bDiff * bDiff).toDouble())
}

fun renderIconBitmap(
    source: Bitmap,
    makeBlack: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    exportScale: Int
): Bitmap {
    val baseSize = 108 * exportScale
    val innerFrame = 72 * exportScale
    val out = Bitmap.createBitmap(baseSize, baseSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val bmp = if (makeBlack) source.toSmartMonochromeBlack() else source

    val fitScale = minOf(innerFrame / bmp.width.toFloat(), innerFrame / bmp.height.toFloat())
    val drawW = (bmp.width * fitScale * scale).toInt()
    val drawH = (bmp.height * fitScale * scale).toInt()

    val cx = baseSize / 2f + offsetX * exportScale
    val cy = baseSize / 2f + offsetY * exportScale
    val left = (cx - drawW / 2f).toInt()
    val top = (cy - drawH / 2f).toInt()

    val rect = Rect(left, top, left + drawW, top + drawH)
    canvas.drawBitmap(bmp, null, rect, paint)

    return out
}

fun exportSingleFile(
    context: Context,
    sourceBitmap: Bitmap,
    makeBlack: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    exportScale: Int,
    format: String
): Uri? {
    val filename = "ic_monochrome_${System.currentTimeMillis()}.$format"
    val mimeType = if (format == "svg") "image/svg+xml" else "image/png"

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
    resolver.openOutputStream(uri)?.use { out ->
        if (format == "svg") {
            val svgString = bitmapToSvg(renderIconBitmap(sourceBitmap, makeBlack, scale, offsetX, offsetY, exportScale))
            out.write(svgString.toByteArray())
        } else {
            val bmp = renderIconBitmap(sourceBitmap, makeBlack, scale, offsetX, offsetY, exportScale)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, cv, null, null)
    }

    return uri
}

fun bitmapToSvg(bitmap: Bitmap): String {
    val w = bitmap.width
    val h = bitmap.height
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
    sb.append("<svg width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\" xmlns=\"http://www.w3.org/2000/svg\">\n")
    sb.append("  <path d=\"")

    for (y in 0 until h) {
        var x = 0
        while (x < w) {
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) > 128) {
                val startX = x
                while (x < w && Color.alpha(bitmap.getPixel(x, y)) > 128) {
                    x++
                }
                // Draw a rectangle for this horizontal run of pixels
                sb.append("M$startX ${y}h${x - startX}v1h-${x - startX}z ")
            } else {
                x++
            }
        }
    }

    sb.append("\" fill=\"#000000\"/>\n")
    sb.append("</svg>")
    return sb.toString()
}
