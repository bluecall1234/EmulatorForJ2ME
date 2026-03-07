package emulator.core.api.javax.microedition.lcdui

/**
 * iOS implementation of NativeGraphicsBridge.
 * This will use C-Interop to call SDL2 directly.
 */
actual object NativeGraphicsBridge {
    
    actual fun initSDL(width: Int, height: Int) {
        // TODO: call sdl2.SDL_Init(sdl2.SDL_INIT_VIDEO...)
        println("[iOS NativeGraphicsBridge] initSDL: $width x $height")
    }

    actual fun setSurface(surface: Any?) {
        // Surface is an Android-only concept. Ignore on iOS.
    }

    actual fun clearScreen() {
        // TODO: call sdl2.SDL_RenderClear(renderer)
        println("[iOS NativeGraphicsBridge] clearScreen")
    }

    actual fun presentScreen() {
        // TODO: call sdl2.SDL_RenderPresent(renderer)
        println("[iOS NativeGraphicsBridge] presentScreen")
    }

    actual fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        // TODO: call sdl2.SDL_SetRenderDrawColor and sdl2.SDL_RenderFillRect
        println("[iOS NativeGraphicsBridge] fillRect")
    }

    actual fun setClip(x: Int, y: Int, w: Int, h: Int) {
        println("[iOS NativeGraphicsBridge] setClip")
    }

    actual fun drawImage(imageRgb: IntArray, imgW: Int, imgH: Int, x: Int, y: Int, anchor: Int) {
        println("[iOS NativeGraphicsBridge] drawImage")
    }

    actual fun drawString(text: String, x: Int, y: Int, color: Int) {
        println("[iOS NativeGraphicsBridge] drawString")
    }
}
