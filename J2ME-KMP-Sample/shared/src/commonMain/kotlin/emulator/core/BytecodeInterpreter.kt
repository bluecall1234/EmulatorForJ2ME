package emulator.core

import emulator.core.classfile.ConstantPool
import emulator.core.interpreter.ExecutionEngine
import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Interface for the Java Bytecode interpreter.
 * On iOS, since there's no JVM/Dalvik, we must interpret bytecode ourselves.
 * On Android, we also interpret (instead of using Dalvik) for consistency.
 */
interface BytecodeInterpreter {
    // Set the path of the JAR being executed to allow dynamic class loading
    var currentJarPath: String
    
    // Load a class from a byte array (read from a .class file inside a JAR)
    fun loadClass(className: String, bytecode: ByteArray)
    
    // Get a previously loaded class
    fun getClass(className: String): JavaClassFile?
    // Find and execute a method (e.g., startApp)
    fun executeMethod(className: String, methodName: String, args: Array<Any?>): Any?
    
    // Find and execute a method matching exact descriptor
    fun executeMethod(className: String, methodName: String, descriptor: String, args: Array<Any?>): Any?
    
    // Ensure <clinit> is called before class is used
    fun initializeClass(className: String)
    
    // Manually manage Heap for Java objects
    fun allocateObject(className: String): HeapObject

    companion object {
        var activeInterpreter: BytecodeInterpreter? = null

        fun injectPointerEvent(type: Int, x: Int, y: Int) {
            val interpreter = activeInterpreter ?: return
            
            // Find the active Canvas from Display
            val activeCanvas = emulator.core.api.javax.microedition.lcdui.Display.activeDisplayable ?: return

            val methodName = when (type) {
                0 -> "pointerPressed"
                1 -> "pointerReleased"
                2 -> "pointerDragged"
                else -> return
            }

            var currentClass = interpreter.getClass(activeCanvas.className)
            var targetMethod: emulator.core.classfile.MemberInfo? = null

            while (currentClass != null) {
                targetMethod = currentClass.methods.find {
                    it.getName(currentClass!!.constantPool) == methodName &&
                    it.getDescriptor(currentClass!!.constantPool) == "(II)V"
                }
                if (targetMethod != null) break
                val superName = currentClass.resolvedSuperClassName
                if (superName != null && superName != "java/lang/Object") {
                    currentClass = interpreter.getClass(superName)
                } else {
                    currentClass = null
                }
            }

            if (targetMethod != null && currentClass != null) {
                CoroutineScope(Dispatchers.Default).launch {
                    interpreter.executeMethod(
                        activeCanvas.className,
                        methodName,
                        "(II)V",
                        arrayOf(activeCanvas, x, y)
                    )
                }
            }
        }

        /**
         * Inject a key event (Press/Release) into the active J2ME Canvas.
         * J2ME expects: public void keyPressed(int keyCode)
         * 
         * @param type 0 = Pressed, 1 = Released, 2 = Repeated
         * @param keyCode Standard J2ME KeyCode (e.g. -1 for UP, 48 for '0')
         */
        fun injectKeyEvent(type: Int, keyCode: Int) {
            val interpreter = activeInterpreter ?: return
            val activeCanvas = emulator.core.api.javax.microedition.lcdui.Display.activeDisplayable ?: return

            val methodName = when (type) {
                0 -> "keyPressed"
                1 -> "keyReleased"
                2 -> "keyRepeated"
                else -> return
            }

            // Standard J2ME method: void keyPressed(int keyCode)
            val descriptor = "(I)V"
            
            var currentClass = interpreter.getClass(activeCanvas.className)
            var targetMethod: emulator.core.classfile.MemberInfo? = null

            // Walk up the hierarchy to find the implementation (or use the one in Canvas stub if needed)
            while (currentClass != null) {
                targetMethod = currentClass.methods.find {
                    it.getName(currentClass!!.constantPool) == methodName &&
                    it.getDescriptor(currentClass!!.constantPool) == descriptor
                }
                if (targetMethod != null) break
                
                val superName = currentClass.resolvedSuperClassName
                if (superName != null && superName != "java/lang/Object") {
                    currentClass = interpreter.getClass(superName)
                } else {
                    currentClass = null
                }
            }

            if (targetMethod != null && currentClass != null) {
                println("[BytecodeInterpreter] Injecting $methodName($keyCode) into ${activeCanvas.className}")
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        interpreter.executeMethod(
                            activeCanvas.className,
                            methodName,
                            descriptor,
                            arrayOf(activeCanvas, keyCode)
                        )
                    } catch (e: Exception) {
                        println("[BytecodeInterpreter] Error during $methodName: ${e.message}")
                    }
                }
            } else {
                println("[BytecodeInterpreter] Warning: Method $methodName$descriptor not found in ${activeCanvas.className}")
            }
        }
    }
}

/**
 * Actual implementation of the Interpreter.
 * This class orchestrates loading classes, resolving methods, and executing opcodes.
 * 
 * It uses ExecutionEngine to run the fetch-decode-execute loop on each method's
 * bytecode. Each method call creates a new ExecutionFrame with its own stack
 * and local variables.
 */
class SimpleKMPInterpreter : BytecodeInterpreter {
    
    override var currentJarPath: String = ""
    private val loadedClasses = mutableMapOf<String, JavaClassFile>()
    private val initializedClasses = mutableSetOf<String>()

    override fun loadClass(className: String, bytecode: ByteArray) {
        val classFile = JavaClassFile(className, bytecode)
        loadedClasses[className] = classFile

        println("[Interpreter] Loaded class: ${classFile.resolvedClassName}")
        println("[Interpreter]   Superclass: ${classFile.resolvedSuperClassName}")
        println("[Interpreter]   Constant Pool entries: ${classFile.constantPool.entries.size}")
        println("[Interpreter]   Methods: ${classFile.methods.size}")
    }

    override fun initializeClass(className: String) {
        if (initializedClasses.contains(className)) return
        initializedClasses.add(className)
        
        val clazz = getClass(className) ?: return
        
        // Initialize superclass first
        val superCls = clazz.resolvedSuperClassName
        if (superCls != null && superCls != "java/lang/Object" && superCls != "none") {
            initializeClass(superCls)
        }
        
        if (clazz.isNativeShell) {
            println("[Interpreter] Skipping <clinit> for native shell $className")
            return
        }

        // Find <clinit>
        val clinit = clazz.methods.find {
            it.getName(clazz.constantPool) == "<clinit>" &&
            it.getDescriptor(clazz.constantPool) == "()V"
        }
        if (clinit != null) {
            println("[Interpreter] Invoking <clinit> for $className")
            executeMethodInternal(className, "<clinit>", clinit, emptyArray())
        }
    }

    override fun getClass(className: String): JavaClassFile? {
        // Skip loading for native classes (handled by NativeMethodBridge)
        val isNative = className.startsWith("java/") || 
                       className.startsWith("javax/") ||
                       className.startsWith("com/nokia/") ||
                       className.startsWith("com/siemens/") ||
                       className.startsWith("com/sprintpcs/")

        if (isNative) {
             if (!loadedClasses.containsKey(className)) {
                 // Create a dummy class shell so initializeClass doesn't fail
                 val shell = JavaClassFile(className, byteArrayOf())
                 
                 // Define standard J2ME hierarchies for native shells
                 when (className) {
                     "javax/microedition/lcdui/Canvas" -> shell.overriddenSuperClassName = "javax/microedition/lcdui/Displayable"
                     "javax/microedition/lcdui/game/GameCanvas" -> shell.overriddenSuperClassName = "javax/microedition/lcdui/Canvas"
                     "javax/microedition/lcdui/Displayable" -> shell.overriddenSuperClassName = "java/lang/Object"
                     "javax/microedition/midlet/MIDlet" -> shell.overriddenSuperClassName = "java/lang/Object"
                 }
                 
                 loadedClasses[className] = shell
             }
             return loadedClasses[className]
        }

        // Auto-load class from JAR on demand
        if (!loadedClasses.containsKey(className)) {
            if (currentJarPath.isNotEmpty()) {
                println("[Interpreter] Auto-loading class $className from $currentJarPath...")
                try {
                    val loader = JarLoader()
                    loadedClasses[className] = loader.loadClassFromJar(currentJarPath, className)
                } catch (e: Exception) {
                    println("[Interpreter] ERROR: Failed to auto-load class $className: ${e.message}")
                    return null
                }
            } else {
                println("[Interpreter] Warning: Class $className not loaded and no JAR path set.")
                return null
            }
        }
        return loadedClasses[className]
    }

    override fun executeMethod(className: String, methodName: String, args: Array<Any?>): Any? {
        // Fallback to finding by name only (might be ambiguous if overloaded)
        val classFile = getClass(className) ?: return null
        val methodInfo = classFile.findMethod(methodName) ?: return null
        return executeMethodInternal(className, methodName, methodInfo, args)
    }

    override fun executeMethod(className: String, methodName: String, descriptor: String, args: Array<Any?>): Any? {
        var currentClassName: String? = className
        var methodInfo: emulator.core.classfile.MemberInfo? = null
        
        while (currentClassName != null && currentClassName != "java/lang/Object") {
            val classFile = getClass(currentClassName)
            if (classFile == null) {
                println("[Interpreter] ERROR: Class $currentClassName could not be loaded while resolving $className.$methodName")
                return null
            }
            
            // Find the method by exact name and descriptor
            methodInfo = classFile.methods.find {
                it.getName(classFile.constantPool) == methodName &&
                it.getDescriptor(classFile.constantPool) == descriptor
            }
            
            if (methodInfo != null) {
                // Found it! Execute using the class it was found in (for correct context, though codeAttr is the same)
                return executeMethodInternal(currentClassName, methodName, methodInfo, args)
            }
            
            // Move up the inheritance chain
            currentClassName = classFile.resolvedSuperClassName
        }
        
        println("[Interpreter] ERROR: Method $methodName$descriptor not found in $className or its superclasses")
        return null
    }

    private fun executeMethodInternal(className: String, methodName: String, methodInfo: emulator.core.classfile.MemberInfo, args: Array<Any?>): Any? {
        println("[Interpreter] === Executing $className.$methodName ===")

        // Get the Code attribute (contains the bytecode)
        val codeAttr = methodInfo.getCodeAttribute()
        if (codeAttr == null) {
            println("[Interpreter] Method $methodName is abstract/native - no bytecode")
            return null
        }

        // Create an execution frame
        val frame = ExecutionFrame(
            maxStack = codeAttr.maxStack,
            maxLocals = codeAttr.maxLocals,
            bytecode = codeAttr.bytecode,
            className = className,
            methodName = methodName,
            interpreter = this
        )

        // Set up local variables with arguments
        // For instance methods, local[0] = "this"
        // args go into local[1], local[2], etc.
        if (args.isNotEmpty()) {
            for (i in args.indices) {
                if (i < frame.locals.size) {
                    frame.locals[i] = args[i]
                }
            }
        }

        // Execute the bytecode
        val classFile = getClass(className)!!
        val engine = ExecutionEngine(classFile.constantPool, this)
        val result = engine.execute(frame)

        println("[Interpreter] === $className.$methodName finished, result=$result ===")
        return result
    }

    override fun allocateObject(className: String): HeapObject {
        println("[Interpreter] Allocating memory for object $className")
        return HeapObject(className)
    }
}
