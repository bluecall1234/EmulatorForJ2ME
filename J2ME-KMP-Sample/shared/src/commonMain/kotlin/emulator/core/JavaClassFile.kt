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
 * The implementation is platform-specific (Android uses java.util.zip.ZipFile, etc.)
 */
expect class JarLoader() {
    /**
     * @param filePath Absolute path to the JAR file on disk
     * @param className Fully qualified class name to extract (e.g. "demo.game.MyTestGame")
     */
    fun loadClassFromJar(filePath: String, className: String): JavaClassFile
}
