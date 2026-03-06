package emulator.core.api.javax.microedition.lcdui

/**
 * iOS implementation of NativeGraphicsBridge.
 * This will use C-Interop to call SDL2 directly.
 */
actual object NativeGraphicsBridge {
    
    actual fun initSDL() {
        // TODO: call sdl2.SDL_Init(sdl2.SDL_INIT_VIDEO...)
        println("[iOS NativeGraphicsBridge] initSDL")
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
}
