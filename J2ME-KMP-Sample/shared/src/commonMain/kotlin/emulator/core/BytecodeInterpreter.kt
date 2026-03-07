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
        if (superCls != null && superCls != "java/lang/Object") {
            initializeClass(superCls)
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
        val classFile = getClass(className)
        if (classFile == null) {
            println("[Interpreter] ERROR: Class $className could not be loaded")
            return null
        }
        
        // Find the method by exact name and descriptor
        val methodInfo = classFile.methods.find {
            it.getName(classFile.constantPool) == methodName &&
            it.getDescriptor(classFile.constantPool) == descriptor
        }
        if (methodInfo == null) {
            println("[Interpreter] ERROR: Method $methodName$descriptor not found in $className")
            return null
        }
        
        return executeMethodInternal(className, methodName, methodInfo, args)
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
