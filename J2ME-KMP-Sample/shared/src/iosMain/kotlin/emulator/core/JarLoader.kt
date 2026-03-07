package emulator.core

import emulator.core.JavaClassFile

/**
 * Simple iOS stub for JarLoader
 */
actual class JarLoader actual constructor() {
    actual fun loadClassFromJar(filePath: String, className: String): JavaClassFile {
        throw UnsupportedOperationException("Not yet implemented for iOS")
    }

    actual fun loadResource(filePath: String, resourceName: String): ByteArray? {
        throw UnsupportedOperationException("Not yet implemented for iOS")
    }
}
