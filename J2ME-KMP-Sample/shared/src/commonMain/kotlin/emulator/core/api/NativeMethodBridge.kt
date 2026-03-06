package emulator.core.api

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * The Native Method Bridge acts as a router between the Bytecode Interpreter
 * and our Kotlin implementations of J2ME APIs (CLDC/MIDP).
 *
 * When the ExecutionEngine sees an INVOKESTATIC, INVOKEVIRTUAL, or INVOKESPECIAL
 * instruction that targets a known API class (like "java/lang/System"),
 * it delegates the call to this bridge instead of trying to parse missing bytecode.
 */
object NativeMethodBridge {

    // A map of "className.methodName:descriptor" -> Native handler function
    // The handler function takes the ExecutionFrame, pops arguments from the stack,
    // and optionally pushes a return value.
    private val nativeMethods = mutableMapOf<String, (ExecutionFrame) -> Unit>()

    init {
        // Register core implementations
        registerJavaLangSystem()
        registerJavaIoPrintStream()

        // Register MIDP UI
        registerJavaxMicroeditionLcduiDisplay()
        registerJavaxMicroeditionLcduiCanvas()
        registerJavaxMicroeditionLcduiGraphics()
    }

    /**
     * Attempts to execute a native method intercepted from bytecode.
     *
     * FIX #4: Added [isStatic] parameter so the fallback unhandled-method path
     * knows whether to pop 'this' from the stack or not.
     * Previously, the code always popped 'this' for any non-init method even for
     * INVOKESTATIC (which has no 'this' on stack), causing EmptyStackException.
     *
     * @param isStatic true if called via INVOKESTATIC (no implicit 'this' on stack)
     * @return true if the method was handled (native), false for normal bytecode dispatch.
     */
    fun callNativeMethod(
        className: String,
        methodName: String,
        descriptor: String,
        frame: ExecutionFrame,
        isStatic: Boolean = false   // FIX #4: new parameter
    ): Boolean {
        val key = "$className.$methodName:$descriptor"
        val handler = nativeMethods[key]

        if (handler != null) {
            handler.invoke(frame)
            return true
        }

        // Fallback: intercept unimplemented java.* / javax.* calls to prevent
        // the interpreter from trying (and failing) to load their .class files.
        if (className.startsWith("java/") || className.startsWith("javax/")) {
            println("  [NativeBridge] WARNING: Unhandled native call: $key")

            // 1. Pop method arguments first
            popArguments(descriptor, frame)

            // FIX #4: Only pop 'this' for non-static calls (INVOKEVIRTUAL / INVOKESPECIAL)
            if (!isStatic) {
                if (frame.stackSize() > 0) {
                    frame.pop() // pop implicit 'this' reference
                } else {
                    println("  [NativeBridge] WARNING: Expected 'this' on stack for $key but stack is empty!")
                }
            }

            // 2. Push a dummy return value for non-void methods
            if (!descriptor.endsWith(")V")) {
                frame.push(getDefaultReturnValue(descriptor))
            }
            return true
        }

        return false
    }

    private fun getDefaultReturnValue(descriptor: String): Any? {
        val retTypeIndex = descriptor.lastIndexOf(')') + 1
        if (retTypeIndex <= 0 || retTypeIndex >= descriptor.length) return 0
        
        return when (descriptor[retTypeIndex]) {
            'B', 'S', 'I', 'Z', 'C' -> 0
            'J' -> 0L
            'F' -> 0.0f
            'D' -> 0.0
            'V' -> null // Should not be called for V
            else -> null // 'L' (Object) or '[' (Array) -> Reference types default to null
        }
    }

    private fun registerJavaLangSystem() {
        // public static long currentTimeMillis()
        nativeMethods["java/lang/System.currentTimeMillis:()J"] = { frame ->
            emulator.core.api.java.lang.System.currentTimeMillis(frame)
        }
    }

    private fun registerJavaIoPrintStream() {
        // public void println(String x)
        nativeMethods["java/io/PrintStream.println:(Ljava/lang/String;)V"] = { frame ->
            emulator.core.api.java.io.PrintStream.println(frame)
        }
    }

    private fun registerJavaxMicroeditionLcduiDisplay() {
        nativeMethods["javax/microedition/lcdui/Display.getDisplay:(Ljavax/microedition/midlet/MIDlet;)Ljavax/microedition/lcdui/Display;"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Display.getDisplay(frame)
        }
        nativeMethods["javax/microedition/lcdui/Display.setCurrent:(Ljavax/microedition/lcdui/Displayable;)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Display.setCurrent(frame)
        }
    }

    private fun registerJavaxMicroeditionLcduiCanvas() {
        nativeMethods["javax/microedition/lcdui/Canvas.repaint:()V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Canvas.repaint(frame)
        }
        nativeMethods["javax/microedition/lcdui/Canvas.setFullScreenMode:(Z)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Canvas.setFullScreenMode(frame)
        }
    }

    private fun registerJavaxMicroeditionLcduiGraphics() {
        nativeMethods["javax/microedition/lcdui/Graphics.setColor:(I)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.setColor(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.setColor:(III)V"] = { frame ->
            // Overload: setColor(int red, int green, int blue)
            emulator.core.api.javax.microedition.lcdui.Graphics.setColorRGB(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.fillRect:(IIII)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.fillRect(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.drawImage:(Ljavax/microedition/lcdui/Image;III)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.drawImage(frame)
        }
    }

    /**
     * Pops method arguments from the frame stack based on the JVM method descriptor.
     * Must be called BEFORE popping 'this' (if any).
     */
    private fun popArguments(descriptor: String, frame: ExecutionFrame) {
        var i = 1 // skip opening '('
        while (i < descriptor.length && descriptor[i] != ')') {
            when (descriptor[i]) {
                'B', 'C', 'I', 'S', 'Z', 'F', 'J', 'D' -> { frame.pop(); i++ }
                'L' -> {
                    frame.pop()
                    i = descriptor.indexOf(';', i) + 1
                }
                '[' -> { i++ } // array prefix; the base type is handled next
                else -> i++
            }
        }
    }
}
