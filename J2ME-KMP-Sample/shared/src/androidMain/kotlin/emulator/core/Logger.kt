package emulator.core

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

/**
 * Android implementation of setupFileLogging.
 * Redirects System.out and System.err to a file in the app's external data directory,
 * while still printing to Logcat.
 */
actual fun setupFileLogging(context: Any?) {
    val androidContext = context as? Context ?: run {
        println("setupFileLogging failed: context is not android.content.Context")
        return
    }

    try {
        // Path: /storage/emulated/0/Android/data/com.example.j2me.android/files/emulator_log.txt
        val externalDir = androidContext.getExternalFilesDir(null) ?: return
        val logFile = File(externalDir, "emulator_log.txt")
        
        // false = overwrite file on each app launch
        val fileOut = PrintStream(FileOutputStream(logFile, false))
        
        val consoleOut = System.out
        val consoleErr = System.err
        
        val dualOut = object : PrintStream(fileOut) {
            override fun write(b: Int) { 
                super.write(b)
                consoleOut.write(b) 
            }
            override fun write(buf: ByteArray, off: Int, len: Int) { 
                super.write(buf, off, len)
                consoleOut.write(buf, off, len) 
            }
        }
        
        val dualErr = object : PrintStream(fileOut) {
            override fun write(b: Int) { 
                super.write(b)
                consoleErr.write(b) 
            }
            override fun write(buf: ByteArray, off: Int, len: Int) { 
                super.write(buf, off, len)
                consoleErr.write(buf, off, len) 
            }
        }
        
        System.setOut(dualOut)
        System.setErr(dualErr)
        
        println("=== [Android] Dual Logger Started ===")
        println("Log File: ${logFile.absolutePath}")
        
    } catch (e: Exception) {
        println("Failed to setup dual logging: ${e.message}")
    }
}
