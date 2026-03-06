package javax.microedition.midlet

/**
 * Base class for all J2ME applications.
 * https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/javax/microedition/midlet/MIDlet.html
 */
abstract class MIDlet {
    
    // Called when the game starts running
    @Throws(MIDletStateChangeException::class)
    abstract fun startApp()

    // Called when the game is paused/sent to background
    abstract fun pauseApp()

    // Called when the game is closed
    @Throws(MIDletStateChangeException::class)
    abstract fun destroyApp(unconditional: Boolean)

    // Utility function to notify that the app is ready to easily
    fun notifyDestroyed() {
        println("MIDlet: notifyDestroyed called")
    }
}

class MIDletStateChangeException(message: String) : Exception(message)
