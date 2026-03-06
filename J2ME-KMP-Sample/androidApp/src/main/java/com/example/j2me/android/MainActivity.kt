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
        
        // 1. Initialize Interpreter
        val interpreter = SimpleKMPInterpreter()
        
        // 2. Use JarLoader to create a realistic mock .class file
        val loader = JarLoader()
        val classFile = loader.loadClassFromJar("demo/game", "MyTestGame")
        interpreter.loadClass(classFile.className, classFile.bytes)

        // 3. Call down to C++ to test Native JNI connection
        NativeBridge.initSDL()
        
        // --- Test the new Graphics Bridge directly ---
        emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.initSDL()
        emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.clearScreen()
        // Draw a red square to test FillRect JNI mapping
        emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.fillRect(10, 10, 50, 50, 0xFFFF0000.toInt())
        emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.presentScreen()
        // ---------------------------------------------

        // 4. Execute the "startApp" method via the bytecode interpreter!
        // This now runs real JVM bytecode: get SystemTime -> PrintStream
        val result = interpreter.executeMethod(
            classFile.className, "startApp", emptyArray()
        )

        // 5. Also run the constructor
        interpreter.executeMethod(
            classFile.className, "<init>", emptyArray()
        )

        setContent {
            Text("J2ME KMP Emulator: Bytecode Engine Running!\n\n" +
                 "Class: ${classFile.resolvedClassName}\n" +
                 "Super: ${classFile.resolvedSuperClassName}\n" +
                 "Methods: ${classFile.methods.size}\n" +
                 "Pool entries: ${classFile.constantPool.entries.size}\n\n" +
                 "startApp() executed: result=$result\n" +
                 "Native Bridge: Connected\n\n" +
                 "Check Logcat for opcode-by-opcode trace!")
        }
    }
}
