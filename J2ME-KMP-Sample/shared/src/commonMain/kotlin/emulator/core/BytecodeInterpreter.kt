package emulator.core

import emulator.core.classfile.ConstantPool
import emulator.core.interpreter.ExecutionEngine
import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Interface for the Java Bytecode interpreter.
 * On iOS, since there's no JVM/Dalvik, we must interpret bytecode ourselves.
 * On Android, we also interpret (instead of using Dalvik) for consistency.
 */
interface BytecodeInterpreter {
    // Load a class from a byte array (read from a .class file inside a JAR)
    fun loadClass(className: String, bytecode: ByteArray)
    
    // Get a previously loaded class
    fun getClass(className: String): JavaClassFile?
    
    // Find and execute a method (e.g., startApp)
    fun executeMethod(className: String, methodName: String, args: Array<Any?>): Any?
    
    // Manually manage Heap for Java objects
    fun allocateObject(className: String): HeapObject
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
    
    private val loadedClasses = mutableMapOf<String, JavaClassFile>()

    override fun loadClass(className: String, bytecode: ByteArray) {
        val classFile = JavaClassFile(className, bytecode)
        loadedClasses[className] = classFile

        println("[Interpreter] Loaded class: ${classFile.resolvedClassName}")
        println("[Interpreter]   Superclass: ${classFile.resolvedSuperClassName}")
        println("[Interpreter]   Constant Pool entries: ${classFile.constantPool.entries.size}")
        println("[Interpreter]   Methods: ${classFile.methods.size}")
    }

    override fun getClass(className: String): JavaClassFile? {
        // Auto-load a mock class if requested (for testing purposes)
        if (!loadedClasses.containsKey(className)) {
            println("[Interpreter] Warning: Class $className not loaded. Loading mock...")
            val loader = JarLoader()
            loadedClasses[className] = loader.loadClassFromJar("", className)
        }
        return loadedClasses[className]
    }

    override fun executeMethod(className: String, methodName: String, args: Array<Any?>): Any? {
        println("[Interpreter] === Executing $className.$methodName ===")
        
        val classFile = getClass(className)
        if (classFile == null) {
            println("[Interpreter] ERROR: Class $className could not be loaded")
            return null
        }
        
        // Find the method
        val methodInfo = classFile.findMethod(methodName)
        if (methodInfo == null) {
            println("[Interpreter] ERROR: Method $methodName not found in $className")
            return null
        }

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
            methodName = methodName
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
