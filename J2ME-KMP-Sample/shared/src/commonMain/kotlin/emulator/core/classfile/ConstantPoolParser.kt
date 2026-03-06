package emulator.core.classfile

import okio.Buffer

/**
 * Parses the Constant Pool section of a Java .class file.
 *
 * HOW IT WORKS:
 * A .class file is a binary format. After the magic number (0xCAFEBABE) and
 * version info, the next section is the Constant Pool. It starts with a 2-byte
 * count (number of entries + 1), followed by the entries themselves.
 *
 * Each entry starts with a 1-byte TAG that tells us what kind of data follows.
 * Based on the tag, we read a different number of bytes.
 *
 * WHY WE USE OKIO:
 * Okio is a multiplatform I/O library (works on Android + iOS via KMP).
 * It provides a `Buffer` class that makes reading binary data easy and safe.
 * We use it instead of raw byte arrays to avoid manual index tracking.
 *
 * Reference: JVM Spec §4.4
 */
class ConstantPoolParser {

    companion object {
        // Tag constants from JVM Spec §4.4 Table 4.4-A
        const val TAG_UTF8 = 1
        const val TAG_INTEGER = 3
        const val TAG_FLOAT = 4
        const val TAG_LONG = 5
        const val TAG_DOUBLE = 6
        const val TAG_CLASS = 7
        const val TAG_STRING = 8
        const val TAG_FIELDREF = 9
        const val TAG_METHODREF = 10
        const val TAG_INTERFACE_METHODREF = 11
        const val TAG_NAME_AND_TYPE = 12
    }

    /**
     * Parse the constant pool from a Buffer that is already positioned
     * right after the version bytes (i.e., bytes 8+ of the .class file).
     *
     * @param buffer The binary data buffer, positioned at the constant_pool_count field.
     * @return A list of ConstantPoolEntry. Index 0 is always Placeholder (pool is 1-indexed).
     */
    fun parse(buffer: Buffer): List<ConstantPoolEntry> {
        // Read constant_pool_count (2 bytes, unsigned).
        // The actual number of entries is (count - 1) because the pool is 1-indexed.
        val constantPoolCount = buffer.readShort().toInt() and 0xFFFF

        // Create the list with a placeholder at index 0 (JVM convention: pool is 1-indexed)
        val pool = mutableListOf<ConstantPoolEntry>(ConstantPoolEntry.Placeholder)

        var i = 1
        while (i < constantPoolCount) {
            val tag = buffer.readByte().toInt() and 0xFF

            val entry = when (tag) {
                TAG_UTF8 -> parseUtf8(buffer)
                TAG_INTEGER -> parseInteger(buffer)
                TAG_FLOAT -> parseFloat(buffer)
                TAG_LONG -> parseLong(buffer)
                TAG_DOUBLE -> parseDouble(buffer)
                TAG_CLASS -> parseClass(buffer)
                TAG_STRING -> parseString(buffer)
                TAG_FIELDREF -> parseFieldRef(buffer)
                TAG_METHODREF -> parseMethodRef(buffer)
                TAG_INTERFACE_METHODREF -> parseInterfaceMethodRef(buffer)
                TAG_NAME_AND_TYPE -> parseNameAndType(buffer)
                else -> throw IllegalArgumentException(
                    "Unknown constant pool tag: $tag at index $i"
                )
            }

            pool.add(entry)
            i++

            // Long and Double take TWO slots in the constant pool (JVM spec rule).
            // We must add a Placeholder for the second slot and skip the index.
            if (entry is ConstantPoolEntry.LongInfo || entry is ConstantPoolEntry.DoubleInfo) {
                pool.add(ConstantPoolEntry.Placeholder)
                i++
            }
        }

        return pool
    }

    // --- Individual parsers for each tag type ---

    /**
     * Tag 1: UTF8 string.
     * Format: 2 bytes (length) + N bytes (modified UTF-8 data)
     */
    private fun parseUtf8(buffer: Buffer): ConstantPoolEntry.Utf8 {
        val length = buffer.readShort().toInt() and 0xFFFF
        val bytes = buffer.readByteArray(length.toLong())
        return ConstantPoolEntry.Utf8(bytes.decodeToString())
    }

    /**
     * Tag 3: Integer.
     * Format: 4 bytes (big-endian int)
     */
    private fun parseInteger(buffer: Buffer): ConstantPoolEntry.IntegerInfo {
        return ConstantPoolEntry.IntegerInfo(buffer.readInt())
    }

    /**
     * Tag 4: Float.
     * Format: 4 bytes (IEEE 754 float)
     */
    private fun parseFloat(buffer: Buffer): ConstantPoolEntry.FloatInfo {
        return ConstantPoolEntry.FloatInfo(Float.fromBits(buffer.readInt()))
    }

    /**
     * Tag 5: Long.
     * Format: 8 bytes (big-endian long). Takes 2 constant pool slots.
     */
    private fun parseLong(buffer: Buffer): ConstantPoolEntry.LongInfo {
        return ConstantPoolEntry.LongInfo(buffer.readLong())
    }

    /**
     * Tag 6: Double.
     * Format: 8 bytes (IEEE 754 double). Takes 2 constant pool slots.
     */
    private fun parseDouble(buffer: Buffer): ConstantPoolEntry.DoubleInfo {
        return ConstantPoolEntry.DoubleInfo(Double.fromBits(buffer.readLong()))
    }

    /**
     * Tag 7: Class Reference.
     * Format: 2 bytes (index into the constant pool pointing to a Utf8 entry)
     */
    private fun parseClass(buffer: Buffer): ConstantPoolEntry.ClassRef {
        return ConstantPoolEntry.ClassRef(buffer.readShort().toInt() and 0xFFFF)
    }

    /**
     * Tag 8: String Reference.
     * Format: 2 bytes (index into the constant pool pointing to a Utf8 entry)
     */
    private fun parseString(buffer: Buffer): ConstantPoolEntry.StringRef {
        return ConstantPoolEntry.StringRef(buffer.readShort().toInt() and 0xFFFF)
    }

    /**
     * Tag 9: Field Reference.
     * Format: 2 bytes (class index) + 2 bytes (name_and_type index)
     */
    private fun parseFieldRef(buffer: Buffer): ConstantPoolEntry.FieldRef {
        val classIndex = buffer.readShort().toInt() and 0xFFFF
        val nameAndTypeIndex = buffer.readShort().toInt() and 0xFFFF
        return ConstantPoolEntry.FieldRef(classIndex, nameAndTypeIndex)
    }

    /**
     * Tag 10: Method Reference.
     * Format: 2 bytes (class index) + 2 bytes (name_and_type index)
     */
    private fun parseMethodRef(buffer: Buffer): ConstantPoolEntry.MethodRef {
        val classIndex = buffer.readShort().toInt() and 0xFFFF
        val nameAndTypeIndex = buffer.readShort().toInt() and 0xFFFF
        return ConstantPoolEntry.MethodRef(classIndex, nameAndTypeIndex)
    }

    /**
     * Tag 11: Interface Method Reference.
     * Format: 2 bytes (class index) + 2 bytes (name_and_type index)
     */
    private fun parseInterfaceMethodRef(buffer: Buffer): ConstantPoolEntry.InterfaceMethodRef {
        val classIndex = buffer.readShort().toInt() and 0xFFFF
        val nameAndTypeIndex = buffer.readShort().toInt() and 0xFFFF
        return ConstantPoolEntry.InterfaceMethodRef(classIndex, nameAndTypeIndex)
    }

    /**
     * Tag 12: Name and Type descriptor.
     * Format: 2 bytes (name index) + 2 bytes (descriptor index)
     * Both point to Utf8 entries.
     */
    private fun parseNameAndType(buffer: Buffer): ConstantPoolEntry.NameAndType {
        val nameIndex = buffer.readShort().toInt() and 0xFFFF
        val descriptorIndex = buffer.readShort().toInt() and 0xFFFF
        return ConstantPoolEntry.NameAndType(nameIndex, descriptorIndex)
    }
}

/**
 * Helper class to resolve references in the constant pool.
 * After parsing, many entries just contain index references.
 * This class provides convenient methods to follow those references
 * and get the actual string values.
 */
class ConstantPool(val entries: List<ConstantPoolEntry>) {

    /**
     * Get the UTF-8 string at the given index.
     */
    fun getUtf8(index: Int): String {
        val entry = entries[index]
        if (entry is ConstantPoolEntry.Utf8) {
            return entry.value
        }
        throw IllegalArgumentException("Constant pool entry at index $index is not Utf8, but ${entry::class.simpleName}")
    }

    /**
     * Get the class name at the given index.
     * ClassRef -> points to Utf8 -> return the string.
     */
    fun getClassName(index: Int): String {
        val entry = entries[index]
        if (entry is ConstantPoolEntry.ClassRef) {
            return getUtf8(entry.nameIndex)
        }
        throw IllegalArgumentException("Constant pool entry at index $index is not ClassRef")
    }

    /**
     * Resolve a NameAndType entry into a pair of (name, descriptor).
     */
    fun getNameAndType(index: Int): Pair<String, String> {
        val entry = entries[index]
        if (entry is ConstantPoolEntry.NameAndType) {
            return Pair(getUtf8(entry.nameIndex), getUtf8(entry.descriptorIndex))
        }
        throw IllegalArgumentException("Constant pool entry at index $index is not NameAndType")
    }

    /**
     * Resolve a MethodRef into a triple of (className, methodName, descriptor).
     * Example output: ("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
     */
    fun resolveMethodRef(index: Int): Triple<String, String, String> {
        val entry = entries[index]
        // FIX #5: Handle both MethodRef (tag 10) AND InterfaceMethodRef (tag 11)
        // INVOKEINTERFACE uses InterfaceMethodRef, not MethodRef
        return when (entry) {
            is ConstantPoolEntry.MethodRef -> {
                val className = getClassName(entry.classIndex)
                val (methodName, descriptor) = getNameAndType(entry.nameAndTypeIndex)
                Triple(className, methodName, descriptor)
            }
            is ConstantPoolEntry.InterfaceMethodRef -> {
                val className = getClassName(entry.classIndex)
                val (methodName, descriptor) = getNameAndType(entry.nameAndTypeIndex)
                Triple(className, methodName, descriptor)
            }
            else -> throw IllegalArgumentException(
                "Expected MethodRef or InterfaceMethodRef at index $index, got ${entry::class.simpleName}"
            )
        }
    }

    /**
     * Resolve a FieldRef into a triple of (className, fieldName, descriptor).
     */
    fun resolveFieldRef(index: Int): Triple<String, String, String> {
        val entry = entries[index]
        if (entry is ConstantPoolEntry.FieldRef) {
            val className = getClassName(entry.classIndex)
            val (fieldName, descriptor) = getNameAndType(entry.nameAndTypeIndex)
            return Triple(className, fieldName, descriptor)
        }
        throw IllegalArgumentException("Constant pool entry at index $index is not FieldRef")
    }
}
