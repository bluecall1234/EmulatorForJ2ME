package demo.game

import javax.microedition.midlet.MIDlet
import javax.microedition.lcdui.Canvas
import javax.microedition.lcdui.Graphics

/**
 * A simple MIDlet to test the KMP Emulator system
 */
class MyTestGame : MIDlet() {
    
    override fun startApp() {
        println("MyTestGame: startApp() is running!")
        val canvas = SimpleCanvas()
        // Simulate rendering the game
        canvas.repaint()
    }

    override fun pauseApp() {
        println("MyTestGame: pauseApp() is running!")
    }

    override fun destroyApp(unconditional: Boolean) {
        println("MyTestGame: destroyApp($unconditional) is running!")
        notifyDestroyed()
    }
}

/**
 * A simple Canvas
 */
class SimpleCanvas : Canvas() {
    override fun paint(g: Graphics) {
        println("SimpleCanvas: Drawing game interface...")
        g.setColor(255, 0, 0) // Red color
        g.fillRect(10, 10, 100, 100)
    }
}
