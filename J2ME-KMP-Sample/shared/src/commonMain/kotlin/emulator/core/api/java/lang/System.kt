package emulator.core.api.java.lang

import emulator.core.interpreter.ExecutionFrame

/**
 * Implementation of J2ME's java.lang.System
 */
object System {
    
    /**
     * java.lang.System.currentTimeMillis()J
     * Returns the current time in milliseconds.
     */
    fun currentTimeMillis(frame: ExecutionFrame) {
        val time = getCurrentTimeMillis()
        frame.push(time) 
    }
}
