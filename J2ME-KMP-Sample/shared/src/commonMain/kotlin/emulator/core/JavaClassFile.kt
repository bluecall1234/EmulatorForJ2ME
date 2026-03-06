package emulator.core

import okio.Buffer
import emulator.core.classfile.*

/**
 * Represents a fully parsed Java .class file.
 *
 * A .class file has the following binary structure:
 * ┌─────────────────────────┐
 * │ Magic Number (4 bytes)  │  0xCAFEBABE
 * │ Minor Version (2 bytes) │
 * │ Major Version (2 bytes) │
 * │ Constant Pool Count (2) │
 * │ Constant Pool (variable)│  <-- The "dictionary" of the class
 * │ Access Flags (2 bytes)  │
 * │ This Class (2 bytes)    │  <-- Index into constant pool
 * │ Super Class (2 bytes)   │
 * │ Interfaces (variable)   │
 * │ Fields (variable)       │
 * │ Methods (variable)      │  <-- Contains the actual bytecode
 * │ Attributes (variable)   │
 * └─────────────────────────┘
 *
 * Reference: JVM Spec §4.1
 */
class JavaClassFile(val className: String, val bytes: ByteArray) {

    var majorVersion: Int = 0
        private set
    var minorVersion: Int = 0
        private set
    var accessFlags: Int = 0
        private set
    var thisClassIndex: Int = 0
        private set
    var superClassIndex: Int = 0
        private set

    lateinit var constantPool: ConstantPool
        private set
    var interfaces: List<String> = emptyList()
        private set
    var fields: List<MemberInfo> = emptyList()
        private set
    var methods: List<MemberInfo> = emptyList()
        private set
    var classAttributes: List<AttributeInfo> = emptyList()
        private set

    // Memory for class-level (static) variables
    val staticFields = mutableMapOf<String, Any?>()

    /** Resolved class name from constant pool. Example: "demo/game/MyTestGame" */
    val resolvedClassName: String
        get() = constantPool.getClassName(thisClassIndex)

    /** Resolved superclass name. Example: "java/lang/Object" */
    val resolvedSuperClassName: String
        get() = if (superClassIndex != 0) constantPool.getClassName(superClassIndex) else "none"

    init {
        parse()
    }

    private fun parse() {
        val buffer = Buffer().write(bytes)

        // 1. Magic Number (4 bytes)
        val magic = buffer.readInt().toUInt()
        if (magic != 0xCAFEBABEu) {
            throw IllegalArgumentException("Not a valid Java class file (Magic: 0x${magic.toString(16)})")
        }

        // 2. Version
        minorVersion = buffer.readShort().toInt() and 0xFFFF
        majorVersion = buffer.readShort().toInt() and 0xFFFF

        // 3. Constant Pool
        val cpParser = ConstantPoolParser()
        constantPool = ConstantPool(cpParser.parse(buffer))

        // 4. Access Flags
        accessFlags = buffer.readShort().toInt() and 0xFFFF

        // 5. This Class & Super Class
        thisClassIndex = buffer.readShort().toInt() and 0xFFFF
        superClassIndex = buffer.readShort().toInt() and 0xFFFF

        // 6. Interfaces, Fields, Methods, Attributes
        val cfParser = ClassFileParser(constantPool)
        interfaces = cfParser.parseInterfaces(buffer)
        fields = cfParser.parseMembers(buffer)
        methods = cfParser.parseMembers(buffer)
        classAttributes = cfParser.parseAttributes(buffer)

        // Log summary
        println("[ClassFile] Parsed: $resolvedClassName extends $resolvedSuperClassName")
        println("[ClassFile]   Java ${majorVersion}.${minorVersion}, " +
                "${constantPool.entries.size} pool entries")
        println("[ClassFile]   ${interfaces.size} interfaces, " +
                "${fields.size} fields, ${methods.size} methods")

        // Log method details
        for (method in methods) {
            val name = method.getName(constantPool)
            val desc = method.getDescriptor(constantPool)
            val code = method.getCodeAttribute()
            if (code != null) {
                println("[ClassFile]   Method: $name$desc -> ${code.bytecode.size} bytes of bytecode")
            } else {
                println("[ClassFile]   Method: $name$desc -> abstract/native (no bytecode)")
            }
        }
    }

    /**
     * Find a method by name and descriptor.
     * Returns null if not found.
     */
    fun findMethod(methodName: String, descriptor: String? = null): MemberInfo? {
        return methods.find { member ->
            val name = member.getName(constantPool)
            val desc = member.getDescriptor(constantPool)
            name == methodName && (descriptor == null || desc == descriptor)
        }
    }
}

/**
 * Utility for loading .class files from JAR archives.
 * Currently generates mock data for testing with a real method containing bytecode.
 */
class JarLoader {
    fun loadClassFromJar(packageName: String, className: String): JavaClassFile {
        val buffer = Buffer()
        val fullName = if (packageName.isNotEmpty()) "$packageName/$className" else className

        // === HEADER ===
        buffer.writeInt(0xCAFEBABE.toInt()) // Magic
        buffer.writeShort(0)   // minor version
        buffer.writeShort(52)  // major version (Java 8)

        // === CONSTANT POOL (19 entries, count = 20) ===
        buffer.writeShort(20)

        // #1: Utf8 - class name
        writeUtf8Entry(buffer, fullName)
        // #2: ClassRef -> #1
        buffer.writeByte(7); buffer.writeShort(1)
        // #3: Utf8 - superclass name
        writeUtf8Entry(buffer, "java/lang/Object")
        // #4: ClassRef -> #3
        buffer.writeByte(7); buffer.writeShort(3)
        // #5: Utf8 - method name "startApp"
        writeUtf8Entry(buffer, "startApp")
        // #6: Utf8 - method descriptor "()V" (no args, returns void)
        writeUtf8Entry(buffer, "()V")
        // #7: Utf8 - "Code" attribute name
        writeUtf8Entry(buffer, "Code")
        // #8: Utf8 - method name "<init>"
        writeUtf8Entry(buffer, "<init>")
        // #9: Utf8 - field name "x"
        writeUtf8Entry(buffer, "x")
        // #10: Utf8 - field descriptor "I"
        writeUtf8Entry(buffer, "I")
        // #11: Utf8 - method name "pauseApp"
        writeUtf8Entry(buffer, "pauseApp")
        // #12: Utf8 - method name "destroyApp"
        writeUtf8Entry(buffer, "destroyApp")
        // #13: Utf8 - method descriptor "(Z)V"
        writeUtf8Entry(buffer, "(Z)V")
        
        // --- Added for Native Bridge Testing ---
        // #14: Utf8 - "java/lang/System"
        writeUtf8Entry(buffer, "java/lang/System")
        // #15: ClassRef -> #14
        buffer.writeByte(7); buffer.writeShort(14)
        // #16: Utf8 - "currentTimeMillis"
        writeUtf8Entry(buffer, "currentTimeMillis")
        // #17: Utf8 - "()J"
        writeUtf8Entry(buffer, "()J")
        // #18: NameAndType -> #16, #17
        buffer.writeByte(12); buffer.writeShort(16); buffer.writeShort(17)
        // #19: MethodRef -> Class(#15), NameAndType(#18)
        buffer.writeByte(10); buffer.writeShort(15); buffer.writeShort(18)

        // === ACCESS FLAGS, THIS, SUPER ===
        buffer.writeShort(0x0001) // public
        buffer.writeShort(2)     // this_class -> #2
        buffer.writeShort(4)     // super_class -> #4

        // === INTERFACES (0) ===
        buffer.writeShort(0)

        // === FIELDS (1 field: "int x") ===
        buffer.writeShort(1)
        // Field: private int x
        buffer.writeShort(0x0002) // ACC_PRIVATE
        buffer.writeShort(9)     // name -> #9 "x"
        buffer.writeShort(10)    // descriptor -> #10 "I"
        buffer.writeShort(0)     // no attributes

        // === METHODS (3 methods) ===
        buffer.writeShort(3)

        // Method 1: <init>()V (constructor)
        writeMockMethod(buffer, nameIndex = 8, descriptorIndex = 6,
            codeAttrNameIndex = 7,
            maxStack = 2, maxLocals = 2,
            bytecode = byteArrayOf(
                0x2A,                   // aload_0 (push "this")
                0xB1.toByte()           // return
            )
        )

        // Method 2: startApp()V
        writeMockMethod(buffer, nameIndex = 5, descriptorIndex = 6,
            codeAttrNameIndex = 7,
            maxStack = 4, maxLocals = 4,
            // Bytecode: invokeStatic System.currentTimeMillis() -> lstore_1 -> return
            // INVOKESTATIC is 0xB8 followed by 2-byte index. Index to MethodRef is #19 (0x0013).
            // LSTORE_1 is 0x3F.
            bytecode = byteArrayOf(
                0xB8.toByte(), 0x00, 0x13, // invokestatic #19 (java/lang/System.currentTimeMillis:()J)
                0x3F,                      // lstore_1
                0xB1.toByte()              // return
            )
        )

        // Method 3: pauseApp()V
        writeMockMethod(buffer, nameIndex = 11, descriptorIndex = 6,
            codeAttrNameIndex = 7,
            maxStack = 1, maxLocals = 1,
            bytecode = byteArrayOf(
                0xB1.toByte()           // return (empty method)
            )
        )

        // === CLASS ATTRIBUTES (0) ===
        buffer.writeShort(0)

        return JavaClassFile(fullName, buffer.readByteArray())
    }

    private fun writeUtf8Entry(buffer: Buffer, value: String) {
        buffer.writeByte(1) // TAG_UTF8
        val bytes = value.encodeToByteArray()
        buffer.writeShort(bytes.size)
        buffer.write(bytes)
    }

    private fun writeMockMethod(
        buffer: Buffer,
        nameIndex: Int,
        descriptorIndex: Int,
        codeAttrNameIndex: Int,
        maxStack: Int = 2,
        maxLocals: Int = 2,
        bytecode: ByteArray
    ) {
        buffer.writeShort(0x0001) // ACC_PUBLIC
        buffer.writeShort(nameIndex)
        buffer.writeShort(descriptorIndex)
        buffer.writeShort(1) // 1 attribute (Code)

        // Code attribute
        buffer.writeShort(codeAttrNameIndex) // attribute_name -> "Code"
        // attribute_length = 2(maxStack) + 2(maxLocals) + 4(codeLen) + bytecode.size
        //                  + 2(exceptionTableLen) + 2(subAttributesCount)
        val attrLength = 2 + 2 + 4 + bytecode.size + 2 + 2
        buffer.writeInt(attrLength)
        buffer.writeShort(maxStack)              // max_stack
        buffer.writeShort(maxLocals)              // max_locals
        buffer.writeInt(bytecode.size)    // code_length
        buffer.write(bytecode)            // the bytecode
        buffer.writeShort(0)              // exception_table_length (0)
        buffer.writeShort(0)              // sub-attributes count (0)
    }
}
