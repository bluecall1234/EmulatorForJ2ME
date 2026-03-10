package emulator.core.interpreter
import emulator.core.classfile.ConstantPool
import emulator.core.classfile.ConstantPoolEntry
import emulator.core.memory.HeapObject

class JavaExceptionWrapper(val exception: Any?) : RuntimeException(
    "Java Exception: ${if (exception is emulator.core.memory.HeapObject) exception.className else exception?.toString() ?: "null"}"
)

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

    private fun throwException(frame: ExecutionFrame, className: String) {
        val exception = interpreter.allocateObject(className)
        // Note: For now we don't call <init> on the exception for simplicity
        // as it might trigger recursive interpretation during another crash.
        // Most code only cares about the class of the thrown object.
        
        val handlerPc = findExceptionHandler(frame, exception)
        if (handlerPc != -1) {
            frame.pc = handlerPc
            frame.push(exception)
            if (debugMode) println("  [VM] Exception $className thrown and caught! Jumping to PC=$handlerPc")
        } else {
            if (debugMode) println("  [VM] Exception $className thrown but NOT caught in current frame.")
            throw JavaExceptionWrapper(exception)
        }
    }

    /**
     * Execute a method's bytecode in the given frame.
     *
     * @param frame The execution frame containing bytecode, stack, and locals
     * @return The return value of the method (null for void methods)
     */
    fun execute(frame: ExecutionFrame): Any? {
        val bytecode = frame.bytecode

        while (frame.pc < bytecode.size) {
            try {
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
                Opcodes.DLOAD   -> frame.push(frame.locals[frame.readU1()])
                Opcodes.ALOAD   -> frame.push(frame.locals[frame.readU1()])
                Opcodes.ILOAD_0 -> frame.push(frame.locals[0])
                Opcodes.ILOAD_1 -> frame.push(frame.locals[1])
                Opcodes.ILOAD_2 -> frame.push(frame.locals[2])
                Opcodes.ILOAD_3 -> frame.push(frame.locals[3])
                Opcodes.LLOAD_0 -> frame.push(frame.locals[0])
                Opcodes.LLOAD_1 -> frame.push(frame.locals[1])
                Opcodes.LLOAD_2 -> frame.push(frame.locals[2])
                Opcodes.LLOAD_3 -> frame.push(frame.locals[3])
                Opcodes.FLOAD_0 -> frame.push(frame.locals[0])
                Opcodes.FLOAD_1 -> frame.push(frame.locals[1])
                Opcodes.FLOAD_2 -> frame.push(frame.locals[2])
                Opcodes.FLOAD_3 -> frame.push(frame.locals[3])
                Opcodes.DLOAD_0 -> frame.push(frame.locals[0])
                Opcodes.DLOAD_1 -> frame.push(frame.locals[1])
                Opcodes.DLOAD_2 -> frame.push(frame.locals[2])
                Opcodes.DLOAD_3 -> frame.push(frame.locals[3])
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
                Opcodes.DUP_X2 -> {
                    val v1 = frame.pop()
                    val v2 = frame.pop()
                    if (v2 is Long || v2 is Double) {
                        // Form 2: v1 is Category 1, v2 is Category 2
                        frame.push(v1)
                        frame.push(v2)
                        frame.push(v1)
                    } else {
                        // Form 1: v1, v2, v3 are Category 1
                        val v3 = frame.pop()
                        frame.push(v1)
                        frame.push(v3)
                        frame.push(v2)
                        frame.push(v1)
                    }
                }
                Opcodes.DUP2 -> {
                    val v1 = frame.pop()
                    if (v1 is Long || v1 is Double) {
                        // Form 2: v1 is Category 2
                        frame.push(v1)
                        frame.push(v1)
                    } else {
                        // Form 1: v1, v2 are Category 1
                        val v2 = frame.pop()
                        frame.push(v2)
                        frame.push(v1)
                        frame.push(v2)
                        frame.push(v1)
                    }
                }
                Opcodes.DUP2_X1 -> {
                    val v1 = frame.pop()
                    val v2 = frame.pop()
                    if (v1 is Long || v1 is Double) {
                        // Form 2: v1 is Category 2, v2 is Category 1
                        frame.push(v1)
                        frame.push(v2)
                        frame.push(v1)
                    } else {
                        // Form 1: v1, v2, v3 are Category 1
                        val v3 = frame.pop()
                        frame.push(v2)
                        frame.push(v1)
                        frame.push(v3)
                        frame.push(v2)
                        frame.push(v1)
                    }
                }
                Opcodes.DUP2_X2 -> {
                    val v1 = frame.pop()
                    val v2 = frame.pop()
                    if (v1 is Long || v1 is Double) {
                        if (v2 is Long || v2 is Double) {
                            // Form 4: v1(Cat2), v2(Cat2)
                            frame.push(v1)
                            frame.push(v2)
                            frame.push(v1)
                        } else {
                            // Form 2: v1(Cat2), v2(Cat1), v3(Cat1)
                            val v3 = frame.pop()
                            frame.push(v1)
                            frame.push(v3)
                            frame.push(v2)
                            frame.push(v1)
                        }
                    } else {
                        val v3 = frame.pop()
                        if (v3 is Long || v3 is Double) {
                            // Form 3: v1(Cat1), v2(Cat1), v3(Cat2)
                            frame.push(v2)
                            frame.push(v1)
                            frame.push(v3)
                            frame.push(v2)
                            frame.push(v1)
                        } else {
                            // Form 1: v1, v2, v3, v4 all Category 1
                            val v4 = frame.pop()
                            frame.push(v2)
                            frame.push(v1)
                            frame.push(v4)
                            frame.push(v3)
                            frame.push(v2)
                            frame.push(v1)
                        }
                    }
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
                Opcodes.IUSHR -> {
                    val shift = frame.popInt(); val value = frame.popInt()
                    frame.push(value ushr (shift and 0x1F))
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
                Opcodes.LSHL -> {
                    val shift = frame.popInt(); val value = frame.popLong()
                    frame.push(value shl (shift and 0x3F))
                }
                Opcodes.LSHR -> {
                    val shift = frame.popInt(); val value = frame.popLong()
                    frame.push(value shr (shift and 0x3F))
                }
                Opcodes.LUSHR -> {
                    val shift = frame.popInt(); val value = frame.popLong()
                    frame.push(value ushr (shift and 0x3F))
                }
                Opcodes.LAND -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a and b)
                }
                Opcodes.LOR -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a or b)
                }
                Opcodes.LDIV -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    if (b == 0L) throw ArithmeticException("Division by zero")
                    frame.push(a / b)
                }
                Opcodes.LREM -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    if (b == 0L) throw ArithmeticException("Division by zero")
                    frame.push(a % b)
                }
                Opcodes.LNEG -> frame.push(-frame.popLong())
                Opcodes.LXOR -> {
                    val b = frame.popLong(); val a = frame.popLong()
                    frame.push(a xor b)
                }

                // === FLOAT ARITHMETIC ===
                Opcodes.FADD -> {
                    val b = frame.pop() as Float; val a = frame.pop() as Float
                    frame.push(a + b)
                }
                Opcodes.FSUB -> {
                    val b = frame.pop() as Float; val a = frame.pop() as Float
                    frame.push(a - b)
                }
                Opcodes.FMUL -> {
                    val b = frame.pop() as Float; val a = frame.pop() as Float
                    frame.push(a * b)
                }
                Opcodes.FDIV -> {
                    val b = frame.pop() as Float; val a = frame.pop() as Float
                    frame.push(a / b)
                }
                Opcodes.FREM -> {
                    val b = frame.pop() as Float; val a = frame.pop() as Float
                    frame.push(a % b)
                }
                Opcodes.FNEG -> frame.push(-(frame.pop() as Float))

                // === DOUBLE ARITHMETIC ===
                Opcodes.DADD -> {
                    val b = frame.pop() as Double; val a = frame.pop() as Double
                    frame.push(a + b)
                }
                Opcodes.DSUB -> {
                    val b = frame.pop() as Double; val a = frame.pop() as Double
                    frame.push(a - b)
                }
                Opcodes.DMUL -> {
                    val b = frame.pop() as Double; val a = frame.pop() as Double
                    frame.push(a * b)
                }
                Opcodes.DDIV -> {
                    val b = frame.pop() as Double; val a = frame.pop() as Double
                    frame.push(a / b)
                }
                Opcodes.DREM -> {
                    val b = frame.pop() as Double; val a = frame.pop() as Double
                    frame.push(a % b)
                }
                Opcodes.DNEG -> frame.push(-(frame.pop() as Double))

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
                Opcodes.I2D -> frame.push(frame.popInt().toDouble())
                Opcodes.L2I -> frame.push(frame.popLong().toInt())
                Opcodes.L2F -> frame.push(frame.popLong().toFloat())
                Opcodes.L2D -> frame.push(frame.popLong().toDouble())
                Opcodes.F2I -> frame.push((frame.pop() as Float).toInt())
                Opcodes.F2L -> frame.push((frame.pop() as Float).toLong())
                Opcodes.F2D -> frame.push((frame.pop() as Float).toDouble())
                Opcodes.D2I -> frame.push((frame.pop() as Double).toInt())
                Opcodes.D2L -> frame.push((frame.pop() as Double).toLong())
                Opcodes.D2F -> frame.push((frame.pop() as Double).toFloat())
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
                        6 -> FloatArray(count)   // T_FLOAT
                        7 -> DoubleArray(count)  // T_DOUBLE
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
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    val len = when (arr) {
                        is IntArray -> arr.size
                        is ByteArray -> arr.size
                        is CharArray -> arr.size
                        is ShortArray -> arr.size
                        is BooleanArray -> arr.size
                        is LongArray -> arr.size
                        is FloatArray -> arr.size
                        is DoubleArray -> arr.size
                        is Array<*> -> arr.size
                        else -> throw RuntimeException("arraylength on non-array: ${arr?.let { it::class.simpleName }}")
                    }
                    frame.push(len)
                }
                Opcodes.IALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? IntArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index])
                }
                Opcodes.IASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as? IntArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    arr[index] = value
                }
                Opcodes.BALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop()
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    when (arr) {
                        is ByteArray -> {
                            if (index < 0 || index >= arr.size) {
                                throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                                continue
                            }
                            frame.push(arr[index].toInt())
                        }
                        is BooleanArray -> {
                            if (index < 0 || index >= arr.size) {
                                throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                                continue
                            }
                            frame.push(if (arr[index]) 1 else 0)
                        }
                        else -> throw RuntimeException("BALOAD on invalid array type: ${arr?.let { it::class.simpleName }}")
                    }
                }
                Opcodes.BASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop()
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    when (arr) {
                        is ByteArray -> {
                            if (index < 0 || index >= arr.size) {
                                throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                                continue
                            }
                            arr[index] = value.toByte()
                        }
                        is BooleanArray -> {
                            if (index < 0 || index >= arr.size) {
                                throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                                continue
                            }
                            arr[index] = (value != 0)
                        }
                        else -> throw RuntimeException("BASTORE on invalid array type: ${arr?.let { it::class.simpleName }}")
                    }
                }
                Opcodes.CALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? CharArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index].code)
                }
                Opcodes.CASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as? CharArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    arr[index] = value.toChar()
                }
                Opcodes.SALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? ShortArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index].toInt())
                }
                Opcodes.SASTORE -> {
                    val value = frame.popInt()
                    val index = frame.popInt()
                    val arr = frame.pop() as? ShortArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    arr[index] = value.toShort()
                }
                Opcodes.LALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? LongArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index])
                }
                Opcodes.LASTORE -> {
                    val value = frame.popLong()
                    val index = frame.popInt()
                    val arr = frame.pop() as? LongArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    arr[index] = value
                }
                Opcodes.FALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? FloatArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index])
                }
                Opcodes.FASTORE -> {
                    val value = frame.popFloat()
                    val index = frame.popInt()
                    val arr = frame.pop() as? FloatArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    arr[index] = value
                }
                Opcodes.DALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? DoubleArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index])
                }
                Opcodes.DASTORE -> {
                    val value = frame.popDouble()
                    val index = frame.popInt()
                    val arr = frame.pop() as? DoubleArray
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    arr[index] = value
                }
                Opcodes.AALOAD -> {
                    val index = frame.popInt()
                    val arr = frame.pop() as? Array<*>
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
                    frame.push(arr[index])
                }
                Opcodes.AASTORE -> {
                    val value = frame.pop()
                    val index = frame.popInt()
                    @Suppress("UNCHECKED_CAST")
                    val arr = frame.pop() as? Array<Any?>
                    if (arr == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (index < 0 || index >= arr.size) {
                        throwException(frame, "java/lang/ArrayIndexOutOfBoundsException")
                        continue
                    }
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
                    val obj = frame.pop() as? emulator.core.memory.HeapObject
                    if (obj == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    val value = obj.instanceFields["$name:$desc"] ?: getDefaultValueForType(desc)
                    if (debugMode) println("  [VM] GETFIELD $name:$desc -> $value on $obj")
                    frame.push(value)
                }
                
                Opcodes.PUTFIELD -> {
                    val index = frame.readU2()
                    val (_, name, desc) = constantPool.resolveFieldRef(index)
                    val value = frame.pop()
                    val obj = frame.pop() as? emulator.core.memory.HeapObject
                    if (obj == null) {
                        throwException(frame, "java/lang/NullPointerException")
                        continue
                    }
                    if (debugMode) println("  [VM] PUTFIELD $name:$desc = $value on $obj")
                    obj.instanceFields["$name:$desc"] = value
                }

                Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE -> {
                    val index = frame.readU2()
                    val (cls, name, desc) = constantPool.resolveMethodRef(index)
                    val isStatic = (opcode == Opcodes.INVOKESTATIC)
                    val isInterface = (opcode == Opcodes.INVOKEINTERFACE)
                    if (isInterface) {
                        frame.readU1() // count
                        frame.readU1() // zero
                    }

                    if (debugMode) {
                        println("  [VM] ${Opcodes.nameOf(opcode)} $cls.$name$desc")
                    } else if (!cls.startsWith("java/")) {
                        // Lightweight trace for all logic including J2ME APIs
                        println("  [Trace] INVOKE $cls.$name$desc")
                    }

                    val argCount = countMethodArgs(desc)
                    val totalArgs = if (isStatic) argCount else argCount + 1

                    val isCommonApi = cls.startsWith("java/lang/") || cls.startsWith("java/io/")
                    
                    if (debugMode || (!isCommonApi)) { 
                        val obj = if (isStatic) null else frame.peekAt(argCount) as? HeapObject
                        println("  [VM] INVOKE opcode=0x${opcode.toString(16)} cls=$cls name=$name desc=$desc static=$isStatic obj_cls=${obj?.className}")
                    }

                    var currentCls: String? = if (isStatic || opcode == Opcodes.INVOKESPECIAL) {
                        cls
                    } else {
                        val obj = frame.peekAt(argCount)
                        if (obj == null) {
                            throwException(frame, "java/lang/NullPointerException")
                            continue
                        }
                        when (obj) {
                            is HeapObject -> obj.className
                            is String -> "java/lang/String"
                            is IntArray -> "[I"
                            is ByteArray -> "[B"
                            is CharArray -> "[C"
                            is ShortArray -> "[S"
                            is LongArray -> "[J"
                            is FloatArray -> "[F"
                            is DoubleArray -> "[D"
                            is Array<*> -> "[Ljava/lang/Object;"
                            else -> throw RuntimeException("Unsupported object type on stack: ${obj::class.simpleName}")
                        }
                    }
                    
                    var isNativeHandled = false
                    while (currentCls != null && currentCls != "none") {
                        if (debugMode || (!isCommonApi)) {
                            println("    [Hierarchy] Checking $currentCls for $name$desc")
                        }
                        // Check Native Bridge first for this level of the hierarchy
                        isNativeHandled = emulator.core.api.NativeMethodBridge.callNativeMethod(
                            className = currentCls,
                            methodName = name,
                            descriptor = desc,
                            frame = frame,
                            isStatic = isStatic
                        )
                        if (isNativeHandled) break

                        // Check if it's a bytecode class we have loaded
                        val methodClass = interpreter.getClass(currentCls)
                        if (methodClass != null && methodClass.findMethod(name, desc) != null) {
                            // Execute bytecode method
                            val args = Array<Any?>(totalArgs) { null }
                            for (j in totalArgs - 1 downTo 0) {
                                args[j] = frame.pop()
                            }
                            
                            if (isStatic) interpreter.initializeClass(currentCls)
                            val result = interpreter.executeMethod(currentCls, name, desc, args)
                            if (!desc.endsWith(")V")) frame.push(result)
                            
                            isNativeHandled = true // Mark as handled so we don't stub
                            break
                        }

                        // Move up the hierarchy
                        val nextCls = if (methodClass != null && methodClass.resolvedSuperClassName != "none") {
                            methodClass.resolvedSuperClassName
                        } else if (currentCls!!.startsWith("java/") || currentCls.startsWith("javax/")) {
                            // For native classes not in bridge, fallback to java/lang/Object if not already there
                            if (currentCls != "java/lang/Object") "java/lang/Object" else null
                        } else {
                            null
                        }
                        
                        if (nextCls == currentCls) {
                            currentCls = null
                        } else {
                            currentCls = nextCls
                        }
                    }

                    if (!isNativeHandled) {
                        // Stub it out
                        println("  [VM] WARNING: Missing class/method for $cls.$name$desc through hierarchy (stubbing)")
                        for (j in 0 until argCount) {
                            if (frame.stackSize() > 0) frame.pop()
                        }
                        if (!isStatic && frame.stackSize() > 0) frame.pop() // pop 'this'
                        if (!desc.endsWith(")V")) {
                            val retDesc = desc.substring(desc.lastIndexOf(')') + 1)
                            frame.push(getDefaultValueForType(retDesc))
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

                Opcodes.TABLESWITCH -> {
                    // Padding bytes to align the PC to a multiple of 4
                    val padding = (4 - (frame.pc % 4)) % 4
                    for (i in 0 until padding) {
                        frame.readU1()
                    }
                    val defaultOffset: Int = frame.readU4().toInt()
                    val low: Int = frame.readU4().toInt()
                    val high: Int = frame.readU4().toInt()
                    val index: Int = frame.popInt()

                    if (index in low..high) {
                        val offsetToShift = (index - low) * 4
                        for (i in 0 until offsetToShift) {
                            frame.readU1()
                        }
                        val jumpOffset: Int = frame.readU4().toInt()
                        frame.pc = opcodePC + jumpOffset
                    } else {
                        frame.pc = opcodePC + defaultOffset
                    }
                }

                Opcodes.LOOKUPSWITCH -> {
                    val padding = (4 - (frame.pc % 4)) % 4
                    for (i in 0 until padding) {
                        frame.readU1()
                    }
                    val defaultOffset: Int = frame.readU4().toInt()
                    val npairs: Int = frame.readU4().toInt()
                    val key: Int = frame.popInt()
                    var matched = false

                    for (i in 0 until npairs) {
                        val match: Int = frame.readU4().toInt()
                        val offset: Int = frame.readU4().toInt()

                        if (match == key) {
                            frame.pc = opcodePC + offset
                            matched = true
                            break
                        }
                    }
                    if (!matched) {
                        frame.pc = opcodePC + defaultOffset
                    }
                }

                Opcodes.ATHROW -> {
                    val exception = frame.pop()
                    if (exception == null) throw RuntimeException("NullPointerException in ATHROW")
                    
                    val handlerPc = findExceptionHandler(frame, exception)
                    if (handlerPc != -1) {
                        frame.pc = handlerPc
                        frame.push(exception) // Catch block expects exception on stack
                        if (debugMode) println("  [VM] Exception caught! Jumping to PC=$handlerPc")
                    } else {
                        throw JavaExceptionWrapper(exception)
                    }
                }

                Opcodes.ANEWARRAY -> {
                    val index = frame.readU2()
                    val count = frame.popInt()
                    frame.push(arrayOfNulls<Any>(count))
                }
                
                Opcodes.MULTIANEWARRAY -> {
                    val index = frame.readU2()
                    val dimensionsCount = frame.readU1()
                    val className = constantPool.getClassName(index) // e.g., "[[I"
                    
                    val dimensions = IntArray(dimensionsCount)
                    for (i in dimensionsCount - 1 downTo 0) {
                        dimensions[i] = frame.popInt()
                    }
                    
                    fun allocateArray(dimIndex: Int, typeDesc: String): Any {
                        val size = dimensions[dimIndex]
                        return if (dimIndex == dimensionsCount - 1) {
                            // Substring after the `[` array brackets
                            val leafType = typeDesc.substring(typeDesc.lastIndexOf('[') + 1)
                            when (leafType) {
                                "I" -> IntArray(size)
                                "B", "Z" -> ByteArray(size)
                                "C" -> CharArray(size)
                                "S" -> ShortArray(size)
                                "J" -> LongArray(size)
                                "F" -> FloatArray(size)
                                "D" -> DoubleArray(size)
                                else -> arrayOfNulls<Any>(size)
                            }
                        } else {
                            val arr = arrayOfNulls<Any>(size)
                            for (i in 0 until size) {
                                arr[i] = allocateArray(dimIndex + 1, typeDesc)
                            }
                            arr
                        }
                    }
                    
                    if (debugMode) println("  [VM] MULTIANEWARRAY $className with dimensions ${dimensions.contentToString()}")
                    val multiArray = allocateArray(0, className)
                    frame.push(multiArray)
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
            } catch (e: JavaExceptionWrapper) {
                // Propagate to caller
                throw e
            } catch (e: Throwable) {
                val javaEx = when {
                    e.message?.contains("java/io/EOFException") == true -> {
                        println("[VM] Converting native EOFException to Java EOFException")
                        interpreter.allocateObject("java/io/EOFException")
                    }
                    e is IndexOutOfBoundsException -> {
                        println("[VM] Converting native IndexOutOfBoundsException to Java ArrayIndexOutOfBoundsException: ${e.message}")
                        interpreter.allocateObject("java/lang/ArrayIndexOutOfBoundsException")
                    }
                    e is NullPointerException -> {
                        println("[VM] Converting native NullPointerException to Java NullPointerException")
                        interpreter.allocateObject("java/lang/NullPointerException")
                    }
                    else -> {
                        println("[VM] Native exception during execution: ${e.message}")
                        e.printStackTrace()
                        // Wrap other exceptions in RuntimeException for J2ME
                        val errObj = interpreter.allocateObject("java/lang/RuntimeException")
                        // Optionally set message field if we implement it
                        errObj
                    }
                }
                
                val handlerPc = findExceptionHandler(frame, javaEx)
                if (handlerPc != -1) {
                    frame.pc = handlerPc
                    frame.push(javaEx)
                } else {
                    throw JavaExceptionWrapper(javaEx)
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

    private fun findExceptionHandler(frame: ExecutionFrame, exception: Any?): Int {
        val pc = frame.pc - 1 // The opcode that threw is at pc-1 (wait, pc already advanced)
        // Actually, some opcodes advance PC before throwing. 
        // For simplicity, we check the PC at the start of the opcode: opcodePC.
        // Wait, I don't have opcodePC in the catch block. 
        // Let's use frame.pc. Most J2ME compilers include the throwing instruction in the range.

        for (entry in frame.exceptionTable) {
            if (frame.pc > entry.startPc && frame.pc <= entry.endPc) {
                if (entry.catchType == 0) return entry.handlerPc // finally
                
                val catchClassName = constantPool.getClassName(entry.catchType)
                if (isInstanceOf(exception, catchClassName)) {
                    return entry.handlerPc
                }
            }
        }
        return -1
    }

    private fun isInstanceOf(obj: Any?, className: String): Boolean {
        if (obj == null) return false
        val objClassName = (obj as? HeapObject)?.className ?: obj::class.simpleName ?: ""
        if (objClassName == className) return true
        
        // Simple hierarchy check
        var current: String? = objClassName
        while (current != null && current != "java/lang/Object" && current != "none") {
            if (current == className) return true
            val clazz = interpreter.getClass(current)
            current = clazz?.resolvedSuperClassName
        }
        return false
    }
}
