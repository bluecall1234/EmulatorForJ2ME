package emulator.core

/**
 * iOS stub implementation of setupFileLogging.
 * In the future, this will use CInterop `freopen` to redirect stdout to a file.
 */
actual fun setupFileLogging(context: Any?) {
    println("=== [iOS] Logger Started (Console only for now) ===")
}
