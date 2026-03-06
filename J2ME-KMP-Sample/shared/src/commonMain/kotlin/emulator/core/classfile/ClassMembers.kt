package emulator.core.classfile

/**
 * Represents a parsed Field or Method from a .class file.
 *
 * In the JVM class file format, both fields and methods share the same structure:
 * ┌──────────────────────────────┐
 * │ access_flags  (2 bytes)     │  public, private, static, etc.
 * │ name_index    (2 bytes)     │  -> Constant Pool (Utf8): field/method name
 * │ descriptor_index (2 bytes)  │  -> Constant Pool (Utf8): type descriptor
 * │ attributes_count (2 bytes)  │
 * │ attributes[]  (variable)    │  Contains "Code" attribute for methods
 * └──────────────────────────────┘
 *
 * Reference: JVM Spec §4.5 (Fields), §4.6 (Methods)
 */
data class MemberInfo(
    val accessFlags: Int,
    val nameIndex: Int,
    val descriptorIndex: Int,
    val attributes: List<AttributeInfo>
) {
    /**
     * Resolve the member name from the constant pool.
     * Example: "startApp", "main", "x", "width"
     */
    fun getName(pool: ConstantPool): String = pool.getUtf8(nameIndex)

    /**
     * Resolve the type descriptor from the constant pool.
     *
     * Field descriptors:  "I" = int, "Z" = boolean, "Ljava/lang/String;" = String
     * Method descriptors: "()V" = void method(), "(II)I" = int method(int, int)
     */
    fun getDescriptor(pool: ConstantPool): String = pool.getUtf8(descriptorIndex)

    /**
     * Find the "Code" attribute (only present in non-abstract, non-native methods).
     * The Code attribute contains the actual bytecode instructions.
     */
    fun getCodeAttribute(): CodeAttribute? {
        return attributes
            .filterIsInstance<AttributeInfo.Code>()
            .firstOrNull()
            ?.codeAttribute
    }

    companion object {
        // Access flag constants (JVM Spec §4.5-Table 4.5-A, §4.6-Table 4.6-A)
        const val ACC_PUBLIC    = 0x0001
        const val ACC_PRIVATE   = 0x0002
        const val ACC_PROTECTED = 0x0004
        const val ACC_STATIC    = 0x0008
        const val ACC_FINAL     = 0x0010
        const val ACC_NATIVE    = 0x0100
        const val ACC_ABSTRACT  = 0x0400
    }
}

/**
 * Represents a parsed attribute from a field, method, or class.
 *
 * Attributes are extensible metadata. The most important one for us is "Code",
 * which contains the bytecode. Other attributes (like "LineNumberTable") are
 * useful for debugging but not required for execution.
 *
 * Reference: JVM Spec §4.7
 */
sealed class AttributeInfo {
    /**
     * The "Code" attribute - contains the actual bytecode of a method.
     *
     * Structure:
     * ┌──────────────────────────────┐
     * │ max_stack     (2 bytes)      │  Max depth of operand stack
     * │ max_locals    (2 bytes)      │  Number of local variables
     * │ code_length   (4 bytes)      │  Length of bytecode array
     * │ code[]        (variable)     │  THE ACTUAL BYTECODE INSTRUCTIONS
     * │ exception_table (variable)   │  try-catch handlers
     * │ attributes[]  (variable)     │  Sub-attributes (LineNumberTable, etc.)
     * └──────────────────────────────┘
     */
    data class Code(val codeAttribute: CodeAttribute) : AttributeInfo()

    /**
     * Any attribute we don't specifically parse yet.
     * We store its raw bytes so we can skip over it without crashing.
     */
    data class Unknown(val name: String, val data: ByteArray) : AttributeInfo()
}

/**
 * The parsed contents of a "Code" attribute.
 * This is where the magic happens - the `bytecode` array contains
 * the actual JVM instructions that our interpreter will execute.
 */
data class CodeAttribute(
    val maxStack: Int,
    val maxLocals: Int,
    val bytecode: ByteArray,
    val exceptionTable: List<ExceptionTableEntry>
) {
    override fun toString(): String {
        return "CodeAttribute(maxStack=$maxStack, maxLocals=$maxLocals, " +
               "bytecodeLength=${bytecode.size}, exceptions=${exceptionTable.size})"
    }
}

/**
 * An entry in the exception table of a Code attribute.
 * Defines the range of bytecode instructions covered by a try-catch block.
 */
data class ExceptionTableEntry(
    val startPc: Int,       // Start of try block (bytecode offset)
    val endPc: Int,         // End of try block
    val handlerPc: Int,     // Start of catch block
    val catchType: Int      // Index into constant pool (0 = catch all / finally)
)
