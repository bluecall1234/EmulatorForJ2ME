package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Displayable
 */
object Displayable {

    /**
     * javax.microedition.lcdui.Displayable.addCommand(Ljavax/microedition/lcdui/Command;)V
     */
    fun addCommand(frame: ExecutionFrame) {
        val cmd = frame.pop() as? HeapObject ?: return
        val thisObj = frame.pop() as? HeapObject ?: return
        
        val commands = thisObj.instanceFields.getOrPut("_commands") { mutableListOf<HeapObject>() } as MutableList<HeapObject>
        if (!commands.contains(cmd)) {
            commands.add(cmd)
        }
        println("[J2ME Displayable] addCommand: ${cmd.instanceFields["label"]}")
    }

    /**
     * javax.microedition.lcdui.Displayable.removeCommand(Ljavax/microedition/lcdui/Command;)V
     */
    fun removeCommand(frame: ExecutionFrame) {
        val cmd = frame.pop() as? HeapObject ?: return
        val thisObj = frame.pop() as? HeapObject ?: return
        
        val commands = thisObj.instanceFields["_commands"] as? MutableList<HeapObject>
        commands?.remove(cmd)
        println("[J2ME Displayable] removeCommand: ${cmd.instanceFields["label"]}")
    }

    /**
     * javax.microedition.lcdui.Displayable.setCommandListener(Ljavax/microedition/lcdui/CommandListener;)V
     */
    fun setCommandListener(frame: ExecutionFrame) {
        val listener = frame.pop() as? HeapObject
        val thisObj = frame.pop() as? HeapObject ?: return
        
        thisObj.instanceFields["_commandListener"] = listener
        println("[J2ME Displayable] setCommandListener: $listener")
    }

    /**
     * javax.microedition.lcdui.Displayable.setTitle(Ljava/lang/String;)V
     */
    fun setTitle(frame: ExecutionFrame) {
        val title = frame.pop() as? String ?: ""
        val thisObj = frame.pop() as? HeapObject ?: return
        
        thisObj.instanceFields["title:Ljava/lang/String;"] = title
        println("[J2ME Displayable] setTitle: $title")
    }

    /**
     * javax.microedition.lcdui.Displayable.getTitle()Ljava/lang/String;
     */
    fun getTitle(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject ?: return
        val title = thisObj.instanceFields["title:Ljava/lang/String;"] as? String ?: ""
        frame.push(title)
    }

    /**
     * javax.microedition.lcdui.Displayable.getWidth()I
     */
    fun getWidth(frame: ExecutionFrame) {
        frame.pop() // this
        frame.push(240) // Default width
    }

    /**
     * javax.microedition.lcdui.Displayable.getHeight()I
     */
    fun getHeight(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject ?: return
        // Canvas height might depend on fullscreen mode
        val isFullscreen = thisObj.instanceFields["fullScreen:Z"] as? Boolean ?: false
        frame.push(if (isFullscreen) 320 else 280) // Header/Footer padding stub
    }

    /**
     * javax.microedition.lcdui.Displayable.isShown()Z
     */
    fun isShown(frame: ExecutionFrame) {
        frame.pop() // this
        // For now, if it's the current displayable, it's shown
        frame.push(1) 
    }

    /**
     * javax.microedition.lcdui.Displayable.setTicker(Ljavax/microedition/lcdui/Ticker;)V
     */
    fun setTicker(frame: ExecutionFrame) {
        val ticker = frame.pop() as? HeapObject
        val thisObj = frame.pop() as? HeapObject ?: return
        thisObj.instanceFields["_ticker"] = ticker
    }

    /**
     * javax.microedition.lcdui.Displayable.getTicker()Ljavax/microedition/lcdui/Ticker;
     */
    fun getTicker(frame: ExecutionFrame) {
        val thisObj = frame.pop() as? HeapObject ?: return
        frame.push(thisObj.instanceFields["_ticker"])
    }
}
