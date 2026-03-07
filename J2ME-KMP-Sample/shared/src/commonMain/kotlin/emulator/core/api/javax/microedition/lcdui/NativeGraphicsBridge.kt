package emulator.core.api.javax.microedition.lcdui

/**
 * Expected interface to bridge J2ME graphics calls to the native SDL2 library.
 * The actual implementations will be provided in androidMain and iosMain.
 */
expect object NativeGraphicsBridge {
    /**
     * Initializes the SDL2 renderer and resources with a specific dimension.
     * @param width The game's original width (e.g. 240)
     * @param height The game's original height (e.g. 320)
     */
    fun initSDL(width: Int = 240, height: Int = 320)

    /**
     * Pass the Android Surface to the C++ NDK renderer natively.
     * Expected to be called only on Android. iOS will ignore this.
     */
    fun setSurface(surface: Any?)

    /**
     * Clears the current screen to black (or a specified background color)
     */
    fun clearScreen()

    /**
     * Present the drawn buffer to the screen (equivalent to Canvas.repaint)
     */
    fun presentScreen()

    /**
     * Draws a filled rectangle on the screen.
     */
    fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int)

    /**
     * Set clipping region
     */
    fun setClip(x: Int, y: Int, w: Int, h: Int)

    /**
     * Draw Image (stubbed for now with dimensions and ARGB array if needed, 
     * but we'll pass simplified args for the JNI bridge)
     */
    fun drawImage(imageRgb: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, anchor: Int)

    /**
     * Draw String (stubbed to native font rendering or ignoring)
     */
    fun drawString(text: String, x: Int, y: Int, color: Int)

    /**
     * Decodes an image from a byte array (PNG, JPG, etc) into a raw ARGB pixel array.
     */
    fun decodeImage(data: ByteArray): ImageInfo?
}

data class ImageInfo(val pixels: IntArray, val width: Int, val height: Int)
