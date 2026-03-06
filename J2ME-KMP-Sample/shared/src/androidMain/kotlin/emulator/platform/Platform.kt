package emulator.platform

import android.util.Log

/**
 * Implementation thực tế (actual) cho Android sử dụng Logcat
 */
actual class PlatformLogger actual constructor() {
    actual fun log(message: String) {
        Log.d("J2ME-Emulator", message)
    }
}

/**
 * Implementation thực tế cho Android (đang để trống logic để build pass)
 */
actual class AudioPlayer actual constructor() {
    actual fun playMidi(data: ByteArray) {
        // Trong tương lai sẽ gọi MediaPlayer hoặc MidiDriver
    }
    
    actual fun stop() {
        // Stop audio
    }
}
