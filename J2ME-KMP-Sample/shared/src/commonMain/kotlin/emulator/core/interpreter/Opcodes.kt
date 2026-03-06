package emulator.core.interpreter

/**
 * Defines all JVM opcode constants used by the Execution Engine.
 *
 * Each opcode is a single byte (0x00-0xFF) that tells the JVM what to do.
 * Think of these as "machine instructions" for the Java Virtual Machine.
 *
 * We only implement the opcodes commonly used in J2ME applications.
 * J2ME uses Java 1.3/1.4 bytecode, so newer opcodes (invokedynamic, etc.)
 * are not needed.
 *
 * Reference: JVM Spec §6.5 (Instructions)
 * https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html
 */
object Opcodes {

    // === Constants ===
    const val NOP          = 0x00  // Do nothing
    const val ACONST_NULL  = 0x01  // Push null onto stack
    const val ICONST_M1    = 0x02  // Push int -1
    const val ICONST_0     = 0x03  // Push int 0
    const val ICONST_1     = 0x04  // Push int 1
    const val ICONST_2     = 0x05  // Push int 2
    const val ICONST_3     = 0x06  // Push int 3
    const val ICONST_4     = 0x07  // Push int 4
    const val ICONST_5     = 0x08  // Push int 5
    const val LCONST_0     = 0x09  // Push long 0
    const val LCONST_1     = 0x0A  // Push long 1
    const val FCONST_0     = 0x0B  // Push float 0.0
    const val FCONST_1     = 0x0C  // Push float 1.0
    const val FCONST_2     = 0x0D  // Push float 2.0
    const val BIPUSH       = 0x10  // Push byte as int (1 operand byte)
    const val SIPUSH       = 0x11  // Push short as int (2 operand bytes)
    const val LDC          = 0x12  // Push item from constant pool (1 byte index)
    const val LDC_W        = 0x13  // Push item from constant pool (2 byte index)
    const val LDC2_W       = 0x14  // Push long/double from constant pool

    // === Load from local variables ===
    const val ILOAD        = 0x15  // Load int from local var (1 byte index)
    const val LLOAD        = 0x16  // Load long
    const val FLOAD        = 0x17  // Load float
    const val ALOAD        = 0x19  // Load reference
    const val ILOAD_0      = 0x1A  // Load int from local var 0
    const val ILOAD_1      = 0x1B  // Load int from local var 1
    const val ILOAD_2      = 0x1C  // Load int from local var 2
    const val ILOAD_3      = 0x1D  // Load int from local var 3
    const val LLOAD_0      = 0x1E  // Load long from local var 0
    const val LLOAD_1      = 0x1F  // Load long from local var 1
    const val ALOAD_0      = 0x2A  // Load reference from local var 0 (usually "this")
    const val ALOAD_1      = 0x2B  // Load reference from local var 1
    const val ALOAD_2      = 0x2C  // Load reference from local var 2
    const val ALOAD_3      = 0x2D  // Load reference from local var 3

    // === Store to local variables ===
    const val ISTORE       = 0x36  // Store int to local var (1 byte index)
    const val LSTORE       = 0x37  // Store long
    const val FSTORE       = 0x38  // Store float
    const val ASTORE       = 0x3A  // Store reference
    const val ISTORE_0     = 0x3B  // Store int to local var 0
    const val ISTORE_1     = 0x3C  // Store int to local var 1
    const val ISTORE_2     = 0x3D  // Store int to local var 2
    const val ISTORE_3     = 0x3E  // Store int to local var 3
    const val ASTORE_0     = 0x4B  // Store reference to local var 0
    const val ASTORE_1     = 0x4C  // Store reference to local var 1
    const val ASTORE_2     = 0x4D  // Store reference to local var 2
    const val ASTORE_3     = 0x4E  // Store reference to local var 3

    // === Stack manipulation ===
    const val POP          = 0x57  // Pop top value from stack
    const val POP2         = 0x58  // Pop top 2 values
    const val DUP          = 0x59  // Duplicate top value
    const val DUP_X1       = 0x5A  // Duplicate top value, insert 2 deep
    const val SWAP         = 0x5F  // Swap top 2 values

    // === Integer arithmetic ===
    const val IADD         = 0x60  // int add
    const val LADD         = 0x61  // long add
    const val ISUB         = 0x64  // int subtract
    const val LSUB         = 0x65  // long subtract
    const val IMUL         = 0x68  // int multiply
    const val LMUL         = 0x69  // long multiply
    const val IDIV         = 0x6C  // int divide
    const val IREM         = 0x70  // int remainder (modulo)
    const val INEG         = 0x74  // int negate
    const val ISHL         = 0x78  // int shift left
    const val ISHR         = 0x7A  // int shift right (signed)
    const val IAND         = 0x7E  // int bitwise AND
    const val IOR          = 0x80  // int bitwise OR
    const val IXOR         = 0x82  // int bitwise XOR

    // === Type conversion ===
    const val I2L          = 0x85  // int to long
    const val I2F          = 0x86  // int to float
    const val I2B          = 0x91  // int to byte
    const val I2C          = 0x92  // int to char
    const val I2S          = 0x93  // int to short

    // === Comparison ===
    const val LCMP         = 0x94  // long compare
    const val IFEQ         = 0x99  // Branch if int == 0
    const val IFNE         = 0x9A  // Branch if int != 0
    const val IFLT         = 0x9B  // Branch if int < 0
    const val IFGE         = 0x9C  // Branch if int >= 0
    const val IFGT         = 0x9D  // Branch if int > 0
    const val IFLE         = 0x9E  // Branch if int <= 0
    const val IF_ICMPEQ    = 0x9F  // Branch if int1 == int2
    const val IF_ICMPNE    = 0xA0  // Branch if int1 != int2
    const val IF_ICMPLT    = 0xA1  // Branch if int1 < int2
    const val IF_ICMPGE    = 0xA2  // Branch if int1 >= int2
    const val IF_ICMPGT    = 0xA3  // Branch if int1 > int2
    const val IF_ICMPLE    = 0xA4  // Branch if int1 <= int2
    const val IF_ACMPEQ    = 0xA5  // Branch if ref1 == ref2
    const val IF_ACMPNE    = 0xA6  // Branch if ref1 != ref2

    // === Control flow ===
    const val GOTO         = 0xA7  // Branch always (2-byte offset)
    const val IRETURN      = 0xAC  // Return int from method
    const val LRETURN      = 0xAD  // Return long
    const val FRETURN      = 0xAE  // Return float
    const val ARETURN      = 0xB0  // Return reference
    const val RETURN       = 0xB1  // Return void

    // === Object/Field/Method access ===
    const val GETSTATIC    = 0xB2  // Get static field value
    const val PUTSTATIC    = 0xB3  // Set static field value
    const val GETFIELD     = 0xB4  // Get instance field value
    const val PUTFIELD     = 0xB5  // Set instance field value
    const val INVOKEVIRTUAL   = 0xB6  // Call instance method (virtual dispatch)
    const val INVOKESPECIAL   = 0xB7  // Call constructor or super method
    const val INVOKESTATIC    = 0xB8  // Call static method
    const val INVOKEINTERFACE = 0xB9  // Call interface method

    // === Object creation ===
    const val NEW          = 0xBB  // Create new object
    const val NEWARRAY     = 0xBC  // Create new primitive array
    const val ANEWARRAY    = 0xBD  // Create new reference array
    const val ARRAYLENGTH  = 0xBE  // Get array length
    const val ATHROW       = 0xBF  // Throw exception
    const val CHECKCAST    = 0xC0  // Check if object is of given type
    const val INSTANCEOF   = 0xC1  // Check if object is instance of type

    // === Array access ===
    const val IALOAD       = 0x2E  // Load int from array
    const val AALOAD       = 0x32  // Load reference from array
    const val BALOAD       = 0x33  // Load byte from array
    const val CALOAD       = 0x34  // Load char from array
    const val IASTORE      = 0x4F  // Store int to array
    const val AASTORE      = 0x53  // Store reference to array
    const val BASTORE      = 0x54  // Store byte to array
    const val CASTORE      = 0x55  // Store char to array

    // === Null check ===
    const val IFNULL       = 0xC6  // Branch if reference is null
    const val IFNONNULL    = 0xC7  // Branch if reference is not null

    // === Increment ===
    const val IINC         = 0x84  // Increment local variable by constant

    /**
     * Get a human-readable name for an opcode (for debugging).
     */
    fun nameOf(opcode: Int): String {
        return when (opcode) {
            NOP -> "nop"; ACONST_NULL -> "aconst_null"
            ICONST_M1 -> "iconst_m1"; ICONST_0 -> "iconst_0"
            ICONST_1 -> "iconst_1"; ICONST_2 -> "iconst_2"
            ICONST_3 -> "iconst_3"; ICONST_4 -> "iconst_4"
            ICONST_5 -> "iconst_5"
            BIPUSH -> "bipush"; SIPUSH -> "sipush"
            LDC -> "ldc"; LDC_W -> "ldc_w"; LDC2_W -> "ldc2_w"
            ILOAD -> "iload"; ALOAD -> "aload"
            ILOAD_0 -> "iload_0"; ILOAD_1 -> "iload_1"
            ILOAD_2 -> "iload_2"; ILOAD_3 -> "iload_3"
            ALOAD_0 -> "aload_0"; ALOAD_1 -> "aload_1"
            ALOAD_2 -> "aload_2"; ALOAD_3 -> "aload_3"
            ISTORE -> "istore"; ASTORE -> "astore"
            ISTORE_0 -> "istore_0"; ISTORE_1 -> "istore_1"
            ISTORE_2 -> "istore_2"; ISTORE_3 -> "istore_3"
            ASTORE_0 -> "astore_0"; ASTORE_1 -> "astore_1"
            POP -> "pop"; DUP -> "dup"; SWAP -> "swap"
            IADD -> "iadd"; ISUB -> "isub"; IMUL -> "imul"
            IDIV -> "idiv"; IREM -> "irem"; INEG -> "ineg"
            IAND -> "iand"; IOR -> "ior"; IXOR -> "ixor"
            IFEQ -> "ifeq"; IFNE -> "ifne"
            IFLT -> "iflt"; IFGE -> "ifge"
            IFGT -> "ifgt"; IFLE -> "ifle"
            IF_ICMPEQ -> "if_icmpeq"; IF_ICMPNE -> "if_icmpne"
            IF_ICMPLT -> "if_icmplt"; IF_ICMPGE -> "if_icmpge"
            GOTO -> "goto"
            IRETURN -> "ireturn"; ARETURN -> "areturn"; RETURN -> "return"
            GETSTATIC -> "getstatic"; PUTSTATIC -> "putstatic"
            GETFIELD -> "getfield"; PUTFIELD -> "putfield"
            INVOKEVIRTUAL -> "invokevirtual"
            INVOKESPECIAL -> "invokespecial"
            INVOKESTATIC -> "invokestatic"
            INVOKEINTERFACE -> "invokeinterface"
            NEW -> "new"; NEWARRAY -> "newarray"; ANEWARRAY -> "anewarray"
            ARRAYLENGTH -> "arraylength"; ATHROW -> "athrow"
            CHECKCAST -> "checkcast"; INSTANCEOF -> "instanceof"
            IINC -> "iinc"
            IFNULL -> "ifnull"; IFNONNULL -> "ifnonnull"
            else -> "unknown_0x${opcode.toString(16)}"
        }
    }
}
