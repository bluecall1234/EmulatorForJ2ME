package emulator.platform

/**
 * Sử dụng pattern expect/actual của KMP để xử lý các thành phần phụ thuộc OS
 */
expect class PlatformLogger() {
    fun log(message: String)
}

expect class AudioPlayer() {
    fun playMidi(data: ByteArray)
    fun stop()
}

// File này sẽ được implement tại shared/src/androidMain và shared/src/iosMain
// Ví dụ: bên android sẽ gọi Android MediaPlayer, bên iOS gọi AVFoundation
