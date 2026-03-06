package emulator.core.api.java.io

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's java.io.PrintStream (e.g., System.out)
 */
object PrintStream {

    /**
     * java.io.PrintStream.println(Ljava/lang/String;)V
     */
    fun println(frame: ExecutionFrame) {
        val strRef = frame.pop() // The String argument
        val thisRef = frame.pop() // The PrintStream instance 'this'

        // Extract string value if it's a HeapObject our emulator allocated,
        // or just print the raw reference for now.
        if (strRef is HeapObject && strRef.className == "java/lang/String") {
            val value = strRef.instanceFields["value:[C"]
            println("[J2ME System.out] String: $value")
        } else {
            println("[J2ME System.out] $strRef")
        }
    }
}
