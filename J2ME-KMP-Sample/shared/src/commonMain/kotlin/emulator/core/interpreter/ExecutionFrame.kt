package emulator.core.interpreter

/**
 * Represents a single method execution context (a "stack frame").
 *
 * HOW THE JVM EXECUTES A METHOD:
 * When a method is called, the JVM creates a new Frame containing:
 *   1. An Operand Stack - a LIFO stack where instructions push/pop values
 *   2. A Local Variables array - indexed slots for arguments and temp vars
 *   3. A Program Counter (PC) - points to the current bytecode instruction
 *
 * Example: executing "int a = 1 + 2;"
 *   iconst_1  → stack: [1]
 *   iconst_2  → stack: [1, 2]
 *   iadd      → stack: [3]       (pops 2 values, pushes result)
 *   istore_1  → stack: []        (pops 3, stores in local var 1)
 *
 * Reference: JVM Spec §2.6 (Frames)
 */
class ExecutionFrame(
    val maxStack: Int,
    val maxLocals: Int,
    val bytecode: ByteArray,
    val className: String,
    val methodName: String
) {
    // Program Counter: index into the bytecode array
    var pc: Int = 0

    // Operand Stack: values are pushed/popped during execution
    private val stack = ArrayDeque<Any?>(maxStack)

    // Local Variables: slot 0 is usually "this" for instance methods
    val locals = arrayOfNulls<Any?>(maxLocals)

    // --- Stack Operations ---

    fun push(value: Any?) {
        stack.addLast(value)
    }

    fun pop(): Any? {
        if (stack.isEmpty()) {
            throw RuntimeException("Stack underflow in $className.$methodName at PC=$pc")
        }
        return stack.removeLast()
    }

    fun popInt(): Int {
        val value = pop()
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Byte -> value.toInt()
            is Short -> value.toInt()
            is Boolean -> if (value) 1 else 0
            else -> throw RuntimeException(
                "Expected int on stack, got ${value?.let { it::class.simpleName } ?: "null"} " +
                "in $className.$methodName at PC=$pc"
            )
        }
    }

    fun popLong(): Long {
        val value = pop()
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            else -> throw RuntimeException("Expected long on stack, got ${value?.let { it::class.simpleName }}")
        }
    }

    fun peek(): Any? {
        return stack.lastOrNull()
    }

    fun stackSize(): Int = stack.size

    // --- Bytecode Reading Helpers ---

    /** Read the next unsigned byte from bytecode and advance PC */
    fun readU1(): Int {
        return bytecode[pc++].toInt() and 0xFF
    }

    /** Read the next signed byte from bytecode and advance PC */
    fun readS1(): Int {
        return bytecode[pc++].toInt() // sign-extended
    }

    /** Read the next unsigned short (2 bytes, big-endian) and advance PC */
    fun readU2(): Int {
        val high = bytecode[pc++].toInt() and 0xFF
        val low = bytecode[pc++].toInt() and 0xFF
        return (high shl 8) or low
    }

    /** Read the next signed short (2 bytes, big-endian) and advance PC */
    fun readS2(): Int {
        val high = bytecode[pc++].toInt()  // sign-extended
        val low = bytecode[pc++].toInt() and 0xFF
        return (high shl 8) or low
    }

    override fun toString(): String {
        return "Frame[$className.$methodName PC=$pc stack=$stack locals=${locals.toList()}]"
    }
}
