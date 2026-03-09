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
        val thisCanvas = frame.pop() as? HeapObject ?: return
        val interpreter = frame.interpreter
        
        // 1. Prepare a Graphics object for the paint() call
        val graphics = HeapObject("javax/microedition/lcdui/Graphics")
        graphics.instanceFields["color:I"] = 0xFF000000.toInt()
        graphics.instanceFields["clipX:I"] = 0
        graphics.instanceFields["clipY:I"] = 0
        graphics.instanceFields["clipW:I"] = 240 // Default width
        graphics.instanceFields["clipH:I"] = 320 // Default height
        
        // 2. Execute the game's paint(Graphics g) method
        try {
            interpreter.executeMethod(thisCanvas.className, "paint", "(Ljavax/microedition/lcdui/Graphics;)V", arrayOf(thisCanvas, graphics))
        } catch (e: Exception) {
            println("[J2ME Canvas] Error during paint(): ${e.message}")
        }

        // 3. Present the drawn buffer
        NativeGraphicsBridge.presentScreen()
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
