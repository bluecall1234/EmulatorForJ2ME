# Emulator Development Tasks

## Phase 5.2: JAD Support & Native Threading
- [x] Update `GameLibraryScreen` & File Picker to accept `.jad` files.
- [x] Create `JadParser` to parse key-value attributes from `.jad` files.
- [x] If a `.jad` is selected, locate the companion `.jar` via `MIDlet-Jar-URL`.
- [x] Implement `java.lang.Thread.<init>` native bridge.
- [x] Implement `java.lang.Thread.start()` native bridge (launch background coroutine).

## Phase 5.3: Graphics APIs & Input Mapping
- [x] Implement `javax.microedition.lcdui.Graphics` bridging methods.
- [x] Route pointer events (Touch) to `Canvas.pointerPressed` via `ExecutionEngine`.
- [x] Build "Game Settings" screen to configure Touch Flags / Dimensions.

## Phase 5.4: Fixing Game Load Crashes
- [x] Implement missing `0xAA` (TABLESWITCH) and `0xAB` (LOOKUPSWITCH) opcodes.
- [x] Implement missing `java.lang.StringBuffer` append/toString methods to prevent NativeBridge unhandled call runtime fallbacks.
- [x] Implement overloaded `StringBuffer.<init>` constructors (`String` and `I`) to fix NullPointerExceptions during game string memory instantiations.
