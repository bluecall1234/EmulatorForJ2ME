# Analysis: Requirements to Run "Bubble Bash Mania" (GloftBBPM)

## Root Cause Analysis & Missing Features

Based on the analysis of `BubbleBashMania_FULL_AndroidGeneric_1773_84163_103_UA004_AYCE.jad` and its corresponding `.jar` execution logs, here is what our J2ME Emulator is currently missing to fully load and render this game:

1. **JAD File Support & Metadata Parsing**
   - **Current State**: The `JarLoader` only expects `.jar` files and reads the internal `META-INF/MANIFEST.MF`.
   - **Missing**: If you select a `.jad` file using the File Picker, the app won't know how to handle it. The `.jad` file contains valuable properties like `MIDlet-Touch-Support: true` and `MIDlet-Jar-URL` (which points to the actual `.jar`). We need to parse the `.jad`, merge its properties with the `.jar` manifest, and load the game based on these flags.

2. **Threading & Game Loop**
   - **Current State**: `GloftBBPM` successfully initialized its singletons and called `startApp`. Inside `startApp`, it created a `java.lang.Thread` and called `start()`. Our `NativeMethodBridge` currently stubs `Thread.start()` with a warning.
   - **Missing**: We need to implement `java.lang.Thread.start()` natively. This entails launching a background Coroutine or native Thread that invokes the `.run()` method of the `Runnable` target instance through our `ExecutionEngine`.

3. **Graphics & Canvas Rendering**
   - **Current State**: We have a basic `NativeGraphicsBridge` connected to SDL2/SurfaceView, but the J2ME `Graphics` API (`javax.microedition.lcdui.Graphics`) methods are not yet bridged.
   - **Missing**: Once the thread starts, the game loop will rapidly call `repaint()`, `drawImage()`, `setClip()`, `fillRect()`, etc. We must implement these native methods in `NativeMethodBridge` and route them to our `NativeGraphicsBridge`.

4. **Touch & Key Input Handling**
   - **Current State**: The Compose UI detects touch, but nothing is forwarded to the game.
   - **Missing**: The game JAD specifies `MIDlet-Touch-Support: true`. We must route Compose pointer events to invoke the `pointerPressed(x, y)`, `pointerDragged(x, y)`, and `pointerReleased(x, y)` methods on the active `Canvas` class inside the Bytecode Engine.

5. **Misc Standard APIs**
   - The game logs show missing implementations for `java.util.Random`, `java.lang.System.currentTimeMillis()`, and likely relies on `java.lang.Runtime` memory methods.

## Proposed Plan

We should divide the implementation into the following logical phases:

### Phase 5.2: JAD Support & Native Threading
- Update `GameLibraryScreen` & `File Picker` to accept `.jad` files. 
- Create a `JadParser` to read `.jad` attributes (especially `MIDlet-Jar-URL` to locate the companion `.jar` in the same directory, or download it if it's a remote URL—though for local files, they'll usually sit next to each other).
- Implement `java.lang.Thread.<init>` and `Thread.start()` in `NativeMethodBridge` to properly execute the game loop in the background.

### Phase 5.3: Graphics APIs & Input Mapping
- Implement `javax.microedition.lcdui.Graphics` methods.
- Route Compose Pointer and Virtual Keyboard events to the active `Canvas` instance via `ExecutionEngine.invokeVirtual(...)`.
- Build the "Game Settings" screen to configure Touch Flags or Screen Resolution.

## User Review Required
Please review this analysis. Do you want to start with Phase 5.2 (JAD support and Threading) so the game loop actually runs?
