package emulator.core

import emulator.core.JavaClassFile
import java.io.File
import java.util.zip.ZipFile

/**
 * Android implementation of JarLoader using java.util.zip.ZipFile
 */
actual class JarLoader actual constructor() {
    actual fun loadClassFromJar(filePath: String, className: String): JavaClassFile {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("JAR file not found: $filePath")
        }

        val zipFile = ZipFile(file)
        val entryName = className.replace('.', '/') + ".class"
        
        val entry = zipFile.getEntry(entryName) 
            ?: throw IllegalArgumentException("Class $entryName not found in JAR $filePath")

        val bytes = zipFile.getInputStream(entry).use { it.readBytes() }
        zipFile.close()

        return JavaClassFile(className, bytes)
    }

    /**
     * Reads META-INF/MANIFEST.MF to find the main MIDlet class
     */
    fun getMainClassFromManifest(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists()) return null

        val zipFile = ZipFile(file)
        val entry = zipFile.getEntry("META-INF/MANIFEST.MF") ?: return null

        val manifestText = zipFile.getInputStream(entry).use { String(it.readBytes()) }
        zipFile.close()

        // Look for MIDlet-1: Name, Icon, ClassName
        val lines = manifestText.split("\n", "\r\n")
        for (line in lines) {
            if (line.startsWith("MIDlet-1:")) {
                val parts = line.substringAfter(":").split(",")
                if (parts.size >= 3) {
                    return parts[2].trim()
                }
            }
        }
        return null
    }

    actual fun loadResource(filePath: String, resourceName: String): ByteArray? {
        val file = File(filePath)
        if (!file.exists()) return null

        val zipFile = ZipFile(file)
        // Normalize name: remove leading slash if present
        val entryName = if (resourceName.startsWith("/")) resourceName.substring(1) else resourceName
        val entry = zipFile.getEntry(entryName)
        if (entry == null) {
            println("[JarLoader] Resource NOT FOUND: $entryName in $filePath")
            zipFile.close()
            return null
        }

        println("[JarLoader] Loading resource: $entryName (${entry.size} bytes)")
        val bytes = zipFile.getInputStream(entry).use { it.readBytes() }
        zipFile.close()
        return bytes
    }
}
