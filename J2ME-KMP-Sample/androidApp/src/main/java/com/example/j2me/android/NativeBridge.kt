package com.example.j2me.android

/**
 * Bridge class calling down to C++ (JNI).
 * This object manages communication with the SDL2 library on the Android platform.
 */
object NativeBridge {
    
    // Load the compiled C++ library (libj2me_native.so)
    init {
        System.loadLibrary("j2me_native")
    }

    /**
     * Function calling down to C++ to initialize SDL2.
     * external: Informs Kotlin that this function is written in another language (C/C++).
     */
    external fun initSDL()
}
