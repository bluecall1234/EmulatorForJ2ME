package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Graphics
 */
object Graphics {

    /**
     * javax.microedition.lcdui.Graphics.setColor(I)V
     * Sets the current color from a packed ARGB integer.
     */
    fun setColor(frame: ExecutionFrame) {
        val rgb = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        thisGraphics?.instanceFields?.put("color:I", rgb)
        println("[J2ME Graphics] setColor(0x${rgb.toString(16).padStart(8, '0')})")
    }

    /**
     * javax.microedition.lcdui.Graphics.setColor(III)V
     * Overload: setColor(int red, int green, int blue)
     */
    fun setColorRGB(frame: ExecutionFrame) {
        val b = frame.popInt()
        val g = frame.popInt()
        val r = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject
        val packed = (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
        thisGraphics?.instanceFields?.put("color:I", packed)
        println("[J2ME Graphics] setColor($r, $g, $b) -> 0x${packed.toString(16).padStart(8, '0')}")
    }

    /**
     * javax.microedition.lcdui.Graphics.fillRect(IIII)V
     */
    fun fillRect(frame: ExecutionFrame) {
        val h = frame.popInt()
        val w = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val thisGraphics = frame.pop() as? HeapObject

        val color = thisGraphics?.instanceFields?.get("color:I") as? Int ?: 0

        NativeGraphicsBridge.fillRect(x, y, w, h, color)
        println("[J2ME Graphics] fillRect(x=$x, y=$y, w=$w, h=$h) color=0x${color.toString(16)}")
    }

    /**
     * javax.microedition.lcdui.Graphics.drawImage(Ljavax/microedition/lcdui/Image;III)V
     */
    fun drawImage(frame: ExecutionFrame) {
        val anchor = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val imageObj = frame.pop() as? HeapObject
        val thisGraphics = frame.pop() as? HeapObject

        // TODO (Phase 4): Bridge this call to SDL2 texture drawing
        println("[J2ME Graphics] drawImage(img=$imageObj, x=$x, y=$y, anchor=$anchor)")
    }
}

