package emulator.core.memory

/**
 * Represents a Java object allocated in the emulator's virtual heap.
 * 
 * In a real JVM, an object is a block of contiguous memory holding an object header
 * and the values of its instance variables. In our KMP emulator, we represent this
 * simply as a Kotlin object with a Map of field names to values.
 * 
 * Garbage Collection (GC) is automatically handled by Kotlin Native/JVM since the
 * emulator is written in Kotlin. When the `ExecutionEngine` stack no longer references
 * a `HeapObject`, Kotlin's GC will clean it up.
 */
class HeapObject(val className: String) {
    
    // Stores the values of instance variables (fields).
    // Key format: "fieldName:descriptor" to handle shadowed fields, 
    // or simply "fieldName" for simplicity in this initial phase.
    val instanceFields = mutableMapOf<String, Any?>()

    override fun toString(): String {
        return "$className@${hashCode().toString(16)}"
    }
}

/**
 * Represents a Java Array allocated in the emulator's virtual heap.
 * For primitives, we use Kotlin's primitive arrays (IntArray, ByteArray, etc.)
 * directly for performance, so we only need a wrapper if we want to unify them.
 * Currently, ExecutionEngine pushes bare IntArray/ByteArray onto the stack, 
 * which is perfectly fine. 
 */
// (Primitive arrays are stored directly on the operand stack)
