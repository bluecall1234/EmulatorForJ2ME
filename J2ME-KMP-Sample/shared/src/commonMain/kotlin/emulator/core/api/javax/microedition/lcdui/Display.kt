package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Display
 */
object Display {

    /**
     * javax.microedition.lcdui.Display.getDisplay(Ljavax/microedition/midlet/MIDlet;)Ljavax/microedition/lcdui/Display;
     */
    fun getDisplay(frame: ExecutionFrame) {
        val midletRef = frame.pop() // The MIDlet instance
        
        // In a real emulator, we might have one Display instance per MIDlet.
        // For now, we instantiate a dummy HeapObject representing the Display
        // so that the game code can call setCurrent on it.
        val displayObj = HeapObject("javax/microedition/lcdui/Display")
        // We could store a reference to the midlet inside it
        displayObj.instanceFields["midlet:Ljavax/microedition/midlet/MIDlet;"] = midletRef
        
        frame.push(displayObj)
        println("[J2ME Display] getDisplay() called for MIDlet $midletRef")
    }

    // Store the currently active Displayable (Canvas or Form)
    var activeDisplayable: HeapObject? = null

    /**
     * javax.microedition.lcdui.Display.setCurrent(Ljavax/microedition/lcdui/Displayable;)V
     */
    fun setCurrent(frame: ExecutionFrame) {
        val displayableRef = frame.pop() as? HeapObject // The Canvas or Form
        val thisDisplay = frame.pop() as? HeapObject // The Display instance
        
        activeDisplayable = displayableRef
        
        // TODO: This is where we tell our CMP / SDL2 window to start rendering
        // this specific Displayable object.
        println("[J2ME Display] setCurrent() called: Set active screen to $displayableRef")
    }
}
