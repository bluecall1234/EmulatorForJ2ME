package emulator.core

/**
 * Utility for loading .class files and general resources from JAR archives.
 * The implementation is platform-specific.
 */
expect class JarLoader() {
    /**
     * @param filePath Absolute path to the JAR file on disk
     * @param className Fully qualified class name to extract (e.g. "demo.game.MyTestGame")
     */
    fun loadClassFromJar(filePath: String, className: String): JavaClassFile

    /**
     * @param filePath Absolute path to the JAR file on disk
     * @param resourceName Path to the resource inside the JAR (e.g. "/icon.png" or "data.bin")
     */
    fun loadResource(filePath: String, resourceName: String): ByteArray?
}
