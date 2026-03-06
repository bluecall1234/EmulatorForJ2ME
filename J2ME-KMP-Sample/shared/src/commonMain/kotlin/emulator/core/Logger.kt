package emulator.core

/**
 * Initializes the file logging system on the device.
 * - On Android, this redirects System.out/err to a file in ExternalStorage.
 * - On iOS, this will use CInterop to redirect stdout to a log file.
 * 
 * @param context Provide the appContext (android.content.Context) on Android.
 *                On iOS, this should be null.
 */
expect fun setupFileLogging(context: Any?)
