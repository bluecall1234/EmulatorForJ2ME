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
        println("=== [NativeBridge] Initializing Native Method Bridge (Plan 19 Active) ===")
        // Register core implementations
        registerJavaLangSystem()
        registerJavaLangThread()
        registerJavaLangClass()
        registerJavaIoInputStream()
        registerJavaLangStringBuffer()
        registerJavaIoPrintStream()
        registerJavaLangMath()
        registerJavaLangRandom()
        registerJavaLangString()
        registerJavaIoDataInputStream()
        registerJavaLangInteger()
        registerJavaLangBoolean()

        // Register MIDP UI
        registerJavaxMicroeditionLcduiDisplay()
        registerJavaxMicroeditionLcduiImage()
        registerJavaxMicroeditionLcduiGraphics()
        registerJavaxMicroeditionLcduiFont()
        registerJavaxMicroeditionLcduiCanvas()
        registerJavaxMicroeditionLcduiDisplayable()
        
        // Register RMS
        registerJavaxMicroeditionRmsRecordStore()
        registerJavaxMicroeditionMidlet()
        registerJavaxMicroeditionMedia()
        registerComNokiaMidUi()
        registerJavaxMicroeditionLcduiGameCanvas()
        registerJavaUtil()
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
            if (className != "java/io/InputStream" && methodName != "read") {
                println("[NativeBridge] Executing: $key")
            }
            handler.invoke(frame)
            return true
        }

        println("[NativeBridge] Lookup failed for: $key")
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

    private fun getString(obj: Any?): String {
        return when (obj) {
            is String -> obj
            is HeapObject -> {
                if (obj.className == "java/lang/String") {
                    obj.instanceFields["value"] as? String ?: ""
                } else {
                    ""
                }
            }
            else -> ""
        }
    }

    private fun registerJavaLangSystem() {
        nativeMethods["java/lang/System.currentTimeMillis:()J"] = { frame ->
            emulator.core.api.java.lang.System.currentTimeMillis(frame)
        }
        
        // public static String getProperty(String key)
        nativeMethods["java/lang/System.getProperty:(Ljava/lang/String;)Ljava/lang/String;"] = { frame ->
            val key = frame.pop() as? String ?: ""
            println("[NativeBridge] System.getProperty(\"$key\") called")
            val value = when(key) {
                "microedition.platform" -> "KMP-Emulator"
                "microedition.configuration" -> "CLDC-1.1"
                "microedition.profiles" -> "MIDP-2.0"
                "microedition.encoding" -> "UTF-8"
                "microedition.locale" -> "en-US"
                "microedition.m3g.version" -> "1.1"
                "microedition.media.version" -> "1.0"
                "microedition.smartcardslots" -> "0"
                "microedition.commports" -> ""
                "microedition.hostname" -> "localhost"
                else -> null
            }
            frame.push(value)
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

        nativeMethods["java/lang/StringBuffer.append:(F)Ljava/lang/StringBuffer;"] = { frame ->
            val f = frame.pop() as? Float ?: 0.0f
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(Float) called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + f.toString()
            frame.push(sbObj)
        }

        nativeMethods["java/lang/StringBuffer.append:(J)Ljava/lang/StringBuffer;"] = { frame ->
            val l = frame.popLong()
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(Long) called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + l.toString()
            frame.push(sbObj)
        }
        // java/lang/StringBuffer.append:(C)Ljava/lang/StringBuffer;
        nativeMethods["java/lang/StringBuffer.append:(C)Ljava/lang/StringBuffer;"] = { frame ->
            val char = frame.popInt().toChar()
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(C) called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + char
            frame.push(sbObj)
        }

        // java/lang/StringBuffer.append:(Ljava/lang/Object;)Ljava/lang/StringBuffer;
        nativeMethods["java/lang/StringBuffer.append:(Ljava/lang/Object;)Ljava/lang/StringBuffer;"] = { frame ->
            val obj = frame.pop()
            val str = if (obj is String) obj else (obj as? emulator.core.memory.HeapObject)?.let { getString(it) } ?: obj.toString()
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(Object) called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + str
            frame.push(sbObj)
        }
        
        nativeMethods["java/lang/StringBuffer.append:(Z)Ljava/lang/StringBuffer;"] = { frame ->
            val b = frame.popInt() != 0
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer append(Boolean) called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            sbObj.instanceFields["value"] = current + b.toString()
            frame.push(sbObj)
        }

        nativeMethods["java/lang/StringBuffer.toString:()Ljava/lang/String;"] = { frame ->
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer toString called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            frame.push(current)
        }

        // java/lang/StringBuffer.length:()I
        nativeMethods["java/lang/StringBuffer.length:()I"] = { frame ->
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer length called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            frame.push(current.length)
        }

        // java/lang/StringBuffer.setLength:(I)V
        nativeMethods["java/lang/StringBuffer.setLength:(I)V"] = { frame ->
            val newLength = frame.popInt()
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer setLength called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            if (newLength <= current.length) {
                sbObj.instanceFields["value"] = current.substring(0, newLength)
            } else {
                sbObj.instanceFields["value"] = current + " ".repeat(newLength - current.length) // pad with spaces as a simple implementation
            }
        }

        // java/lang/StringBuffer.delete:(II)Ljava/lang/StringBuffer;
        nativeMethods["java/lang/StringBuffer.delete:(II)Ljava/lang/StringBuffer;"] = { frame ->
            val endIndex = frame.popInt()
            val beginIndex = frame.popInt()
            val sbObj = frame.pop() as? emulator.core.memory.HeapObject 
                ?: throw RuntimeException("NullPointerException: StringBuffer delete called on null or invalid object")
            val current = sbObj.instanceFields["value"] as? String ?: ""
            if (beginIndex >= 0 && beginIndex <= current.length && endIndex >= beginIndex) {
                val realEnd = minOf(endIndex, current.length)
                sbObj.instanceFields["value"] = current.substring(0, beginIndex) + current.substring(realEnd)
            }
            frame.push(sbObj)
        }
    }

    private fun registerJavaxMicroeditionLcduiDisplay() {
        nativeMethods["javax/microedition/lcdui/Display.getDisplay:(Ljavax/microedition/midlet/MIDlet;)Ljavax/microedition/lcdui/Display;"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Display.getDisplay(frame)
        }
        nativeMethods["javax/microedition/lcdui/Display.setCurrent:(Ljavax/microedition/lcdui/Displayable;)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Display.setCurrent(frame)
        }
        nativeMethods["javax/microedition/lcdui/Display.getCurrent:()Ljavax/microedition/lcdui/Displayable;"] = { frame ->
            frame.pop() // this
            frame.push(emulator.core.api.javax.microedition.lcdui.Display.activeDisplayable)
        }
        nativeMethods["javax/microedition/lcdui/Display.vibrate:(I)Z"] = { frame ->
            frame.popInt() // duration
            frame.pop() // this
            frame.push(1) // Succesfull
        }
        nativeMethods["javax/microedition/lcdui/Display.isColor:()Z"] = { frame ->
            frame.pop() // this
            frame.push(1)
        }
        nativeMethods["javax/microedition/lcdui/Display.numColors:()I"] = { frame ->
            frame.pop() // this
            frame.push(65536)
        }
    }

    private fun registerJavaLangMath() {
        // Math.min(int, int)
        nativeMethods["java/lang/Math.min:(II)I"] = { frame ->
            val b = frame.popInt(); val a = frame.popInt()
            frame.push(minOf(a, b))
        }
        // Math.min(long, long)
        nativeMethods["java/lang/Math.min:(JJ)J"] = { frame ->
            val b = frame.popLong(); val a = frame.popLong()
            frame.push(minOf(a, b))
        }
        // Math.min(float, float)
        nativeMethods["java/lang/Math.min:(FF)F"] = { frame ->
            val b = frame.pop() as Float; val a = frame.pop() as Float
            frame.push(minOf(a, b))
        }
        // Math.max(int, int)
        nativeMethods["java/lang/Math.max:(II)I"] = { frame ->
            val b = frame.popInt(); val a = frame.popInt()
            frame.push(maxOf(a, b))
        }
        // Math.max(long, long)
        nativeMethods["java/lang/Math.max:(JJ)J"] = { frame ->
            val b = frame.popLong(); val a = frame.popLong()
            frame.push(maxOf(a, b))
        }
        // Math.abs(int)
        nativeMethods["java/lang/Math.abs:(I)I"] = { frame ->
            frame.push(kotlin.math.abs(frame.popInt()))
        }
        // Math.abs(long)
        nativeMethods["java/lang/Math.abs:(J)J"] = { frame ->
            frame.push(kotlin.math.abs(frame.popLong()))
        }
        // Math.abs(float)
        nativeMethods["java/lang/Math.abs:(F)F"] = { frame ->
            frame.push(kotlin.math.abs(frame.pop() as Float))
        }
        // Math.sqrt(double)
        nativeMethods["java/lang/Math.sqrt:(D)D"] = { frame ->
            frame.push(kotlin.math.sqrt(frame.pop() as Double))
        }
        // Math.sin(double)
        nativeMethods["java/lang/Math.sin:(D)D"] = { frame ->
            frame.push(kotlin.math.sin(frame.pop() as Double))
        }
        // Math.cos(double)
        nativeMethods["java/lang/Math.cos:(D)D"] = { frame ->
            frame.push(kotlin.math.cos(frame.pop() as Double))
        }
    }

    private fun registerJavaLangRandom() {
        val random = kotlin.random.Random

        // Random.<init>(J) - seed ignored, use Kotlin Random
        nativeMethods["java/util/Random.<init>:(J)V"] = { frame ->
            frame.popLong() // seed - pop and ignore
            val obj = frame.pop() as? HeapObject
            // Store internal state marker
            obj?.instanceFields?.set("_rng", true)
        }

        // Random.<init>() - no-arg
        nativeMethods["java/util/Random.<init>:()V"] = { frame ->
            val obj = frame.pop() as? HeapObject
            obj?.instanceFields?.set("_rng", true)
        }

        // Random.nextInt(int bound)
        nativeMethods["java/util/Random.nextInt:(I)I"] = { frame ->
            val bound = frame.popInt()
            frame.pop() // pop 'this'
            frame.push(if (bound > 0) random.nextInt(bound) else 0)
        }

        // Random.nextInt()
        nativeMethods["java/util/Random.nextInt:()I"] = { frame ->
            frame.pop() // pop 'this'
            frame.push(random.nextInt())
        }

        // Random.nextLong()
        nativeMethods["java/util/Random.nextLong:()J"] = { frame ->
            frame.pop() // pop 'this'
            frame.push(random.nextLong())
        }
    }

    private fun registerJavaxMicroeditionLcduiCanvas() {
        nativeMethods["javax/microedition/lcdui/Canvas.repaint:()V"] = { frame ->
            println("[NativeBridge] Calling Canvas.repaint()")
            emulator.core.api.javax.microedition.lcdui.Canvas.repaint(frame)
        }
        nativeMethods["javax/microedition/lcdui/Canvas.serviceRepaints:()V"] = { frame ->
            frame.pop() // this
        }
        nativeMethods["javax/microedition/lcdui/Canvas.setFullScreenMode:(Z)V"] = { frame ->
            println("[NativeBridge] Calling Canvas.setFullScreenMode()")
            emulator.core.api.javax.microedition.lcdui.Canvas.setFullScreenMode(frame)
            println("[NativeBridge] Returned from Canvas.setFullScreenMode()")
        }
        nativeMethods["javax/microedition/lcdui/Canvas.getWidth:()I"] = { frame ->
            frame.pop() // this
            frame.push(240)
        }
        nativeMethods["javax/microedition/lcdui/Canvas.getHeight:()I"] = { frame ->
            frame.pop() // this
            frame.push(320)
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
        nativeMethods["javax/microedition/lcdui/Graphics.translate:(II)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.translate(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.getClipX:()I"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.getClipX(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.getClipY:()I"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.getClipY(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.getClipWidth:()I"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.getClipWidth(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.getClipHeight:()I"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.getClipHeight(frame)
        }
        nativeMethods["javax/microedition/lcdui/Graphics.getFont:()Ljavax/microedition/lcdui/Font;"] = { frame ->
            frame.pop() // this
            frame.push(HeapObject("javax/microedition/lcdui/Font"))
        }

        // javax/microedition/lcdui/Graphics.drawRegion:(Ljavax/microedition/lcdui/Image;IIIIIIII)V
        nativeMethods["javax/microedition/lcdui/Graphics.drawRegion:(Ljavax/microedition/lcdui/Image;IIIIIIII)V"] = { frame ->
            emulator.core.api.javax.microedition.lcdui.Graphics.drawRegion(frame)
        }
    }

    private fun registerJavaxMicroeditionLcduiFont() {
        // static getFont(int face, int style, int size)
        nativeMethods["javax/microedition/lcdui/Font.getFont:(III)Ljavax/microedition/lcdui/Font;"] = { frame ->
            val size = frame.popInt()
            val style = frame.popInt()
            val face = frame.popInt()
            val fontObj = HeapObject("javax/microedition/lcdui/Font")
            fontObj.instanceFields["style:I"] = style
            fontObj.instanceFields["face:I"] = face
            fontObj.instanceFields["size:I"] = size
            frame.push(fontObj)
        }
        nativeMethods["javax/microedition/lcdui/Font.getDefaultFont:()Ljavax/microedition/lcdui/Font;"] = { frame ->
            frame.push(HeapObject("javax/microedition/lcdui/Font"))
        }
        nativeMethods["javax/microedition/lcdui/Font.getHeight:()I"] = { frame ->
            frame.pop() // this
            frame.push(16) // Stub height
        }
        nativeMethods["javax/microedition/lcdui/Font.stringWidth:(Ljava/lang/String;)I"] = { frame ->
            val str = frame.pop() as? String ?: ""
            frame.pop() // this
            frame.push(str.length * 8)
        }
    }

    private fun registerJavaLangClass() {
        // java/lang/Object.getClass:()Ljava/lang/Class;
        nativeMethods["java/lang/Object.getClass:()Ljava/lang/Class;"] = { frame ->
            val obj = frame.pop() as? HeapObject
            if (obj != null) {
                val classObj = HeapObject("java/lang/Class")
                classObj.instanceFields["_targetClassName"] = obj.className
                frame.push(classObj)
            } else {
                frame.push(null)
            }
        }

        nativeMethods["java/lang/Object.wait:(J)V"] = { frame ->
            val timeout = frame.popLong()
            frame.pop() // this
            if (timeout > 0) {
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.delay(timeout)
                }
            }
        }
        nativeMethods["java/lang/Object.wait:()V"] = { frame ->
            frame.pop() // this
            // Infinite wait not really supported well here, but let's at least not spin.
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.delay(10)
            }
        }
        nativeMethods["java/lang/Object.notify:()V"] = { frame -> frame.pop() }
        nativeMethods["java/lang/Object.notifyAll:()V"] = { frame -> frame.pop() }
        
        nativeMethods["java/lang/Object.<init>:()V"] = { frame ->
            frame.pop() // this
        }

        // java/lang/Class.getResourceAsStream:(Ljava/lang/String;)Ljava/io/InputStream;
        nativeMethods["java/lang/Class.getResourceAsStream:(Ljava/lang/String;)Ljava/io/InputStream;"] = { frame ->
            val resourceName = frame.pop() as? String ?: ""
            val classObj = frame.pop() as? HeapObject
            
            val jarPath = frame.interpreter.currentJarPath
            if (jarPath.isNotEmpty()) {
                val data = emulator.core.JarLoader().loadResource(jarPath, resourceName)
                if (data != null) {
                    val isObj = HeapObject("java/io/InputStream")
                    isObj.instanceFields["_data:[B"] = data
                    isObj.instanceFields["_pos:I"] = 0
                    frame.push(isObj)
                    println("[NativeBridge] getResourceAsStream(\"$resourceName\") SUCCESS (${data.size} bytes)")
                } else {
                    println("[NativeBridge] getResourceAsStream(\"$resourceName\") FAILED (not found in $jarPath)")
                    frame.push(null)
                }
            } else {
                frame.push(null)
            }
        }
    }

    private fun registerJavaIoInputStream() {
        // java/io/InputStream.read:()I
        nativeMethods["java/io/InputStream.read:()I"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val data = thisObj?.instanceFields?.get("_data:[B") as? ByteArray ?: ByteArray(0)
            var pos = thisObj?.instanceFields?.get("_pos:I") as? Int ?: 0
            
            if (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                thisObj?.instanceFields?.set("_pos:I", pos + 1)
                frame.push(b)
            } else {
                frame.push(-1)
            }
        }

        // java/io/InputStream.read:([B)I
        nativeMethods["java/io/InputStream.read:([B)I"] = { frame ->
            val b = frame.pop() as? ByteArray ?: ByteArray(0)
            val thisObj = frame.pop() as? HeapObject
            
            val data = thisObj?.instanceFields?.get("_data:[B") as? ByteArray ?: ByteArray(0)
            var pos = thisObj?.instanceFields?.get("_pos:I") as? Int ?: 0
            
            if (pos >= data.size) {
                frame.push(-1)
            } else {
                val toRead = minOf(b.size, data.size - pos)
                data.copyInto(b, 0, pos, pos + toRead)
                thisObj?.instanceFields?.set("_pos:I", pos + toRead)
                frame.push(toRead)
            }
        }

        // java/io/InputStream.close:()V
        nativeMethods["java/io/InputStream.close:()V"] = { frame ->
            frame.pop()
        }

        // java/io/InputStream.available:()I
        nativeMethods["java/io/InputStream.available:()I"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val data = thisObj?.instanceFields?.get("_data:[B") as? ByteArray ?: ByteArray(0)
            val pos = thisObj?.instanceFields?.get("_pos:I") as? Int ?: 0
            frame.push(maxOf(0, data.size - pos))
        }

        // java/io/InputStream.skip:(J)J
        nativeMethods["java/io/InputStream.skip:(J)J"] = { frame ->
            val n = frame.popLong()
            val thisObj = frame.pop() as? HeapObject
            val data = thisObj?.instanceFields?.get("_data:[B") as? ByteArray ?: ByteArray(0)
            val pos = thisObj?.instanceFields?.get("_pos:I") as? Int ?: 0
            
            val toSkip = minOf(n, (data.size - pos).toLong()).toInt()
            thisObj?.instanceFields?.set("_pos:I", pos + toSkip)
            frame.push(toSkip.toLong())
        }
    }

    private fun registerJavaxMicroeditionLcduiImage() {
        // javax/microedition/lcdui/Image.createImage:(Ljava/lang/String;)Ljavax/microedition/lcdui/Image;
        nativeMethods["javax/microedition/lcdui/Image.createImage:(Ljava/lang/String;)Ljavax/microedition/lcdui/Image;"] = { frame ->
            val name = frame.pop() as? String ?: ""
            val jarPath = frame.interpreter.currentJarPath
            
            if (jarPath.isNotEmpty()) {
                val data = emulator.core.JarLoader().loadResource(jarPath, name)
                if (data != null) {
                    val info = emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.decodeImage(data)
                    if (info != null) {
                        val imgObj = HeapObject("javax/microedition/lcdui/Image")
                        imgObj.instanceFields["width:I"] = info.width
                        imgObj.instanceFields["height:I"] = info.height
                        imgObj.instanceFields["rgb:[I"] = info.pixels
                        frame.push(imgObj)
                        println("[NativeBridge] Image.createImage(\"$name\") SUCCESS (${info.width}x${info.height})")
                    } else {
                        throw RuntimeException("Failed to decode image: $name")
                    }
                } else {
                    throw RuntimeException("Resource not found: $name")
                }
            } else {
                frame.push(null)
            }
        }

        // javax/microedition/lcdui/Image.createImage:([BII)Ljavax/microedition/lcdui/Image;
        nativeMethods["javax/microedition/lcdui/Image.createImage:([BII)Ljavax/microedition/lcdui/Image;"] = { frame ->
            val len = frame.popInt()
            val off = frame.popInt()
            val data = frame.pop() as? ByteArray ?: ByteArray(0)
            
            val subData = if (off == 0 && len == data.size) data else data.copyOfRange(off, off + len)
            val info = emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.decodeImage(subData)
            if (info != null) {
                val imgObj = HeapObject("javax/microedition/lcdui/Image")
                imgObj.instanceFields["width:I"] = info.width
                imgObj.instanceFields["height:I"] = info.height
                imgObj.instanceFields["rgb:[I"] = info.pixels
                frame.push(imgObj)
                println("[NativeBridge] Image.createImage([B]) SUCCESS (${info.width}x${info.height})")
            } else {
                println("[NativeBridge] Image.createImage([B]) FAILED to decode")
                frame.push(null)
            }
        }


        // javax/microedition/lcdui/Image.getHeight:()I
        nativeMethods["javax/microedition/lcdui/Image.getHeight:()I"] = { frame ->
            val imgObj = frame.pop() as? HeapObject
            frame.push(imgObj?.instanceFields?.get("height:I") as? Int ?: 0)
        }

        // javax/microedition/lcdui/Image.createImage:(Ljava/io/InputStream;)Ljavax/microedition/lcdui/Image;
        nativeMethods["javax/microedition/lcdui/Image.createImage:(Ljava/io/InputStream;)Ljavax/microedition/lcdui/Image;"] = { frame ->
            val isObj = frame.pop() as? HeapObject
            val data = isObj?.instanceFields?.get("_data:[B") as? ByteArray
            if (data != null) {
                val info = emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.decodeImage(data)
                if (info != null) {
                    val imgObj = HeapObject("javax/microedition/lcdui/Image")
                    imgObj.instanceFields["width:I"] = info.width
                    imgObj.instanceFields["height:I"] = info.height
                    imgObj.instanceFields["rgb:[I"] = info.pixels
                    frame.push(imgObj)
                    println("[NativeBridge] Image.createImage(InputStream) SUCCESS (${info.width}x${info.height})")
                } else {
                    println("[NativeBridge] Image.createImage(InputStream) FAILED to decode")
                    frame.push(null)
                }
            } else {
                println("[NativeBridge] Image.createImage(InputStream) FAILED: data is null")
                frame.push(null)
            }
        }

        // javax/microedition/lcdui/Image.getWidth:()I
        nativeMethods["javax/microedition/lcdui/Image.getWidth:()I"] = { frame ->
            val imgObj = frame.pop() as? HeapObject
            frame.push(imgObj?.instanceFields?.get("width:I") as? Int ?: 0)
        }

        // javax/microedition/lcdui/Image.getGraphics:()Ljavax/microedition/lcdui/Graphics;
        nativeMethods["javax/microedition/lcdui/Image.getGraphics:()Ljavax/microedition/lcdui/Graphics;"] = { frame ->
            val imgObj = frame.pop() as? HeapObject
            val gObj = HeapObject("javax/microedition/lcdui/Graphics")
            gObj.instanceFields["_targetImage"] = imgObj
            frame.push(gObj)
        }

        // javax/microedition/lcdui/Image.createImage:(II)Ljavax/microedition/lcdui/Image;
        nativeMethods["javax/microedition/lcdui/Image.createImage:(II)Ljavax/microedition/lcdui/Image;"] = { frame ->
            val h = frame.popInt()
            val w = frame.popInt()
            val imgObj = HeapObject("javax/microedition/lcdui/Image")
            imgObj.instanceFields["width:I"] = w
            imgObj.instanceFields["height:I"] = h
            imgObj.instanceFields["rgb:[I"] = IntArray(w * h)
            frame.push(imgObj)
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

    private fun registerJavaxMicroeditionRmsRecordStore() {
        // public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
        nativeMethods["javax/microedition/rms/RecordStore.openRecordStore:(Ljava/lang/String;Z)Ljavax/microedition/rms/RecordStore;"] = { frame ->
            val createIfNecessary = frame.popInt() != 0
            val storeName = frame.pop() as? String ?: ""
            
            try {
                val store = emulator.core.api.javax.microedition.rms.RecordStore.openRecordStore(storeName, createIfNecessary)
                
                // Return a HeapObject ref for the RecordStore instance
                val storeObj = emulator.core.memory.HeapObject("javax/microedition/rms/RecordStore")
                storeObj.instanceFields["_internalRef"] = store
                frame.push(storeObj)
            } catch (e: Exception) {
                e.printStackTrace()
                frame.push(null) // Or throw RecordStoreNotFoundException
            }
        }

        // public int addRecord(byte[] data, int offset, int numBytes)
        nativeMethods["javax/microedition/rms/RecordStore.addRecord:([BII)I"] = { frame ->
            val numBytes = frame.popInt()
            val offset = frame.popInt()
            val data = frame.pop() as ByteArray
            val thisObj = frame.pop() as emulator.core.memory.HeapObject
            
            val store = thisObj.instanceFields["_internalRef"] as emulator.core.api.javax.microedition.rms.RecordStore
            val newId = store.addRecord(data, offset, numBytes)
            frame.push(newId)
        }

        // public byte[] getRecord(int recordId)
        nativeMethods["javax/microedition/rms/RecordStore.getRecord:(I)[B"] = { frame ->
            val recordId = frame.popInt()
            val thisObj = frame.pop() as emulator.core.memory.HeapObject
            
            val store = thisObj.instanceFields["_internalRef"] as emulator.core.api.javax.microedition.rms.RecordStore
            val data = store.getRecord(recordId)
            frame.push(data) // Push ByteArray (represented as Kotlin ByteArray)
        }

        // public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
        nativeMethods["javax/microedition/rms/RecordStore.setRecord:(I[BII)V"] = { frame ->
            val numBytes = frame.popInt()
            val offset = frame.popInt()
            val newData = frame.pop() as ByteArray
            val recordId = frame.popInt()
            val thisObj = frame.pop() as emulator.core.memory.HeapObject
            
            val store = thisObj.instanceFields["_internalRef"] as emulator.core.api.javax.microedition.rms.RecordStore
            store.setRecord(recordId, newData, offset, numBytes)
        }

        // public void closeRecordStore()
        nativeMethods["javax/microedition/rms/RecordStore.closeRecordStore:()V"] = { frame ->
            val thisObj = frame.pop() as emulator.core.memory.HeapObject
            
            val store = thisObj.instanceFields["_internalRef"] as emulator.core.api.javax.microedition.rms.RecordStore
            store.closeRecordStore()
        }

        // public int getNumRecords()
        nativeMethods["javax/microedition/rms/RecordStore.getNumRecords:()I"] = { frame ->
            val thisObj = frame.pop() as emulator.core.memory.HeapObject
            
            val store = thisObj.instanceFields["_internalRef"] as emulator.core.api.javax.microedition.rms.RecordStore
            frame.push(store.getNumRecords())
        }
        
        // public static void deleteRecordStore(String recordStoreName)
        nativeMethods["javax/microedition/rms/RecordStore.deleteRecordStore:(Ljava/lang/String;)V"] = { frame ->
            val storeName = frame.pop() as? String ?: ""
            emulator.core.api.javax.microedition.rms.RecordStore.deleteRecordStore(storeName)
        }
    }

    private fun registerJavaLangString() {
        // java/lang/String.equals:(Ljava/lang/Object;)Z
        nativeMethods["java/lang/String.equals:(Ljava/lang/Object;)Z"] = { frame ->
            val other = frame.pop()
            val thisStr = frame.pop() as? String ?: ""
            
            if (other is String) {
                frame.push(if (thisStr == other) 1 else 0)
            } else if (other is HeapObject && other.className == "java/lang/String") {
                 val otherStr = other.instanceFields["value"] as? String ?: ""
                 frame.push(if (thisStr == otherStr) 1 else 0)
            } else {
                frame.push(0)
            }
        }
        
        // java/lang/String.length:()I
        nativeMethods["java/lang/String.length:()I"] = { frame ->
            val thisStr = getString(frame.pop())
            frame.push(thisStr.length)
        }

        // java/lang/String.trim:()Ljava/lang/String;
        nativeMethods["java/lang/String.trim:()Ljava/lang/String;"] = { frame ->
            val thisStr = getString(frame.pop())
            val result = thisStr.trim()
            println("[NativeBridge] String.trim(\"$thisStr\") -> \"$result\"")
            frame.push(result)
        }

        // java/lang/String.charAt:(I)C
        nativeMethods["java/lang/String.charAt:(I)C"] = { frame ->
            val index = frame.popInt()
            val thisStr = getString(frame.pop())
            if (index >= 0 && index < thisStr.length) {
                frame.push(thisStr[index].code)
            } else {
                frame.push(0)
            }
        }

        // java/lang/String.substring:(I)Ljava/lang/String;
        nativeMethods["java/lang/String.substring:(I)Ljava/lang/String;"] = { frame ->
            val beginIndex = frame.popInt()
            val thisStr = getString(frame.pop())
            if (beginIndex >= 0 && beginIndex <= thisStr.length) {
                frame.push(thisStr.substring(beginIndex))
            } else {
                frame.push("")
            }
        }

        // java/lang/String.substring:(II)Ljava/lang/String;
        nativeMethods["java/lang/String.substring:(II)Ljava/lang/String;"] = { frame ->
            val endIndex = frame.popInt()
            val beginIndex = frame.popInt()
            val thisStr = getString(frame.pop())
            if (beginIndex >= 0 && beginIndex <= thisStr.length && endIndex >= beginIndex && endIndex <= thisStr.length) {
                frame.push(thisStr.substring(beginIndex, endIndex))
            } else {
                frame.push("")
            }
        }

        // java/lang/String.indexOf:(I)I
        nativeMethods["java/lang/String.indexOf:(I)I"] = { frame ->
            val ch = frame.popInt()
            val thisStr = getString(frame.pop())
            frame.push(thisStr.indexOf(ch.toChar()))
        }

        // java/lang/String.indexOf:(Ljava/lang/String;)I
        nativeMethods["java/lang/String.indexOf:(Ljava/lang/String;)I"] = { frame ->
            val str = getString(frame.pop())
            val thisStr = getString(frame.pop())
            frame.push(thisStr.indexOf(str))
        }

        // java/lang/String.startsWith:(Ljava/lang/String;)Z
        nativeMethods["java/lang/String.startsWith:(Ljava/lang/String;)Z"] = { frame ->
            val prefix = getString(frame.pop())
            val thisStr = getString(frame.pop())
            val result = thisStr.startsWith(prefix)
            println("[NativeBridge] String.startsWith(\"$thisStr\", \"$prefix\") -> $result")
            frame.push(if (result) 1 else 0)
        }

        // java/lang/String.endsWith:(Ljava/lang/String;)Z
        nativeMethods["java/lang/String.endsWith:(Ljava/lang/String;)Z"] = { frame ->
            val suffix = getString(frame.pop())
            val thisStr = getString(frame.pop())
            frame.push(if (thisStr.endsWith(suffix)) 1 else 0)
        }

        // java/lang/String.replace:(CC)Ljava/lang/String;
        nativeMethods["java/lang/String.replace:(CC)Ljava/lang/String;"] = { frame ->
            val newChar = frame.popInt().toChar()
            val oldChar = frame.popInt().toChar()
            val thisStr = getString(frame.pop())
            frame.push(thisStr.replace(oldChar, newChar))
        }

        // java/lang/String.toLowerCase:()Ljava/lang/String;
        nativeMethods["java/lang/String.toLowerCase:()Ljava/lang/String;"] = { frame ->
            val thisStr = getString(frame.pop())
            frame.push(thisStr.lowercase())
        }

        // java/lang/String.toUpperCase:()Ljava/lang/String;
        nativeMethods["java/lang/String.toUpperCase:()Ljava/lang/String;"] = { frame ->
            val thisStr = getString(frame.pop())
            frame.push(thisStr.uppercase())
        }

        // java/lang/String.valueOf:(I)Ljava/lang/String;
        nativeMethods["java/lang/String.valueOf:(I)Ljava/lang/String;"] = { frame ->
            val i = frame.popInt()
            frame.push(i.toString())
        }

        // java/lang/String.valueOf:(Z)Ljava/lang/String;
        nativeMethods["java/lang/String.valueOf:(Z)Ljava/lang/String;"] = { frame ->
            val b = frame.popInt() != 0
            frame.push(b.toString())
        }
    }

    private fun registerJavaIoDataInputStream() {
        // java/io/DataInputStream.<init>:(Ljava/io/InputStream;)V
        nativeMethods["java/io/DataInputStream.<init>:(Ljava/io/InputStream;)V"] = { frame ->
            val inputStream = frame.pop() as? HeapObject
            val thisObj = frame.pop() as? HeapObject
            thisObj?.instanceFields?.set("_stream", inputStream)
        }

        // java/io/DataInputStream.readUnsignedShort:()I
        nativeMethods["java/io/DataInputStream.readUnsignedShort:()I"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                
                if (pos + 1 < data.size) {
                    val b1 = data[pos].toInt() and 0xFF
                    val b2 = data[pos + 1].toInt() and 0xFF
                    inputStream.instanceFields["_pos:I"] = pos + 2
                    frame.push((b1 shl 8) or b2)
                } else {
                    frame.push(0)
                }
            } else {
                frame.push(0)
            }
        }

        // java/io/DataInputStream.readShort:()S
        nativeMethods["java/io/DataInputStream.readShort:()S"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                if (pos + 1 < data.size) {
                    val b1 = data[pos].toInt()
                    val b2 = data[pos + 1].toInt() and 0xFF
                    inputStream.instanceFields["_pos:I"] = pos + 2
                    frame.push(((b1 shl 8) or b2).toShort().toInt())
                } else {
                    frame.push(0)
                }
            } else {
                frame.push(0)
            }
        }

        // java/io/DataInputStream.readInt:()I
        nativeMethods["java/io/DataInputStream.readInt:()I"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                if (pos + 3 < data.size) {
                    val v = ((data[pos].toInt() and 0xFF) shl 24) or
                            ((data[pos+1].toInt() and 0xFF) shl 16) or
                            ((data[pos+2].toInt() and 0xFF) shl 8) or
                            (data[pos+3].toInt() and 0xFF)
                    inputStream.instanceFields["_pos:I"] = pos + 4
                    frame.push(v)
                } else {
                    frame.push(0)
                }
            } else {
                frame.push(0)
            }
        }

        // java/io/DataInputStream.readByte:()B
        nativeMethods["java/io/DataInputStream.readByte:()B"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                if (pos < data.size) {
                    val b = data[pos].toInt()
                    inputStream.instanceFields["_pos:I"] = pos + 1
                    frame.push(b)
                } else {
                    frame.push(0)
                }
            } else {
                frame.push(0)
            }
        }

        // java/io/DataInputStream.readUnsignedByte:()I
        nativeMethods["java/io/DataInputStream.readUnsignedByte:()I"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                if (pos < data.size) {
                    val b = data[pos].toInt() and 0xFF
                    inputStream.instanceFields["_pos:I"] = pos + 1
                    frame.push(b)
                } else {
                    frame.push(0)
                }
            } else {
                frame.push(0)
            }
        }

        // java/io/DataInputStream.readUTF:()Ljava/lang/String;
        nativeMethods["java/io/DataInputStream.readUTF:()Ljava/lang/String;"] = { frame ->
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                
                if (pos + 1 < data.size) {
                    val len = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
                    pos += 2
                    if (pos + len <= data.size) {
                        val utf8 = data.decodeToString(pos, pos + len)
                        inputStream.instanceFields["_pos:I"] = pos + len
                        frame.push(utf8)
                    } else {
                        frame.push("")
                    }
                } else {
                    frame.push("")
                }
            } else {
                frame.push("")
            }
        }

        // java/io/DataInputStream.skipBytes:(I)I
        nativeMethods["java/io/DataInputStream.skipBytes:(I)I"] = { frame ->
            val n = frame.popInt()
            val thisObj = frame.pop() as? HeapObject
            val inputStream = thisObj?.instanceFields?.get("_stream") as? HeapObject
            
            if (inputStream != null) {
                val data = inputStream.instanceFields["_data:[B"] as? ByteArray ?: ByteArray(0)
                var pos = inputStream.instanceFields["_pos:I"] as? Int ?: 0
                val skipped = minOf(n, data.size - pos)
                inputStream.instanceFields["_pos:I"] = pos + skipped
                frame.push(skipped)
            } else {
                frame.push(0)
            }
        }

        // java/io/DataInputStream.close:()V
        nativeMethods["java/io/DataInputStream.close:()V"] = { frame ->
            frame.pop()
        }

        // java/io/DataInputStream.markSupported:()Z
        nativeMethods["java/io/DataInputStream.markSupported:()Z"] = { frame ->
            frame.pop()
            frame.push(0) // false
        }
        
        // java/io/DataInputStream.mark:(I)V
        nativeMethods["java/io/DataInputStream.mark:(I)V"] = { frame ->
            frame.popInt()
            frame.pop()
        }
    }

    private fun registerJavaxMicroeditionMedia() {
        // javax/microedition/media/Manager.createPlayer:(Ljava/io/InputStream;Ljava/lang/String;)Ljavax/microedition/media/Player;
        nativeMethods["javax/microedition/media/Manager.createPlayer:(Ljava/io/InputStream;Ljava/lang/String;)Ljavax/microedition/media/Player;"] = { frame ->
            val type = frame.pop() as? String
            val isObj = frame.pop() as? HeapObject
            
            // Return a stub Player object
            val playerObj = HeapObject("javax/microedition/media/Player")
            println("[NativeBridge] Manager.createPlayer(type=$type) -> Stubbed")
            frame.push(playerObj)
        }

        // javax/microedition/media/Player.realize:()V
        nativeMethods["javax/microedition/media/Player.realize:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/media/Player.prefetch:()V
        nativeMethods["javax/microedition/media/Player.prefetch:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/media/Player.start:()V
        nativeMethods["javax/microedition/media/Player.start:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/media/Player.stop:()V
        nativeMethods["javax/microedition/media/Player.stop:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/media/Player.close:()V
        nativeMethods["javax/microedition/media/Player.close:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/media/Player.setLoopCount:(I)V
        nativeMethods["javax/microedition/media/Player.setLoopCount:(I)V"] = { frame ->
            frame.popInt() // count
            frame.pop()    // this
        }

        // javax/microedition/media/Player.getControl:(Ljava/lang/String;)Ljavax/microedition/media/Control;
        nativeMethods["javax/microedition/media/Player.getControl:(Ljava/lang/String;)Ljavax/microedition/media/Control;"] = { frame ->
            frame.pop() // controlName
            frame.pop() // this
            frame.push(null) // Controls not supported (VolumeControl etc)
        }
    }

    private fun registerJavaxMicroeditionMidlet() {
        // javax/microedition/midlet/MIDlet.<init>:()V
        nativeMethods["javax/microedition/midlet/MIDlet.<init>:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/midlet/MIDlet.getAppProperty:(Ljava/lang/String;)Ljava/lang/String;
        nativeMethods["javax/microedition/midlet/MIDlet.getAppProperty:(Ljava/lang/String;)Ljava/lang/String;"] = { frame ->
            val key = frame.pop() as? String ?: ""
            frame.pop() // this
            println("[NativeBridge] MIDlet.getAppProperty(\"$key\") called")
            val valStr = when(key) {
                "MIDlet-Name" -> "Bounce Tales"
                "MIDlet-Vendor" -> "Nokia"
                "MIDlet-Version" -> "2.0.17"
                "MicroEdition-Configuration" -> "CLDC-1.1"
                "MicroEdition-Profile" -> "MIDP-2.0"
                else -> null
            }
            frame.push(valStr)
        }
    }

    private fun registerJavaxMicroeditionLcduiDisplayable() {
        // javax/microedition/lcdui/Displayable.<init>:()V
        nativeMethods["javax/microedition/lcdui/Displayable.<init>:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/lcdui/Displayable.getWidth:()I
        nativeMethods["javax/microedition/lcdui/Displayable.getWidth:()I"] = { frame ->
            frame.pop() // this
            frame.push(240) // Default width
        }

        // javax/microedition/lcdui/Displayable.getHeight:()I
        nativeMethods["javax/microedition/lcdui/Displayable.getHeight:()I"] = { frame ->
            frame.pop() // this
            frame.push(320) // Default height
        }

        // javax/microedition/lcdui/Canvas.<init>:()V
        nativeMethods["javax/microedition/lcdui/Canvas.<init>:()V"] = { frame ->
            frame.pop() // this
        }

        // javax/microedition/lcdui/Canvas.getGraphics:()Ljavax/microedition/lcdui/Graphics;
        nativeMethods["javax/microedition/lcdui/Canvas.getGraphics:()Ljavax/microedition/lcdui/Graphics;"] = { frame ->
             frame.pop() // this
             frame.push(HeapObject("javax/microedition/lcdui/Graphics"))
        }

        // javax/microedition/lcdui/game/GameCanvas.<init>:(Z)V
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.<init>:(Z)V"] = { frame ->
            val suppress = frame.popInt()
            val thisObj = frame.pop()
        }
    }

    private fun registerJavaxMicroeditionLcduiGameCanvas() {
        // javax/microedition/lcdui/game/GameCanvas.flushGraphics:()V
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.flushGraphics:()V"] = { frame ->
            frame.pop() // this
            emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.presentScreen()
        }

        // javax/microedition/lcdui/game/GameCanvas.flushGraphics:(IIII)V
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.flushGraphics:(IIII)V"] = { frame ->
            frame.popInt() // h
            frame.popInt() // w
            frame.popInt() // y
            frame.popInt() // x
            frame.pop() // this
            emulator.core.api.javax.microedition.lcdui.NativeGraphicsBridge.presentScreen()
        }
        
        // javax/microedition/lcdui/game/GameCanvas.getKeyStates:()I
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.getKeyStates:()I"] = { frame ->
            frame.pop() // this
            frame.push(0) // No keys pressed for now
        }
        
        // javax/microedition/lcdui/game/GameCanvas.getGraphics:()Ljavax/microedition/lcdui/Graphics;
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.getGraphics:()Ljavax/microedition/lcdui/Graphics;"] = { frame ->
            frame.pop() // this
            val gObj = HeapObject("javax/microedition/lcdui/Graphics")
            frame.push(gObj)
        }

        // javax/microedition/lcdui/game/GameCanvas.getWidth:()I
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.getWidth:()I"] = { frame ->
            frame.pop() // this
            frame.push(240)
        }

        // javax/microedition/lcdui/game/GameCanvas.getHeight:()I
        nativeMethods["javax/microedition/lcdui/game/GameCanvas.getHeight:()I"] = { frame ->
            frame.pop() // this
            frame.push(320)
        }
    }

    private fun registerComNokiaMidUi() {
        // com/nokia/mid/ui/DirectUtils.getDirectGraphics:(Ljavax/microedition/lcdui/Graphics;)Lcom/nokia/mid/ui/DirectGraphics;
        nativeMethods["com/nokia/mid/ui/DirectUtils.getDirectGraphics:(Ljavax/microedition/lcdui/Graphics;)Lcom/nokia/mid/ui/DirectGraphics;"] = { frame ->
            val graphics = frame.pop() as? HeapObject
            // DirectGraphics is often an interface that Graphics implements in Nokia phones.
            // We just return the Graphics object itself as a DirectGraphics shell.
            frame.push(graphics)
        }

        // Stub DirectGraphics methods to prevent crash
        nativeMethods["com/nokia/mid/ui/DirectGraphics.setARGBColor:(I)V"] = { frame ->
            val argb = frame.popInt()
            val thisObj = frame.pop()
        }
        
        nativeMethods["com/nokia/mid/ui/DirectGraphics.drawPixels:([SZI[IIIIIII)V"] = { frame ->
            // High-complexity Nokia pixel drawing (ignore for stub)
            popArguments("([SZI[IIIIIII)V", frame)
            frame.pop() // this
        }
    }
    private fun registerJavaUtil() {
        // java/util/Vector.<init>:()V
        nativeMethods["java/util/Vector.<init>:()V"] = { frame ->
            val obj = frame.pop() as? HeapObject
            obj?.instanceFields?.set("_data", mutableListOf<Any?>())
        }
        // java/util/Vector.<init>:(I)V
        nativeMethods["java/util/Vector.<init>:(I)V"] = { frame ->
            frame.popInt() // capacity
            val obj = frame.pop() as? HeapObject
            obj?.instanceFields?.set("_data", mutableListOf<Any?>())
        }
        // java/util/Vector.addElement:(Ljava/lang/Object;)V
        nativeMethods["java/util/Vector.addElement:(Ljava/lang/Object;)V"] = { frame ->
            val element = frame.pop()
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? MutableList<Any?>
            list?.add(element)
        }
        // java/util/Vector.size:()I
        nativeMethods["java/util/Vector.size:()I"] = { frame ->
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? List<Any?>
            frame.push(list?.size ?: 0)
        }
        // java/util/Vector.elementAt:(I)Ljava/lang/Object;
        nativeMethods["java/util/Vector.elementAt:(I)Ljava/lang/Object;"] = { frame ->
            val index = frame.popInt()
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? List<Any?>
            if (list != null && index >= 0 && index < list.size) {
                frame.push(list[index])
            } else {
                frame.push(null)
            }
        }
        // java/util/Vector.removeAllElements:()V
        nativeMethods["java/util/Vector.removeAllElements:()V"] = { frame ->
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? MutableList<Any?>
            list?.clear()
        }

        // java/util/Hashtable.<init>:()V
        nativeMethods["java/util/Hashtable.<init>:()V"] = { frame ->
            val obj = frame.pop() as? HeapObject
            obj?.instanceFields?.set("_data", mutableMapOf<Any, Any?>())
        }
        // java/util/Hashtable.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
        nativeMethods["java/util/Hashtable.put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"] = { frame ->
            val value = frame.pop()
            val key = frame.pop()
            val obj = frame.pop() as? HeapObject
            val map = obj?.instanceFields?.get("_data") as? MutableMap<Any, Any?>
            if (map != null && key != null) {
                val old = map.put(key, value)
                frame.push(old)
            } else {
                frame.push(null)
            }
        }
        // java/util/Hashtable.get:(Ljava/lang/Object;)Ljava/lang/Object;
        nativeMethods["java/util/Hashtable.get:(Ljava/lang/Object;)Ljava/lang/Object;"] = { frame ->
            val key = frame.pop()
            val obj = frame.pop() as? HeapObject
            val map = obj?.instanceFields?.get("_data") as? Map<Any, Any?>
            frame.push(map?.get(key ?: ""))
        }

        // java/util/Vector.contains:(Ljava/lang/Object;)Z
        nativeMethods["java/util/Vector.contains:(Ljava/lang/Object;)Z"] = { frame ->
            val element = frame.pop()
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? List<Any?>
            frame.push(if (list?.contains(element) == true) 1 else 0)
        }
        // java/util/Vector.indexOf:(Ljava/lang/Object;)I
        nativeMethods["java/util/Vector.indexOf:(Ljava/lang/Object;)I"] = { frame ->
            val element = frame.pop()
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? List<Any?>
            frame.push(list?.indexOf(element) ?: -1)
        }
        // java/util/Vector.setElementAt:(Ljava/lang/Object;I)V
        nativeMethods["java/util/Vector.setElementAt:(Ljava/lang/Object;I)V"] = { frame ->
            val index = frame.popInt()
            val element = frame.pop()
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? MutableList<Any?>
            if (list != null && index >= 0 && index < list.size) {
                list[index] = element
            }
        }
        // java/util/Vector.removeElement:(Ljava/lang/Object;)Z
        nativeMethods["java/util/Vector.removeElement:(Ljava/lang/Object;)Z"] = { frame ->
            val element = frame.pop()
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? MutableList<Any?>
            frame.push(if (list?.remove(element) == true) 1 else 0)
        }
        // java/util/Vector.isEmpty:()Z
        nativeMethods["java/util/Vector.isEmpty:()Z"] = { frame ->
            val obj = frame.pop() as? HeapObject
            val list = obj?.instanceFields?.get("_data") as? List<Any?>
            frame.push(if (list?.isEmpty() == true) 1 else 0)
        }
        // java/util/Hashtable.containsKey:(Ljava/lang/Object;)Z
        nativeMethods["java/util/Hashtable.containsKey:(Ljava/lang/Object;)Z"] = { frame ->
            val key = frame.pop()
            val obj = frame.pop() as? HeapObject
            val map = obj?.instanceFields?.get("_data") as? Map<Any, Any?>
            frame.push(if (map?.containsKey(key ?: "") == true) 1 else 0)
        }
    }

    private fun registerJavaLangInteger() {
        nativeMethods["java/lang/Integer.<init>:(I)V"] = { frame ->
            val value = frame.popInt()
            val obj = frame.pop() as? HeapObject
            obj?.instanceFields?.set("value", value)
        }
        nativeMethods["java/lang/Integer.intValue:()I"] = { frame ->
            val obj = frame.pop() as? HeapObject
            frame.push(obj?.instanceFields?.get("value") as? Int ?: 0)
        }
        nativeMethods["java/lang/Integer.toString:(I)Ljava/lang/String;"] = { frame ->
            frame.push(frame.popInt().toString())
        }
    }

    private fun registerJavaLangBoolean() {
        nativeMethods["java/lang/Boolean.<init>:(Z)V"] = { frame ->
            val value = frame.popInt() != 0
            val obj = frame.pop() as? HeapObject
            obj?.instanceFields?.set("value", value)
        }
        nativeMethods["java/lang/Boolean.booleanValue:()Z"] = { frame ->
            val obj = frame.pop() as? HeapObject
            frame.push(if (obj?.instanceFields?.get("value") as? Boolean == true) 1 else 0)
        }
    }
}
