# Implementation Plan: Phase 5.3 - Graphics APIs & Input Mapping

## Goal Description
The J2ME Emulator is currently able to parse `.jad` files, load resources, and execute the game loop in a background thread using dynamic bytecode interpretation. However, we cannot see any game output or interact with it because the `javax.microedition.lcdui.Graphics` APIs are not implemented, and input events (like Touch) are not mapped to the J2ME `Canvas` interface.

In this phase, we will implement the core components that bridge Jetpack Compose UI with the J2ME `Graphics` and `Canvas` subsystems, enabling full visual rendering and interactive gameplay for "Bubble Bash Mania" (and other games).

## User Review Required
None at this stage.

## Proposed Changes

### `shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/`

#### [MODIFY] [Graphics.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/Graphics.kt)
Implement native stubs for the standard J2ME `Graphics` calls. This involves parsing arguments from the `ExecutionFrame` and forwarding them to `NativeGraphicsBridge`.
- `setColor(int RGB)` / `setColor(int R, int G, int B)`
- `fillRect(int x, int y, int width, int height)`
- `drawImage(Image img, int x, int y, int anchor)`
- `drawString(String str, int x, int y, int anchor)`
- `setClip(int x, int y, int width, int height)`

#### [MODIFY] [NativeGraphicsBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/NativeGraphicsBridge.kt)
Update the common, android, and iOS actual implementations. For Android, `NativeGraphicsBridge` uses JNI to call C++ SDL2 functions. We need to expose new `external fun` for all the drawing commands defined above.

### `nativeMain/bridge/`

#### [MODIFY] [android_jni_bridge.cpp](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/nativeMain/bridge/android_jni_bridge.cpp)
Implement the C++ JNI counterparts for the SDL2 rendering commands.
- `Java_..._fillRect(...)` maps to `SDL_RenderFillRect`
- `Java_..._drawImage(...)` maps to `SDL_RenderCopy`
- `Java_..._setColor(...)` maps to `SDL_SetRenderDrawColor`

### `shared/src/commonMain/kotlin/emulator/core/interpreter/`

#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
Register the new `Graphics` method signatures so the `ExecutionEngine` can cleanly intercept them and route them to our custom API implementations.

#### [MODIFY] [ExecutionEngine.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionEngine.kt)
When Compose detects a touch event, it needs to be injected into the `ExecutionEngine`. We will add a helper method in the BytecodeInterpreter / ExecutionEngine that programmatically acts like `INVOKEVIRTUAL` to call `pointerPressed(II)`, `pointerReleased(II)`, and `pointerDragged(II)` on the currently active `Canvas` object in the heap.

### `androidApp/src/main/java/com/example/j2me/android/`

#### [MODIFY] [EmulatorScreen.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/androidApp/src/main/java/com/example/j2me/android/EmulatorScreen.kt)
- Wrap the Native `AndroidView` (SDL2 Surface) with `Modifier.pointerInput` to intercept tap and drag gestures.
- Upon receiving a `PointerEventType.Press`, determine the touch coordinates, translate them relative to the game's virtual resolution, and pass them down into the emulator engine.
- Build a toggle or Game Settings dialog that lets the user enable/disable Touch input, utilizing the `touchSupport` boolean we extracted during Phase 5.2.

## Verification Plan

### Automated Tests
- Not applicable for UI layer.

### Manual Verification
- Launch "Bubble Bash Mania".
- Ensure that the Gameloft splash screen or menu visually renders on the screen, meaning the `Graphics.drawImage` and `fillRect` APIs are functioning perfectly.
- Tap the screen and verify that menu buttons highlight or react, confirming that `Canvas.pointerPressed` is correctly bridged from Compose multiplatform to the J2ME execution heap.
