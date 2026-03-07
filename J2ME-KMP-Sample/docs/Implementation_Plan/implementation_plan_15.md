# Implementation Plan 15: Nokia API Support & Image Loading Fixes

Resolve the black screen in `Bounce Tales` by providing stubs for Nokia-specific APIs and implementing missing standard J2ME `Image` overloads.

## Proposed Changes

### Core Interpreter

#### [MODIFY] [BytecodeInterpreter.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\BytecodeInterpreter.kt)

- Extend the "native shell" detection in `getClass` to include:
  - `com/nokia/mid/ui/`
  - `com/nokia/mid/sound/`
  - `com/siemens/mp/`
- This ensures these classes are treated as empty shells rather than failing to load from the JAR.

### Native Bridge

#### [MODIFY] [NativeMethodBridge.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\api\NativeMethodBridge.kt)

- **Standard Image APIs**:
  - Add `javax/microedition/lcdui/Image.createImage:(Ljava/io/InputStream;)Ljavax/microedition/lcdui/Image;`
  - Add `javax/microedition/lcdui/Image.createImage:(Ljavax/microedition/lcdui/Image;IIII)Ljavax/microedition/lcdui/Image;` (for sub-images/tiles)
- **GameCanvas**:
  - Add `javax/microedition/lcdui/game/GameCanvas.flushGraphics:()V` (Stub calling `NativeGraphicsBridge.presentScreen()`).
- **Nokia Extensions**:
  - Register `com/nokia/mid/ui/DirectUtils` and `DirectGraphics`.
  - Stub `DirectUtils.getDirectGraphics:(Ljavax/microedition/lcdui/Graphics;)Lcom/nokia/mid/ui/DirectGraphics;` to return the same Graphics object.
  - Stub common `DirectGraphics` methods: `drawPixels`, `getPixels`, `setARGBColor`.

## Verification Plan

### Automated Tests
- Build the project and ensure it compiles.
- Check logs during `Bounce Tales` startup for "Unhandled native call" reductions.

### Manual Verification
- Launch `Bounce Tales` and verify if the splash screen or menu appears.
- If images appear but are garbled, verify the `DirectGraphics` drawing logic.
