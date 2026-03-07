package emulator.core.api

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

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
        registerJavaLangThread()
        registerJavaLangStringBuffer()
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

    private fun registerJavaLangStringBuffer() {
        nativeMethods["java/lang/StringBuffer.<init>:()V"] = { frame ->
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer <init> called on null or invalid object")
            sbObj.instanceFields["value"] = ""
        }

        nativeMethods["java/lang/StringBuffer.<init>:(Ljava/lang/String;)V"] = { frame ->
            val arg = frame.pop()
            val str = if (arg is String) {
                arg
            } else if (arg is emulator.core.memory.HeapObject) {
                 arg.instanceFields["value"] as? String ?: ""
            } else {
                ""
            }
            
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer <init>(String) called on null or invalid object")
            sbObj.instanceFields["value"] = str
        }
        
        nativeMethods["java/lang/StringBuffer.<init>:(I)V"] = { frame ->
            val capacity = frame.popInt() // Just consume the capacity argument
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer <init>(I) called on null or invalid object")
            sbObj.instanceFields["value"] = ""
        }

        nativeMethods["java/lang/StringBuffer.append:(Ljava/lang/String;)Ljava/lang/StringBuffer;"] = { frame ->
            val arg = frame.pop()
            val str = if (arg is String) {
                arg
            } else if (arg is emulator.core.memory.HeapObject) {
                 // A real java.lang.String object. For now we will just use toString unless we emulate String objects fully.
                 arg.instanceFields["value"] as? String ?: "null"
            } else {
                "null"
            }
            
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(String) called on null or invalid object")
            
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + str
            frame.push(sbObj) // return this
        }

        nativeMethods["java/lang/StringBuffer.append:(I)Ljava/lang/StringBuffer;"] = { frame ->
            val i = frame.popInt()
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(Int) called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + i.toString()
            frame.push(sbObj)
        }

        nativeMethods["java/lang/StringBuffer.toString:()Ljava/lang/String;"] = { frame ->
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer toString called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            frame.push(current)
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
        nativeMethods["javax/microedition/lcdui/Graphics.drawString:(Ljava/lang/String;III)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.drawString(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.setClip:(IIII)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.setClip(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.drawImage:(Ljavax/microedition/lcdui/Image;III)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.drawImage(frame)
        }
    }

    private fun registerJavaLangThread() {
        // Thread(Runnable r)
        nativeMethods["java/lang/Thread.<init>:(Ljava/lang/Runnable;)V"] = { frame ->
            val runnableInfo = frame.pop() // The Runnable object (HeapObject)
            val threadObj = frame.pop() as emulator.core.memory.HeapObject // The Thread object
            threadObj.instanceFields["target"] = runnableInfo
        }

        // Thread(String s)
        nativeMethods["java/lang/Thread.<init>:(Ljava/lang/String;)V"] = { frame ->
            val strObj = frame.pop()
            val threadObj = frame.pop() as emulator.core.memory.HeapObject
            threadObj.instanceFields["name"] = strObj
        }

        // Thread()
        nativeMethods["java/lang/Thread.<init>:()V"] = { frame ->
            val threadObj = frame.pop() as emulator.core.memory.HeapObject
        }
        
        nativeMethods["java/lang/Thread.start:()V"] = { frame ->
            val threadObj = frame.pop() as emulator.core.memory.HeapObject
            
            // In KMP we can use GlobalScope to spawn a background coroutine
            // Since ExecutionEngine runs inside the current thread, we'll dispatch it
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val target = threadObj.instanceFields["target"] as? emulator.core.memory.HeapObject
                        ?: threadObj // If no target given, Thread runs its own run() method
                    
                    val interpreter = frame.interpreter
                    // Invoke public void run()
                    interpreter.executeMethod(target.className, "run", "()V", arrayOf(target))
                } catch (e: Exception) {
                    println("[Thread] Crash in game loop: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        
        nativeMethods["java/lang/Thread.sleep:(J)V"] = { frame ->
            val time = frame.popLong()
            // This is tricky: we are inside a blocking call in KMP but we shouldn't block main.
            // However, J2ME Thread.sleep is meant to block the CURRENT Thread. 
            // In JVM backend we can use Thread.sleep(). In KMP common we can use:
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.delay(time)
            }
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
