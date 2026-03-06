package emulator.core.api.javax.microedition.lcdui

/**
 * Expected interface to bridge J2ME graphics calls to the native SDL2 library.
 * The actual implementations will be provided in androidMain and iosMain.
 */
expect object NativeGraphicsBridge {
    /**
     * Initializes the SDL2 renderer and resources.
     */
    fun initSDL()

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
}
