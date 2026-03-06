package emulator.core.interpreter

import emulator.core.classfile.ConstantPool
import emulator.core.classfile.ConstantPoolEntry

/**
 * The Execution Engine - the core "brain" of the J2ME emulator.
 *
 * HOW IT WORKS (Fetch-Decode-Execute loop):
 * 1. FETCH:  Read the opcode byte at the current PC position
 * 2. DECODE: Look up what the opcode means (e.g., 0x60 = iadd)
 * 3. EXECUTE: Perform the operation (e.g., pop 2 ints, add, push result)
 * 4. REPEAT: Move PC forward and go to step 1
 *
 * This loop continues until we hit a "return" opcode, which pops the
 * current frame off the call stack and returns to the caller.
 *
 * WHY THIS APPROACH?
 * On iOS, there's no JVM or Dalvik. We must interpret every bytecode
 * instruction ourselves. This is called a "tree-walking interpreter" or
 * more precisely a "bytecode interpreter". It's slower than JIT compilation
 * but much simpler and works perfectly for J2ME games which are lightweight.
 *
 * Reference: JVM Spec §2.11 (Instruction Set Summary)
 */
class ExecutionEngine(
    private val constantPool: ConstantPool,
    private val interpreter: emulator.core.BytecodeInterpreter
) {

    // Enable/disable verbose logging of each opcode execution
    var debugMode: Boolean = true

    /**
     * Execute a method's bytecode in the given frame.
     *
     * @param frame The execution frame containing bytecode, stack, and locals
     * @return The return value of the method (null for void methods)
     */
    fun execute(frame: ExecutionFrame): Any? {
        val bytecode = frame.bytecode

        while (frame.pc < bytecode.size) {
            val opcodePC = frame.pc  // Save PC for logging
            val opcode = frame.readU1()

            if (debugMode) {
                println("  [VM] PC=$opcodePC ${Opcodes.nameOf(opcode)}")
            }

            when (opcode) {
                // === CONSTANTS: Push fixed values onto the stack ===
                Opcodes.NOP -> { /* Do nothing */ }
                Opcodes.ACONST_NULL -> frame.push(null)
                Opcodes.ICONST_M1  -> frame.push(-1)
                Opcodes.ICONST_0   -> frame.push(0)
                Opcodes.ICONST_1   -> frame.push(1)
                Opcodes.ICONST_2   -> frame.push(2)
                Opcodes.ICONST_3   -> frame.push(3)
                Opcodes.ICONST_4   -> frame.push(4)
                Opcodes.ICONST_5   -> frame.push(5)
                Opcodes.LCONST_0   -> frame.push(0L)
                Opcodes.LCONST_1   -> frame.push(1L)
                Opcodes.FCONST_0   -> frame.push(0.0f)
                Opcodes.FCONST_1   -> frame.push(1.0f)
                Opcodes.FCONST_2   -> frame.push(2.0f)

                // Push a byte value as int
                Opcodes.BIPUSH -> {
                    val value = frame.readS1()
                    frame.push(value)
                }

                // Push a short value as int
                Opcodes.SIPUSH -> {
                    val value = frame.readS2()
                    frame.push(value)
                }

                // Load constant from pool (1-byte index)
                Opcodes.LDC -> {
                    val index = frame.readU1()
                    frame.push(resolveConstant(index))
                }

                // Load constant from pool (2-byte index)
                Opcodes.LDC_W -> {
                    val index = frame.readU2()
                    frame.push(resolveConstant(index))
                }

                // Load long/double constant from pool
                Opcodes.LDC2_W -> {
                    val index = frame.readU2()
                    frame.push(resolveConstant(index))
                }

                // === LOAD: Copy value from local variable to stack ===
                Opcodes.ILOAD   -> frame.push(frame.locals[frame.readU1()])
                Opcodes.LLOAD   -> frame.push(frame.locals[frame.readU1()])
                Opcodes.FLOAD   -> frame.push(frame.locals[frame.readU1()])
                Opcodes.ALOAD   -> frame.push(frame.locals[frame.readU1()])
                Opcodes.ILOAD_0 -> frame.push(frame.locals[0])
                Opcodes.ILOAD_1 -> frame.push(frame.locals[1])
                Opcodes.ILOAD_2 -> frame.push(frame.locals[2])
                Opcodes.ILOAD_3 -> frame.push(frame.locals[3])
                Opcodes.LLOAD_0 -> frame.push(frame.locals[0])
                Opcodes.LLOAD_1 -> frame.push(frame.locals[1])
                Opcodes.ALOAD_0 -> frame.push(frame.locals[0])
                Opcodes.ALOAD_1 -> frame.push(frame.locals[1])
                Opcodes.ALOAD_2 -> frame.push(frame.locals[2])
                Opcodes.ALOAD_3 -> frame.push(frame.locals[3])

                // === STORE: Pop value from stack into local variable ===
                Opcodes.ISTORE   -> frame.locals[frame.readU1()] = frame.pop()
                Opcodes.LSTORE   -> frame.locals[frame.readU1()] = frame.pop()
                Opcodes.FSTORE   -> frame.locals[frame.readU1()] = frame.pop()
                Opcodes.ASTORE   -> frame.locals[frame.readU1()] = frame.pop()
                Opcodes.ISTORE_0 -> frame.locals[0] = frame.pop()
                Opcodes.ISTORE_1 -> frame.locals[1] = frame.pop()
                Opcodes.ISTORE_2 -> frame.locals[2] = frame.pop()
                Opcodes.ISTORE_3 -> frame.locals[3] = frame.pop()
                Opcodes.LSTORE_0 -> frame.locals[0] = frame.pop()
                Opcodes.LSTORE_1 -> frame.locals[1] = frame.pop()
                Opcodes.LSTORE_2 -> frame.locals[2] = frame.pop()
                Opcodes.LSTORE_3 -> frame.locals[3] = frame.pop()
                Opcodes.FSTORE_0 -> frame.locals[0] = frame.pop()
                Opcodes.FSTORE_1 -> frame.locals[1] = frame.pop()
                Opcodes.FSTORE_2 -> frame.locals[2] = frame.pop()
                Opcodes.FSTORE_3 -> frame.locals[3] = frame.pop()
                Opcodes.ASTORE_0 -> frame.locals[0] = frame.pop()
                Opcodes.ASTORE_1 -> frame.locals[1] = frame.pop()
                Opcodes.ASTORE_2 -> frame.locals[2] = frame.pop()
                Opcodes.ASTORE_3 -> frame.locals[3] = frame.pop()

                // === STACK MANIPULATION ===
                Opcodes.POP -> frame.pop()
                Opcodes.POP2 -> { frame.pop(); frame.pop() }
                Opcodes.DUP -> {
                    val v = frame.pop()
                    frame.push(v)
                    frame.push(v)
                }
                Opcodes.DUP_X1 -> {
                    val v1 = frame.pop()
                    val v2 = frame.pop()
                    frame.push(v1)
                    frame.push(v2)
                    frame.push(v1)
                }
                Opcodes.SWAP -> {
                    val v1 = frame.pop()
                    val v2 = frame.pop()
                    frame.push(v1)
                    frame.push(v2)
                }

                // === INTEGER ARITHMETIC ===
                Opcodes.IADD -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    frame.push(a + b)
                }
                Opcodes.ISUB -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    frame.push(a - b)
                }
                Opcodes.IMUL -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    frame.push(a * b)
                }
                Opcodes.IDIV -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    if (b == 0) throw ArithmeticException("Division by zero")
                    frame.push(a / b)
                }
                Opcodes.IREM -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    if (b == 0) throw ArithmeticException("Division by zero")
                    frame.push(a % b)
                }
                Opcodes.INEG -> frame.push(-frame.popInt())
                Opcodes.ISHL -> {
                    val shift = frame.popInt(); val value = frame.popInt()
                    frame.push(value shl (shift and 0x1F))
                }
                Opcodes.ISHR -> {
                    val shift = frame.popInt(); val value = frame.popInt()
                    frame.push(value shr (shift and 0x1F))
                }
                Opcodes.IAND -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    frame.push(a and b)
                }
                Opcodes.IOR -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    frame.push(a or b)
                }
                Opcodes.IXOR -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    frame.push(a xor b)
                }

                // === LONG ARITHMETIC ===
                Opcodes.LADD -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a + b)
                }
                Opcodes.LSUB -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a - b)
                }
                Opcodes.LMUL -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a * b)
                }
                Opcodes.LCMP -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a.compareTo(b))
                }
                Opcodes.FCMPL, Opcodes.FCMPG -> {
                    val b = frame.pop() as Float; val a = frame.pop() as Float
                    when {
                        a.isNaN() || b.isNaN() -> frame.push(if (opcode == Opcodes.FCMPG) 1 else -1)
                        a > b -> frame.push(1)
                        a == b -> frame.push(0)
                        else -> frame.push(-1)
                    }
                }
                Opcodes.DCMPL, Opcodes.DCMPG -> {
                    val b = frame.pop() as Double; val a = frame.pop() as Double
                    when {
                        a.isNaN() || b.isNaN() -> frame.push(if (opcode == Opcodes.DCMPG) 1 else -1)
                        a > b -> frame.push(1)
                        a == b -> frame.push(0)
                        else -> frame.push(-1)
                    }
                }

                // === INCREMENT ===
                Opcodes.IINC -> {
                    val index = frame.readU1()
                    val increment = frame.readS1()
                    val current = (frame.locals[index] as? Int) ?: 0
                    frame.locals[index] = current + increment
                }

                // === TYPE CONVERSION ===
                Opcodes.I2L -> frame.push(frame.popInt().toLong())
                Opcodes.I2F -> frame.push(frame.popInt().toFloat())
                Opcodes.I2B -> frame.push(frame.popInt().toByte().toInt())
                Opcodes.I2C -> frame.push(frame.popInt() and 0xFFFF)
                Opcodes.I2S -> frame.push(frame.popInt().toShort().toInt())

                // === COMPARISON & BRANCHING ===
                // Single-operand branches (compare with 0)
                Opcodes.IFEQ -> conditionalBranch(frame, opcodePC) { frame.popInt() == 0 }
                Opcodes.IFNE -> conditionalBranch(frame, opcodePC) { frame.popInt() != 0 }
                Opcodes.IFLT -> conditionalBranch(frame, opcodePC) { frame.popInt() < 0 }
                Opcodes.IFGE -> conditionalBranch(frame, opcodePC) { frame.popInt() >= 0 }
                Opcodes.IFGT -> conditionalBranch(frame, opcodePC) { frame.popInt() > 0 }
                Opcodes.IFLE -> conditionalBranch(frame, opcodePC) { frame.popInt() <= 0 }

                // Two-operand branches (compare 2 ints)
                Opcodes.IF_ICMPEQ -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    conditionalBranchPreRead(frame, opcodePC) { a == b }
                }
                Opcodes.IF_ICMPNE -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    conditionalBranchPreRead(frame, opcodePC) { a != b }
                }
                Opcodes.IF_ICMPLT -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    conditionalBranchPreRead(frame, opcodePC) { a < b }
                }
                Opcodes.IF_ICMPGE -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    conditionalBranchPreRead(frame, opcodePC) { a >= b }
                }
                Opcodes.IF_ICMPGT -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    conditionalBranchPreRead(frame, opcodePC) { a > b }
                }
                Opcodes.IF_ICMPLE -> {
                    val b = frame.popInt(); val a = frame.popInt()
                    conditionalBranchPreRead(frame, opcodePC) { a <= b }
                }

                // Reference comparison
                Opcodes.IF_ACMPEQ -> {
                    val b = frame.pop(); val a = frame.pop()
                    conditionalBranchPreRead(frame, opcodePC) { a === b }
                }
                Opcodes.IF_ACMPNE -> {
                    val b = frame.pop(); val a = frame.pop()
                    conditionalBranchPreRead(frame, opcodePC) { a !== b }
                }

                // Null checks
                Opcodes.IFNULL    -> conditionalBranch(frame, opcodePC) { frame.pop() == null }
                Opcodes.IFNONNULL -> conditionalBranch(frame, opcodePC) { frame.pop() != null }

                // Unconditional jump
                Opcodes.GOTO -> {
                    val offset = frame.readS2()
                    frame.pc = opcodePC + offset
                }

                // === RETURN ===
                Opcodes.RETURN  -> {
                    if (debugMode) println("  [VM] Method ${frame.methodName} returned void")
                    return null
                }
                Opcodes.IRETURN -> {
                    val result = frame.popInt()
                    if (debugMode) println("  [VM] Method ${frame.methodName} returned int: $result")
                    return result
                }
                Opcodes.LRETURN -> {
                    val result = frame.popLong()
                    if (debugMode) println("  [VM] Method ${frame.methodName} returned long: $result")
                    return result
                }
                Opcodes.ARETURN -> {
                    val result = frame.pop()
                    if (debugMode) println("  [VM] Method ${frame.methodName} returned ref: $result")
                    return result
                }

                // === ARRAY OPERATIONS ===
                Opcodes.NEWARRAY -> {
                    val atype = frame.readU1()
                    val count = frame.popInt()
                    val array = when (atype) {
                        4 -> BooleanArray(count) // T_BOOLEAN
                        5 -> CharArray(count)    // T_CHAR
                        8 -> ByteArray(count)    // T_BYTE
                        9 -> ShortArray(count)   // T_SHORT
                        10 -> IntArray(count)    // T_INT
                        11 -> LongArray(count)   // T_LONG
                        else -> IntArray(count)  // default
                    }
                    frame.push(array)
                }
                Opcodes.ARRAYLENGTH -> {
                    val arr = frame.pop()
                    val len = when (arr) {
                        is IntArray -> arr.size
                        is ByteArray -> arr.size
                        is CharArray -> arr.size
                        is BooleanArray -> arr.size
                        is LongArray -> arr.size
                        is Array<*> -> arr.size
                        else -> throw RuntimeException("arraylength on non-array: ${arr?.let { it::class.simpleName }}")
                    }
                    frame.push(len)
                }
                Opcodes.IALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as IntArray
                    frame.push(arr[index])
                }
                Opcodes.IASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as IntArray
                    arr[index] = value
                }
                Opcodes.BALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as ByteArray
                    frame.push(arr[index].toInt())
                }
                Opcodes.BASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as ByteArray
                    arr[index] = value.toByte()
                }
                Opcodes.CALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as CharArray
                    frame.push(arr[index].code)
                }
                Opcodes.CASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as CharArray
                    arr[index] = value.toChar()
                }
                Opcodes.SALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as ShortArray
                    frame.push(arr[index].toInt())
                }
                Opcodes.SASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as ShortArray
                    arr[index] = value.toShort()
                }
                Opcodes.LALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as LongArray
                    frame.push(arr[index])
                }
                Opcodes.LASTORE -> {
                    val value = frame.popLong()
                    val index = frame.popInt()
                    val arr = frame.pop() as LongArray
                    arr[index] = value
                }
                Opcodes.FALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as FloatArray
                    frame.push(arr[index])
                }
                Opcodes.FASTORE -> {
                    val value = frame.pop() as Float
                    val index = frame.popInt()
                    val arr = frame.pop() as FloatArray
                    arr[index] = value
                }
                Opcodes.DALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as DoubleArray
                    frame.push(arr[index])
                }
                Opcodes.DASTORE -> {
                    val value = frame.pop() as Double
                    val index = frame.popInt()
                    val arr = frame.pop() as DoubleArray
                    arr[index] = value
                }

                // === OBJECT ALLOCATION & FIELD ACCESS ===
                Opcodes.NEW -> {
                    val index = frame.readU2()
                    val className = constantPool.getClassName(index)
                    if (debugMode) println("  [VM] NEW $className")
                    interpreter.initializeClass(className)
                    frame.push(interpreter.allocateObject(className))
                }

                Opcodes.GETSTATIC -> {
                    val index = frame.readU2()
                    val (cls, name, desc) = constantPool.resolveFieldRef(index)
                    interpreter.initializeClass(cls)
                    val targetClass = interpreter.getClass(cls) 
                        ?: throw RuntimeException("Class not loaded: $cls")
                    val value = targetClass.staticFields["$name:$desc"] ?: getDefaultValueForType(desc)
                    if (debugMode) println("  [VM] GETSTATIC $cls.$name:$desc -> $value")
                    frame.push(value)
                }
                
                Opcodes.PUTSTATIC -> {
                    val index = frame.readU2()
                    val (cls, name, desc) = constantPool.resolveFieldRef(index)
                    val value = frame.pop()
                    if (debugMode) println("  [VM] PUTSTATIC $cls.$name:$desc = $value")
                    interpreter.initializeClass(cls)
                    val targetClass = interpreter.getClass(cls) 
                        ?: throw RuntimeException("Class not loaded: $cls")
                    targetClass.staticFields["$name:$desc"] = value
                }
                
                Opcodes.GETFIELD -> {
                    val index = frame.readU2()
                    val (_, name, desc) = constantPool.resolveFieldRef(index)
                    val obj = frame.pop() as emulator.core.memory.HeapObject
                    val value = obj.instanceFields["$name:$desc"] ?: getDefaultValueForType(desc)
                    if (debugMode) println("  [VM] GETFIELD $name:$desc -> $value on $obj")
                    frame.push(value)
                }
                
                Opcodes.PUTFIELD -> {
                    val index = frame.readU2()
                    val (_, name, desc) = constantPool.resolveFieldRef(index)
                    val value = frame.pop()
                    val obj = frame.pop() as emulator.core.memory.HeapObject
                    if (debugMode) println("  [VM] PUTFIELD $name:$desc = $value on $obj")
                    obj.instanceFields["$name:$desc"] = value
                }

                Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC -> {
                    val index = frame.readU2()
                    val (cls, name, desc) = constantPool.resolveMethodRef(index)
                    val isStatic = (opcode == Opcodes.INVOKESTATIC)

                    if (debugMode) println("  [VM] ${Opcodes.nameOf(opcode)} $cls.$name$desc")
                    
                    if (isStatic) {
                        interpreter.initializeClass(cls)
                    }

                    // FIX #4: Pass isStatic so NativeMethodBridge pops 'this' correctly
                    val isHandled = emulator.core.api.NativeMethodBridge.callNativeMethod(
                        className = cls,
                        methodName = name,
                        descriptor = desc,
                        frame = frame,
                        isStatic = isStatic
                    )

                    // If not handled by NativeBridge, it means it's a custom game class 
                    // (or an unimplemented API we must stub if it doesn't exist).
                    if (!isHandled) {
                        val methodClass = interpreter.getClass(cls)
                        
                        if (methodClass != null && methodClass.findMethod(name) != null) {
                            // Phase 5.2: Dynamic Bytecode Execution for non-native methods!
                            val argCount = countMethodArgs(desc)
                            val totalArgs = if (isStatic) argCount else argCount + 1
                            val args = Array<Any?>(totalArgs) { null }
                            
                            // Pop arguments from stack (in reverse order)
                            for (j in totalArgs - 1 downTo 0) {
                                args[j] = frame.pop()
                            }
                            
                            // Let the interpreter recursively execute the bytecode
                            val result = interpreter.executeMethod(cls, name, desc, args)
                            
                            // Push the return value back (if any)
                            if (!desc.endsWith(")V")) {
                                frame.push(result)
                            }
                        } else {
                            // Stub it out (e.g. missing API or interface we haven't ported)
                            println("  [VM] WARNING: Missing class/method for $cls.$name$desc (stubbing)")
                            val argCount = countMethodArgs(desc)
                            for (j in 0 until argCount) {
                                if (frame.stackSize() > 0) frame.pop()
                            }
                            // Pop 'this' only for non-static invocations
                            if (!isStatic && frame.stackSize() > 0) frame.pop()
                            if (!desc.endsWith(")V")) {
                                frame.push(0) // dummy return value
                            }
                        }
                    }
                }

                Opcodes.INVOKEINTERFACE -> {
                    val index = frame.readU2()
                    val _count = frame.readU1() // argument count (redundant)
                    val _zero = frame.readU1()  // always 0
                    val (cls, name, desc) = constantPool.resolveMethodRef(index)
                    if (debugMode) println("  [VM] invokeinterface $cls.$name$desc")

                    // FIX #7: Route through NativeMethodBridge (same as INVOKEVIRTUAL)
                    val isHandled = emulator.core.api.NativeMethodBridge.callNativeMethod(
                        className = cls,
                        methodName = name,
                        descriptor = desc,
                        frame = frame,
                        isStatic = false
                    )

                    if (!isHandled) {
                        val methodClass = interpreter.getClass(cls)
                        
                        if (methodClass != null && methodClass.findMethod(name) != null) {
                            val argCount = countMethodArgs(desc)
                            val totalArgs = argCount + 1 // Interfaces are always instance methods (have 'this')
                            val args = Array<Any?>(totalArgs) { null }
                            
                            for (j in totalArgs - 1 downTo 0) {
                                args[j] = frame.pop()
                            }
                            val result = interpreter.executeMethod(cls, name, desc, args)
                            if (!desc.endsWith(")V")) {
                                frame.push(result)
                            }
                        } else {
                            println("  [VM] WARNING: Unhandled invokeinterface $cls.$name$desc (stub)")
                            val argCount = countMethodArgs(desc)
                            for (j in 0 until argCount) {
                                if (frame.stackSize() > 0) frame.pop()
                            }
                            if (frame.stackSize() > 0) frame.pop() // pop "this"
                            if (!desc.endsWith(")V")) frame.push(0)
                        }
                    }
                }

                Opcodes.CHECKCAST -> {
                    val index = frame.readU2()
                    // Don't pop - checkcast leaves the reference on the stack
                    if (debugMode) {
                        val cls = constantPool.getClassName(index)
                        println("  [VM] checkcast $cls (stub - always passes)")
                    }
                }
                Opcodes.INSTANCEOF -> {
                    val index = frame.readU2()
                    val obj = frame.pop()
                    if (debugMode) {
                        val cls = constantPool.getClassName(index)
                        println("  [VM] instanceof $cls (stub - always true)")
                    }
                    frame.push(if (obj != null) 1 else 0)
                }

                Opcodes.ATHROW -> {
                    val exception = frame.pop()
                    throw RuntimeException("J2ME Exception thrown: $exception")
                }

                Opcodes.ANEWARRAY -> {
                    val index = frame.readU2()
                    val count = frame.popInt()
                    frame.push(arrayOfNulls<Any>(count))
                }
                Opcodes.AALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as Array<*>
                    frame.push(arr[index])
                }
                Opcodes.AASTORE -> {
                    val value = frame.pop()
                    val index = frame.popInt()
                    @Suppress("UNCHECKED_CAST")
                    val arr = frame.pop() as Array<Any?>
                    arr[index] = value
                }

                Opcodes.MONITORENTER -> {
                    val obj = frame.pop()
                    if (debugMode) println("  [VM] MONITORENTER on $obj")
                    // Note: Since J2ME emulator is single-threaded, we can safely ignore the lock
                }

                Opcodes.MONITOREXIT -> {
                    val obj = frame.pop()
                    if (debugMode) println("  [VM] MONITOREXIT on $obj")
                    // Note: Since J2ME emulator is single-threaded, we can safely ignore the unlock
                }

                else -> {
                    throw RuntimeException(
                        "Unimplemented opcode: 0x${opcode.toString(16)} " +
                        "(${Opcodes.nameOf(opcode)}) at PC=$opcodePC " +
                        "in ${frame.className}.${frame.methodName}"
                    )
                }
            }
        }

        // If we reach the end of bytecode without a return instruction
        if (debugMode) println("  [VM] Method ${frame.methodName} fell off end (implicit return)")
        return null
    }

    // --- Helper Methods ---

    /**
     * Resolve a constant pool entry to a runtime value.
     */
    private fun resolveConstant(index: Int): Any? {
        return when (val entry = constantPool.entries[index]) {
            is ConstantPoolEntry.IntegerInfo -> entry.value
            is ConstantPoolEntry.FloatInfo -> entry.value
            is ConstantPoolEntry.LongInfo -> entry.value
            is ConstantPoolEntry.DoubleInfo -> entry.value
            is ConstantPoolEntry.StringRef -> constantPool.getUtf8(entry.stringIndex)
            is ConstantPoolEntry.ClassRef -> constantPool.getClassName(index)
            else -> throw RuntimeException("Cannot resolve constant at index $index: ${entry::class.simpleName}")
        }
    }

    /**
     * Handle a conditional branch where the condition check reads from the stack.
     * The offset is 2 bytes relative to the opcode start position.
     */
    private inline fun conditionalBranch(frame: ExecutionFrame, opcodePC: Int, condition: () -> Boolean) {
        val offset = frame.readS2()
        if (condition()) {
            frame.pc = opcodePC + offset
            if (debugMode) println("  [VM]   -> branch taken to PC=${frame.pc}")
        } else {
            if (debugMode) println("  [VM]   -> branch NOT taken")
        }
    }

    /**
     * Like conditionalBranch but the values are already popped from stack.
     */
    private inline fun conditionalBranchPreRead(frame: ExecutionFrame, opcodePC: Int, condition: () -> Boolean) {
        val offset = frame.readS2()
        if (condition()) {
            frame.pc = opcodePC + offset
            if (debugMode) println("  [VM]   -> branch taken to PC=${frame.pc}")
        }
    }

    /**
     * Count the number of arguments in a method descriptor.
     * Example: "(ILjava/lang/String;Z)V" -> 3 arguments (int, String, boolean)
     */
    private fun countMethodArgs(descriptor: String): Int {
        var count = 0
        var i = 1 // skip opening '('
        while (i < descriptor.length && descriptor[i] != ')') {
            when (descriptor[i]) {
                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> {
                    count++
                    i++
                }
                'L' -> {
                    count++
                    i = descriptor.indexOf(';', i) + 1
                }
                '[' -> {
                    i++ // skip array prefix, the base type will be counted
                }
                else -> i++
            }
        }
        return count
    }

    private fun getDefaultValueForType(desc: String): Any? {
        if (desc.isEmpty()) return null
        return when (desc[0]) {
            'B', 'S', 'I', 'Z', 'C' -> 0
            'J' -> 0L
            'F' -> 0.0f
            'D' -> 0.0
            else -> null // 'L' (Object) or '[' (Array)
        }
    }
}
