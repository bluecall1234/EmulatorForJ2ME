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

    external fun nativeInitSDL()
    external fun nativeClearScreen()
    external fun nativePresentScreen()
    external fun nativeFillRect(x: Int, y: Int, w: Int, h: Int, r: Int, g: Int, b: Int, a: Int)
}

actual object NativeGraphicsBridge {

    init {
        NativeGraphicsBridgeJni  // trigger static init → loads the .so
    }

    actual fun initSDL() {
        NativeGraphicsBridgeJni.nativeInitSDL()
    }

    actual fun clearScreen() {
        NativeGraphicsBridgeJni.nativeClearScreen()
    }

    actual fun presentScreen() {
        NativeGraphicsBridgeJni.nativePresentScreen()
    }

    actual fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        val a = (color shr 24) and 0xFF
        val alpha = if (a == 0 && (color and 0xFFFFFF) != 0) 255 else a
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        NativeGraphicsBridgeJni.nativeFillRect(x, y, w, h, r, g, b, alpha)
    }
}
