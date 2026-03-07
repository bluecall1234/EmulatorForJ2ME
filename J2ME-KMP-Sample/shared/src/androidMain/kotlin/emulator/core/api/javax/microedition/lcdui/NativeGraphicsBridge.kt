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
        if (frameCount % 60 == 0) {
            println("[JNI] presentScreen() called (frame #$frameCount)")
        }
        NativeGraphicsBridgeJni.nativePresentScreen()
    }

    actual fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        val a = (color shr 24) and 0xFF
        val alpha = if (a == 0 && (color and 0xFFFFFF) != 0) 255 else a
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        if (frameCount % 60 == 0) {
            println("[JNI] fillRect(x=$x, y=$y, w=$w, h=$h) color=0x${color.toString(16)} (ARGB: $alpha, $r, $g, $b)")
        }
        NativeGraphicsBridgeJni.nativeFillRect(x, y, w, h, r, g, b, alpha)
    }

    actual fun setClip(x: Int, y: Int, w: Int, h: Int) {
        NativeGraphicsBridgeJni.nativeSetClip(x, y, w, h)
    }

    actual fun drawImage(imageRgb: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, anchor: Int) {
        if (frameCount % 60 == 0) {
            println("[JNI] drawImage() called for ${imgW}x${imgH} at ($x, $y)")
        }
        NativeGraphicsBridgeJni.nativeDrawImage(imageRgb, imgW, imgH, x, y, anchor)
    }

    actual fun drawString(text: String, x: Int, y: Int, color: Int) {
        NativeGraphicsBridgeJni.nativeDrawString(text, x, y, color)
    }

    actual fun decodeImage(data: ByteArray): ImageInfo? {
        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size) ?: return null
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
}
