package emulator.core.classfile

/**
 * Represents a single entry in the Constant Pool of a .class file.
 *
 * The Constant Pool is a table at the beginning of every Java .class file.
 * It stores all string literals, class names, method names, field names,
 * and numeric constants that the bytecode refers to by index.
 *
 * Reference: JVM Spec §4.4
 * https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4
 */
sealed class ConstantPoolEntry {

    /**
     * Tag 1: CONSTANT_Utf8
     * A raw UTF-8 encoded string. This is the most fundamental type.
     * All names (class, method, field) ultimately point to a Utf8 entry.
     */
    data class Utf8(val value: String) : ConstantPoolEntry()

    /**
     * Tag 3: CONSTANT_Integer
     * A 4-byte integer literal used in the code.
     */
    data class IntegerInfo(val value: Int) : ConstantPoolEntry()

    /**
     * Tag 4: CONSTANT_Float
     * A 4-byte float literal.
     */
    data class FloatInfo(val value: Float) : ConstantPoolEntry()

    /**
     * Tag 5: CONSTANT_Long
     * An 8-byte long literal. Takes up TWO slots in the constant pool.
     */
    data class LongInfo(val value: Long) : ConstantPoolEntry()

    /**
     * Tag 6: CONSTANT_Double
     * An 8-byte double literal. Also takes up TWO slots.
     */
    data class DoubleInfo(val value: Double) : ConstantPoolEntry()

    /**
     * Tag 7: CONSTANT_Class
     * Points to a Utf8 entry that contains the fully qualified class name.
     * Example: "java/lang/Object"
     */
    data class ClassRef(val nameIndex: Int) : ConstantPoolEntry()

    /**
     * Tag 8: CONSTANT_String
     * Points to a Utf8 entry that holds the string literal value.
     * Example: "Hello World" in `System.out.println("Hello World")`
     */
    data class StringRef(val stringIndex: Int) : ConstantPoolEntry()

    /**
     * Tag 9: CONSTANT_Fieldref
     * Describes a field: which class it belongs to + its name and type.
     */
    data class FieldRef(val classIndex: Int, val nameAndTypeIndex: Int) : ConstantPoolEntry()

    /**
     * Tag 10: CONSTANT_Methodref
     * Describes a method: which class it belongs to + its name and descriptor.
     * This is what `invokevirtual` and `invokespecial` opcodes reference.
     */
    data class MethodRef(val classIndex: Int, val nameAndTypeIndex: Int) : ConstantPoolEntry()

    /**
     * Tag 11: CONSTANT_InterfaceMethodref
     * Same as MethodRef but for interface methods.
     */
    data class InterfaceMethodRef(val classIndex: Int, val nameAndTypeIndex: Int) : ConstantPoolEntry()

    /**
     * Tag 12: CONSTANT_NameAndType
     * A pair of (name, descriptor). Used by FieldRef and MethodRef.
     * name -> points to Utf8 (e.g., "println")
     * descriptor -> points to Utf8 (e.g., "(Ljava/lang/String;)V")
     */
    data class NameAndType(val nameIndex: Int, val descriptorIndex: Int) : ConstantPoolEntry()

    /**
     * Placeholder for empty slots.
     * Long and Double entries take 2 slots, so the second slot is a Placeholder.
     */
    object Placeholder : ConstantPoolEntry()
}
