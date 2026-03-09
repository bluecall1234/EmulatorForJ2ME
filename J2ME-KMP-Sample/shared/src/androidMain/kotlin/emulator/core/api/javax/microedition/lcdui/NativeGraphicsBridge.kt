package emulator.core.api.javax.microedition.lcdui

/**
 * Android implementation of NativeGraphicsBridge.
 *
 * FIX #1: Library name corrected: "shared" → "j2me_native"
 * FIX #2: JNI externals declared in a standalone Jni helper object.
 *         The JNI function names in android_jni_bridge.cpp are updated
 *         to match this class: NativeGraphicsBridgeJni
 *           → Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeXxx
 */
internal object NativeGraphicsBridgeJni {
    init {
        try {
            // FIX #1: Load the correct library as declared in CMakeLists.txt
            System.loadLibrary("j2me_native")
        } catch (e: UnsatisfiedLinkError) {
            println("[NativeGraphicsBridge] ERROR: Failed to load j2me_native: ${e.message}")
        }
    }

    external fun nativeInitSDL(width: Int, height: Int)
    external fun nativeSetSurface(surface: Any?)
    external fun nativeClearScreen()
    external fun nativePresentScreen()
    external fun nativeFillRect(x: Int, y: Int, w: Int, h: Int, r: Int, g: Int, b: Int, a: Int)
    external fun nativeSetClip(x: Int, y: Int, w: Int, h: Int)
    external fun nativeDrawImage(imageRgb: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, anchor: Int)
    external fun nativeDrawString(text: String, x: Int, y: Int, color: Int)
}

actual object NativeGraphicsBridge {

    init {
        NativeGraphicsBridgeJni  // trigger static init → loads the .so
    }

    actual fun initSDL(width: Int, height: Int) {
        NativeGraphicsBridgeJni.nativeInitSDL(width, height)
    }

    actual fun setSurface(surface: Any?) {
        NativeGraphicsBridgeJni.nativeSetSurface(surface)
    }

    actual fun clearScreen() {
        NativeGraphicsBridgeJni.nativeClearScreen()
    }

    private var frameCount = 0

    actual fun presentScreen() {
        frameCount++
        println("[JNI] presentScreen() called (frame #$frameCount)")
        NativeGraphicsBridgeJni.nativePresentScreen()
    }

    actual fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        val a = (color shr 24) and 0xFF
        val alpha = a
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        // if (color != -1) {
        //     println("[JNI] fillRect(x=$x, y=$y, w=$w, h=$h) color=0x${color.toString(16)} (ARGB: $alpha, $r, $g, $b)")
        // }
        NativeGraphicsBridgeJni.nativeFillRect(x, y, w, h, r, g, b, alpha)
    }

    actual fun setClip(x: Int, y: Int, w: Int, h: Int) {
        NativeGraphicsBridgeJni.nativeSetClip(x, y, w, h)
    }

    actual fun drawImage(imageRgb: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, anchor: Int) {
        println("[JNI] drawImage() called for ${imgW}x${imgH} at ($x, $y)")
        NativeGraphicsBridgeJni.nativeDrawImage(imageRgb, imgW, imgH, x, y, anchor)
    }

    actual fun drawString(text: String, x: Int, y: Int, anchor: Int, color: Int) {
        if (text.isEmpty()) return
        println("[NativeBridge] drawString: text=\"$text\", x=$x, y=$y, anchor=$anchor, color=0x${color.toString(16)}")

        val paint = android.graphics.Paint().apply {
            this.color = color or (0xFF shl 24).toInt() // Ensure opaque
            this.textSize = 24f // Increased for visibility
            this.isAntiAlias = true
        }

        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val fontMetrics = paint.fontMetrics
        val width = bounds.width()
        val height = (fontMetrics.descent - fontMetrics.ascent).toInt()

        if (width <= 0 || height <= 0) {
            println("[NativeBridge] drawString: Invalid dimensions ${width}x$height")
            return
        }

        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        canvas.drawText(text, -bounds.left.toFloat(), -fontMetrics.ascent, paint)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

        // Now calculate screen position based on J2ME anchor
        var drawX = x
        var drawY = y
        
        // ... (rest of the logic)

        // Constants from javax.microedition.lcdui.Graphics
        val HCENTER = 1
        val LEFT = 4
        val RIGHT = 8
        val TOP = 16
        val BOTTOM = 32
        val BASELINE = 64

        // Horizontal
        if ((anchor and HCENTER) != 0) {
            drawX -= width / 2
        } else if ((anchor and RIGHT) != 0) {
            drawX -= width
        }
        // Vertical
        if ((anchor and BOTTOM) != 0) {
            drawY -= height
        } else if ((anchor and BASELINE) != 0) {
            drawY += fontMetrics.ascent.toInt()
        } else if ((anchor and TOP) != 0) {
            // drawY is already top
        } else {
            // Default is TOP|LEFT
        }

        NativeGraphicsBridgeJni.nativeDrawImage(pixels, width, height, drawX, drawY, 0)
    }

    actual fun drawTextToBuffer(pixels: IntArray, width: Int, height: Int, text: String, x: Int, y: Int, anchor: Int, color: Int) {
        if (text.isEmpty()) return
        
        val paint = android.graphics.Paint().apply {
            this.color = color or (0xFF shl 24).toInt()
            this.textSize = 24f
            this.isAntiAlias = true
        }

        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val fontMetrics = paint.fontMetrics
        val txtW = bounds.width()
        val txtH = (fontMetrics.descent - fontMetrics.ascent).toInt()

        if (txtW <= 0 || txtH <= 0) return

        val textBitmap = android.graphics.Bitmap.createBitmap(txtW, txtH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(textBitmap)
        canvas.drawText(text, -bounds.left.toFloat(), -fontMetrics.ascent, paint)

        val textPixels = IntArray(txtW * txtH)
        textBitmap.getPixels(textPixels, 0, txtW, 0, 0, txtW, txtH)
        textBitmap.recycle()

        // Calculate position based on anchor
        var drawX = x
        var drawY = y
        val HCENTER = 1
        val RIGHT = 8
        val BOTTOM = 32
        val BASELINE = 64
        val TOP = 16

        if ((anchor and HCENTER) != 0) drawX -= txtW / 2
        else if ((anchor and RIGHT) != 0) drawX -= txtW
        
        if ((anchor and BOTTOM) != 0) drawY -= txtH
        else if ((anchor and BASELINE) != 0) drawY += fontMetrics.ascent.toInt()

        // Blend onto target buffer
        for (iy in 0 until txtH) {
            val dy = drawY + iy
            if (dy < 0 || dy >= height) continue
            for (ix in 0 until txtW) {
                val dx = drawX + ix
                if (dx < 0 || dx >= width) continue
                
                val pixel = textPixels[iy * txtW + ix]
                if (((pixel shr 24) and 0xFF) > 128) {
                    pixels[dy * width + dx] = pixel
                }
            }
        }
    }

    actual fun decodeImage(data: ByteArray): ImageInfo? {
        return decodeImage(data, 0, data.size)
    }

    actual fun decodeImage(data: ByteArray, offset: Int, length: Int): ImageInfo? {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, offset, length) ?: return null
            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()
            return ImageInfo(pixels, w, h)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    actual fun measureString(text: String): Int {
        if (text.isEmpty()) return 0
        val paint = android.graphics.Paint().apply {
            this.textSize = 24f
        }
        val width = paint.measureText(text).toInt()
        println("[NativeBridge] measureString: text=\"$text\" result=$width")
        return width
    }
}
