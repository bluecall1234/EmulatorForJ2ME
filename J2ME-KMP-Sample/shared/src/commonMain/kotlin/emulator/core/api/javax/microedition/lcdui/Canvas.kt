package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Canvas
 * Canvas is a subclass of Displayable.
 */
object Canvas {

    /**
     * javax.microedition.lcdui.Canvas.repaint()V
     */
    fun repaint(frame: ExecutionFrame) {
        val thisCanvas = frame.pop() as? HeapObject
        
        // Trigger a render cycle on SDL2/Compose side.
        NativeGraphicsBridge.presentScreen()
        // println("[J2ME Canvas] repaint() requested for $thisCanvas")
    }

    /**
     * javax.microedition.lcdui.Canvas.setFullScreenMode(Z)V
     */
    fun setFullScreenMode(frame: ExecutionFrame) {
        val mode = frame.popInt() != 0 // boolean
        val thisCanvas = frame.pop() as? HeapObject
        
        println("[J2ME Canvas] setFullScreenMode($mode) called for $thisCanvas")
    }
}
