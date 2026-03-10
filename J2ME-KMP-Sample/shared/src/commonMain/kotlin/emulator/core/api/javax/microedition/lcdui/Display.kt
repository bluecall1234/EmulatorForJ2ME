package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Display
 */
object Display {
    // Image types Constants
    const val LIST_ELEMENT = 1
    const val CHOICE_GROUP_ELEMENT = 2
    const val ALERT = 3

    // Color Constants
    const val COLOR_BACKGROUND = 0
    const val COLOR_FOREGROUND = 1
    const val COLOR_HIGHLIGHTED_BACKGROUND = 2
    const val COLOR_HIGHLIGHTED_FOREGROUND = 3
    const val COLOR_BORDER = 4
    const val COLOR_HIGHLIGHTED_BORDER = 5

    // Store the currently active Displayable (Canvas or Form)
    var activeDisplayable: HeapObject? = null
    private var displayInstance: HeapObject? = null

    /**
     * javax.microedition.lcdui.Display.getDisplay(Ljavax/microedition/midlet/MIDlet;)Ljavax/microedition/lcdui/Display;
     */
    fun getDisplay(frame: ExecutionFrame) {
        val midletRef = frame.pop() // The MIDlet instance
        
        if (displayInstance == null) {
            displayInstance = HeapObject("javax/microedition/lcdui/Display")
            displayInstance?.instanceFields?.put("midlet:Ljavax/microedition/midlet/MIDlet;", midletRef)
        }
        
        frame.push(displayInstance)
        println("[J2ME Display] getDisplay() called for MIDlet $midletRef")
    }

    /**
     * javax.microedition.lcdui.Display.getCurrent()Ljavax/microedition/lcdui/Displayable;
     */
    fun getCurrent(frame: ExecutionFrame) {
        frame.pop() // this
        frame.push(activeDisplayable)
    }

    /**
     * javax.microedition.lcdui.Display.setCurrent(Ljavax/microedition/lcdui/Displayable;)V
     */
    fun setCurrent(frame: ExecutionFrame) {
        val next = frame.pop() as? HeapObject // The Canvas or Form
        val thisDisplay = frame.pop() as? HeapObject // The Display instance
        
        if (next == null) return
        
        val prev = activeDisplayable
        if (prev == next) return

        val interpreter = frame.interpreter

        // 1. hideNotify() for previous screen
        if (prev != null) {
            try {
                interpreter.executeMethod(prev.className, "hideNotify", "()V", arrayOf(prev))
            } catch (e: Exception) {
                // Ignore if method not implemented in game
            }
        }

        activeDisplayable = next
        println("[J2ME Display] setCurrent(): Set active screen to $next")

        // 2. showNotify() for next screen
        try {
            interpreter.executeMethod(next.className, "showNotify", "()V", arrayOf(next))
        } catch (e: Exception) {
            // Ignore if method not implemented in game
        }

        // 3. sizeChanged() - notify layout change
        try {
            interpreter.executeMethod(next.className, "sizeChanged", "(II)V", arrayOf(next, 240, 320))
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * javax.microedition.lcdui.Display.isColor()Z
     */
    fun isColor(frame: ExecutionFrame) {
        frame.pop() // this
        frame.push(1) // true
    }

    /**
     * javax.microedition.lcdui.Display.numColors()I
     */
    fun numColors(frame: ExecutionFrame) {
        frame.pop() // this
        frame.push(16777216) // 24-bit color
    }

    /**
     * javax.microedition.lcdui.Display.numAlphaLevels()I
     */
    fun numAlphaLevels(frame: ExecutionFrame) {
        frame.pop() // this
        frame.push(256) // Full alpha support
    }

    /**
     * javax.microedition.lcdui.Display.getColor(I)I
     */
    fun getColor(frame: ExecutionFrame) {
        val colorSpecifier = frame.popInt()
        frame.pop() // this
        
        val color = when(colorSpecifier) {
            COLOR_BACKGROUND -> 0xFFFFFF
            COLOR_FOREGROUND -> 0x000000
            COLOR_HIGHLIGHTED_BACKGROUND -> 0x000080 // Navy
            COLOR_HIGHLIGHTED_FOREGROUND -> 0xFFFFFF
            COLOR_BORDER -> 0x808080
            COLOR_HIGHLIGHTED_BORDER -> 0x000000
            else -> 0x000000
        }
        frame.push(color)
    }

    /**
     * javax.microedition.lcdui.Display.vibrate(I)Z
     */
    fun vibrate(frame: ExecutionFrame) {
        val duration = frame.popInt()
        frame.pop() // this
        println("[J2ME Display] vibrate($duration) - STUB")
        frame.push(1) // success
    }

    /**
     * javax.microedition.lcdui.Display.flashBacklight(I)Z
     */
    fun flashBacklight(frame: ExecutionFrame) {
        val duration = frame.popInt()
        frame.pop() // this
        println("[J2ME Display] flashBacklight($duration) - STUB")
        frame.push(1) // success
    }

    fun getBestImageHeight(frame: ExecutionFrame) {
        val type = frame.popInt()
        frame.pop() // this
        frame.push(320)
    }

    fun getBestImageWidth(frame: ExecutionFrame) {
        val type = frame.popInt()
        frame.pop() // this
        frame.push(240)
    }

    fun callSerially(frame: ExecutionFrame) {
        val runnable = frame.pop() as? HeapObject
        frame.pop() // this
        if (runnable != null) {
            // Simple immediate execution for now
            frame.interpreter.executeMethod(runnable.className, "run", "()V", arrayOf(runnable))
        }
    }
}
