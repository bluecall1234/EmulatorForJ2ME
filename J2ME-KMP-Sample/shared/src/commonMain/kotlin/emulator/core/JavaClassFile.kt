package emulator.core

import okio.Buffer
import emulator.core.classfile.*

/**
 * Represents a fully parsed Java .class file.
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

    /** Is this class a native shell (no bytecode, just for type info/inheritance)? */
    val isNativeShell: Boolean = bytes.isEmpty()

    /** Resolved class name from constant pool. Example: "demo/game/MyTestGame" */
    val resolvedClassName: String
        get() = if (isNativeShell) className else constantPool.getClassName(thisClassIndex)

    /** Optional override for superclass name (used for native shells) */
    var overriddenSuperClassName: String? = null

    /** Resolved superclass name. Example: "java/lang/Object" */
    val resolvedSuperClassName: String
        get() = when {
            isNativeShell -> {
                overriddenSuperClassName ?: when {
                    className.contains("Canvas") || className.contains("Form") -> "javax/microedition/lcdui/Displayable"
                    className.contains("MIDlet") -> "java/lang/Object"
                    else -> "java/lang/Object"
                }
            }
            superClassIndex != 0 -> constantPool.getClassName(superClassIndex)
            else -> "none"
        }

    init {
        if (!isNativeShell) {
            parse()
        } else {
            // Minimal setup for native shells
            constantPool = ConstantPool(emptyList())
        }
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
