package emulator.core.classfile

import okio.Buffer

/**
 * Parses the remaining sections of a .class file after the Constant Pool:
 * interfaces, fields, methods, and class-level attributes.
 *
 * The parser needs the ConstantPool to resolve attribute names
 * (e.g., to recognize the "Code" attribute by name).
 *
 * WHY SEPARATE FROM ConstantPoolParser?
 * The Constant Pool must be fully parsed before we can read attributes,
 * because attribute names are stored as Utf8 entries in the Constant Pool.
 * So we split parsing into two phases:
 *   Phase A: ConstantPoolParser (no dependencies)
 *   Phase B: ClassFileParser (depends on ConstantPool being ready)
 *
 * Reference: JVM Spec §4.1
 */
class ClassFileParser(private val pool: ConstantPool) {

    /**
     * Parse the interfaces section.
     * Format: 2 bytes (count) + N * 2 bytes (each is an index into constant pool -> ClassRef)
     *
     * @return List of interface names (e.g., ["java/lang/Runnable", "javax/microedition/lcdui/CommandListener"])
     */
    fun parseInterfaces(buffer: Buffer): List<String> {
        val count = buffer.readShort().toInt() and 0xFFFF
        return (0 until count).map {
            val classIndex = buffer.readShort().toInt() and 0xFFFF
            pool.getClassName(classIndex)
        }
    }

    /**
     * Parse the fields or methods section (they share the same binary format).
     * Format: 2 bytes (count) + N * member_info structures
     *
     * Each member_info:
     *   2 bytes access_flags
     *   2 bytes name_index
     *   2 bytes descriptor_index
     *   2 bytes attributes_count
     *   N * attribute_info structures
     */
    fun parseMembers(buffer: Buffer): List<MemberInfo> {
        val count = buffer.readShort().toInt() and 0xFFFF
        return (0 until count).map { parseMember(buffer) }
    }

    private fun parseMember(buffer: Buffer): MemberInfo {
        val accessFlags = buffer.readShort().toInt() and 0xFFFF
        val nameIndex = buffer.readShort().toInt() and 0xFFFF
        val descriptorIndex = buffer.readShort().toInt() and 0xFFFF
        val attributes = parseAttributes(buffer)

        return MemberInfo(accessFlags, nameIndex, descriptorIndex, attributes)
    }

    /**
     * Parse a list of attributes.
     * Format: 2 bytes (count) + N * attribute_info structures
     *
     * Each attribute_info:
     *   2 bytes attribute_name_index (-> Utf8 in constant pool)
     *   4 bytes attribute_length
     *   N bytes data (depends on attribute type)
     *
     * We specifically parse "Code" attributes because they contain bytecode.
     * All other attributes are stored as Unknown with their raw bytes.
     */
    fun parseAttributes(buffer: Buffer): List<AttributeInfo> {
        val count = buffer.readShort().toInt() and 0xFFFF
        return (0 until count).map { parseAttribute(buffer) }
    }

    private fun parseAttribute(buffer: Buffer): AttributeInfo {
        val nameIndex = buffer.readShort().toInt() and 0xFFFF
        val length = buffer.readInt().toLong() and 0xFFFFFFFFL
        val attributeName = pool.getUtf8(nameIndex)

        return when (attributeName) {
            "Code" -> parseCodeAttribute(buffer)
            else -> {
                // Skip unknown attributes by reading their raw bytes
                val data = buffer.readByteArray(length)
                AttributeInfo.Unknown(attributeName, data)
            }
        }
    }

    /**
     * Parse a "Code" attribute - the most important attribute for our interpreter.
     *
     * This is where the actual bytecode lives. For example, a simple method like:
     *   void hello() { System.out.println("Hi"); }
     * Would produce bytecode like:
     *   getstatic #2     // Field java/lang/System.out
     *   ldc #3           // String "Hi"
     *   invokevirtual #4 // Method PrintStream.println
     *   return
     */
    private fun parseCodeAttribute(buffer: Buffer): AttributeInfo.Code {
        val maxStack = buffer.readShort().toInt() and 0xFFFF
        val maxLocals = buffer.readShort().toInt() and 0xFFFF

        // Read the bytecode array
        val codeLength = buffer.readInt().toLong() and 0xFFFFFFFFL
        val bytecode = buffer.readByteArray(codeLength)

        // Read exception table
        val exceptionTableLength = buffer.readShort().toInt() and 0xFFFF
        val exceptionTable = (0 until exceptionTableLength).map {
            ExceptionTableEntry(
                startPc = buffer.readShort().toInt() and 0xFFFF,
                endPc = buffer.readShort().toInt() and 0xFFFF,
                handlerPc = buffer.readShort().toInt() and 0xFFFF,
                catchType = buffer.readShort().toInt() and 0xFFFF
            )
        }

        // Read sub-attributes of the Code attribute (e.g., LineNumberTable)
        // We parse them recursively but they'll be stored as Unknown
        val _subAttributes = parseAttributes(buffer)

        return AttributeInfo.Code(
            CodeAttribute(maxStack, maxLocals, bytecode, exceptionTable)
        )
    }
}
