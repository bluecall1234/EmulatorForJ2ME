package emulator.core.api.java.lang

internal actual fun getCurrentTimeMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
