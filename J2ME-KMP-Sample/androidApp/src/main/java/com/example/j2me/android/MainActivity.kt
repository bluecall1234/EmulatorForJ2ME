package com.example.j2me.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import emulator.core.SimpleKMPInterpreter
import emulator.core.JarLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init File Logging (intercepts stdout and stderr)
        emulator.core.setupFileLogging(this)

        // FIX #6: Wrap everything in try-catch so crash info is shown on screen
        var statusLog = ""

        try {
            // 1. Initialize Interpreter
            statusLog += "Step 1: Creating interpreter...\n"
            val interpreter = SimpleKMPInterpreter()

            // 2. Use JarLoader to create a realistic mock .class file
            statusLog += "Step 2: Loading mock class...\n"
            val loader = JarLoader()
            val classFile = loader.loadClassFromJar("demo/game", "MyTestGame")
            interpreter.loadClass(classFile.className, classFile.bytes)

            // 3. Call down to C++ to test Native JNI connection
            statusLog += "Step 3: NativeBridge.initSDL()...\n"
            NativeBridge.initSDL()

            // 4. Test the new Graphics Bridge directly
            statusLog += "Step 4: NativeGraphicsBridge.initSDL()...\n"
            emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.initSDL()
            emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.clearScreen()

            statusLog += "Step 5: Testing fillRect JNI...\n"
            emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.fillRect(
                10, 10, 50, 50, 0xFFFF0000.toInt()
            )
            emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.presentScreen()

            // 6. Execute the "startApp" method via bytecode interpreter
            statusLog += "Step 6: Executing startApp()...\n"
            val result = interpreter.executeMethod(
                classFile.className, "startApp", emptyArray()
            )

            // 7. Also run the constructor
            statusLog += "Step 7: Executing <init>()...\n"
            interpreter.executeMethod(
                classFile.className, "<init>", emptyArray()
            )

            statusLog += "\n✅ ALL STEPS COMPLETED SUCCESSFULLY!"
            println("[MainActivity] All steps completed successfully.")

            setContent {
                Text(
                    "J2ME KMP Emulator: ✅ SUCCESS\n\n" +
                    "Class: ${classFile.resolvedClassName}\n" +
                    "Super: ${classFile.resolvedSuperClassName}\n" +
                    "Methods: ${classFile.methods.size}\n" +
                    "Pool entries: ${classFile.constantPool.entries.size}\n\n" +
                    "startApp() result: $result\n" +
                    "Native Bridge: Connected\n\n" +
                    "--- Execution Log ---\n$statusLog"
                )
            }

        } catch (e: Throwable) {
            // Write standard stack trace which will now be intercepted to emulator_log.txt
            println("[MainActivity] CRASH DETECTED")
            e.printStackTrace()

            setContent {
                Text(
                    "J2ME KMP Emulator: 🔴 CRASHED\n\n" +
                    "--- Progress Before Crash ---\n$statusLog\n" +
                    "--- Error ---\n${e::class.simpleName}: ${e.message}\n\n" +
                    "--- Stack Trace ---\n${e.stackTraceToString()}"
                )
            }
        }
    }
}
