package emulator.core.api.java.lang

/**
 * Returns the current time in milliseconds.
 * This is implemented differently on Android (JVM) and iOS (Native).
 */
internal expect fun getCurrentTimeMillis(): Long
