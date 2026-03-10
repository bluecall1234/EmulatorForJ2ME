package emulator.core.api.javax.microedition.lcdui

import emulator.core.interpreter.ExecutionFrame
import emulator.core.memory.HeapObject

/**
 * Implementation of J2ME's javax.microedition.lcdui.Canvas
 * Canvas is a subclass of Displayable.
 */
object Canvas {
    // Game Action constants
    const val UP = 1
    const val LEFT = 2
    const val RIGHT = 5
    const val DOWN = 6
    const val FIRE = 8
    const val GAME_A = 9
    const val GAME_B = 10
    const val GAME_C = 11
    const val GAME_D = 12

    // Key Code constants (Standard ASCII for numbers)
    const val KEY_NUM0 = 48
    const val KEY_NUM1 = 49
    const val KEY_NUM2 = 50
    const val KEY_NUM3 = 51
    const val KEY_NUM4 = 52
    const val KEY_NUM5 = 53
    const val KEY_NUM6 = 54
    const val KEY_NUM7 = 55
    const val KEY_NUM8 = 56
    const val KEY_NUM9 = 57
    const val KEY_STAR = 42
    const val KEY_POUND = 35

    /**
     * javax.microedition.lcdui.Canvas.repaint()V
     */
    fun repaint(frame: ExecutionFrame) {
        val thisCanvas = frame.pop() as? HeapObject ?: return
        triggerPaint(frame, thisCanvas, 0, 0, 240, 320)
    }

    /**
     * javax.microedition.lcdui.Canvas.repaint(IIII)V
     */
    fun repaintRegion(frame: ExecutionFrame) {
        val h = frame.popInt()
        val w = frame.popInt()
        val y = frame.popInt()
        val x = frame.popInt()
        val thisCanvas = frame.pop() as? HeapObject ?: return
        triggerPaint(frame, thisCanvas, x, y, w, h)
    }

    private fun triggerPaint(frame: ExecutionFrame, thisCanvas: HeapObject, x: Int, y: Int, w: Int, h: Int) {
        val interpreter = frame.interpreter
        
        // 1. Prepare a Graphics object for the paint() call
        val graphics = HeapObject("javax/microedition/lcdui/Graphics")
        graphics.instanceFields["color:I"] = 0xFF000000.toInt()
        graphics.instanceFields["clipX:I"] = x
        graphics.instanceFields["clipY:I"] = y
        graphics.instanceFields["clipW:I"] = w
        graphics.instanceFields["clipH:I"] = h
        graphics.instanceFields["translateX:I"] = 0
        graphics.instanceFields["translateY:I"] = 0
        
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
        thisCanvas?.instanceFields?.put("fullScreen:Z", mode)
        println("[J2ME Canvas] setFullScreenMode($mode) called for $thisCanvas")
    }

    /**
     * javax.microedition.lcdui.Canvas.getGameAction(I)I
     */
    fun getGameAction(frame: ExecutionFrame) {
        val keyCode = frame.popInt()
        frame.pop() // this pointer
        
        val action = when(keyCode) {
            KEY_NUM2 -> UP
            KEY_NUM8 -> DOWN
            KEY_NUM4 -> LEFT
            KEY_NUM6 -> RIGHT
            KEY_NUM5 -> FIRE
            KEY_NUM1 -> GAME_A
            KEY_NUM3 -> GAME_B
            KEY_NUM7 -> GAME_C
            KEY_NUM9 -> GAME_D
            else -> 0
        }
        frame.push(action)
    }

    /**
     * javax.microedition.lcdui.Canvas.getKeyCode(I)I
     */
    fun getKeyCode(frame: ExecutionFrame) {
        val action = frame.popInt()
        frame.pop() // this
        
        val keyCode = when(action) {
            UP -> KEY_NUM2
            DOWN -> KEY_NUM8
            LEFT -> KEY_NUM4
            RIGHT -> KEY_NUM6
            FIRE -> KEY_NUM5
            GAME_A -> KEY_NUM1
            GAME_B -> KEY_NUM3
            GAME_C -> KEY_NUM7
            GAME_D -> KEY_NUM9
            else -> 0
        }
        frame.push(keyCode)
    }

    /**
     * javax.microedition.lcdui.Canvas.getKeyName(I)Ljava/lang/String;
     */
    fun getKeyName(frame: ExecutionFrame) {
        val keyCode = frame.popInt()
        frame.pop() // this
        val name = when(keyCode) {
            KEY_NUM0 -> "0"
            KEY_NUM1 -> "1"
            KEY_NUM2 -> "2"
            KEY_NUM3 -> "3"
            KEY_NUM4 -> "4"
            KEY_NUM5 -> "5"
            KEY_NUM6 -> "6"
            KEY_NUM7 -> "7"
            KEY_NUM8 -> "8"
            KEY_NUM9 -> "9"
            KEY_STAR -> "*"
            KEY_POUND -> "#"
            else -> "Unknown"
        }
        frame.push(name)
    }

    fun hasPointerEvents(frame: ExecutionFrame) {
        frame.pop()
        frame.push(1) // Always true
    }

    fun hasPointerMotionEvents(frame: ExecutionFrame) {
        frame.pop()
        frame.push(1)
    }

    fun hasRepeatEvents(frame: ExecutionFrame) {
        frame.pop()
        frame.push(1)
    }

    fun isDoubleBuffered(frame: ExecutionFrame) {
        frame.pop()
        frame.push(1)
    }

    fun getWidth(frame: ExecutionFrame) {
        frame.pop()
        frame.push(240) // Default width stub
    }

    fun getHeight(frame: ExecutionFrame) {
        val thisCanvas = frame.pop() as? HeapObject
        val isFullscreen = thisCanvas?.instanceFields?.get("fullScreen:Z") as? Boolean ?: false
        frame.push(if (isFullscreen) 320 else 280) // Simple stub
    }
}
