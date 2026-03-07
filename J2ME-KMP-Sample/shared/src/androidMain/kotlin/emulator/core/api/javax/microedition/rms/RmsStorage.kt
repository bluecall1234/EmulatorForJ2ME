package emulator.core.api.javax.microedition.rms

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception

actual class RmsStorage actual constructor(private val storeName: String) {

    // Simple hack to get reference to the application context. In a real architecture, we inject this.
    // For now, emulator core needs a way to resolve paths without changing constructor signatures.
    companion object {
        var appContext: Context? = null 
    }

    private fun getRmsDir(): File {
        val dir = File(appContext?.filesDir, "rms")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getStoreFile(): File {
        return File(getRmsDir(), "$storeName.rms")
    }

    @Suppress("UNCHECKED_CAST")
    actual fun readStore(): Map<Int, ByteArray>? {
        val file = getStoreFile()
        if (!file.exists()) return null

        return try {
            ObjectInputStream(FileInputStream(file)).use { ois ->
                ois.readObject() as? Map<Int, ByteArray>
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun writeStore(records: Map<Int, ByteArray>) {
        val file = getStoreFile()
        try {
            ObjectOutputStream(FileOutputStream(file)).use { oos ->
                oos.writeObject(records)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun deleteStore() {
        val file = getStoreFile()
        if (file.exists()) {
            file.delete()
        }
    }
}
