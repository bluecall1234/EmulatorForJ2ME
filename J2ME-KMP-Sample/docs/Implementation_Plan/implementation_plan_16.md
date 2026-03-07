# Implementation Plan 16: Hierarchy-Aware Method Resolution & Critical Bridges

Fix the black screen in `Bounce Tales` by correctly resolving inherited native methods (like `getWidth`, `getHeight`, `getGraphics`) and adding missing synchronization bridges.

## User Review Required

> [!IMPORTANT]
> This change introduces a more complex method resolution logic in the `ExecutionEngine`. While it improves compatibility with obfuscated games (like `Bounce Tales`), it might slightly impact performance.

## Proposed Changes

### Core Interpreter

#### [MODIFY] [ExecutionEngine.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\interpreter\ExecutionEngine.kt)

- Refactor `INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, and `INVOKEINTERFACE` to use a shared `resolveAndExecute` helper.
- The helper will:
  1. Start with the target class.
  2. Check `NativeMethodBridge.callNativeMethod` for the current class.
  3. If not found, check if the bytecode class exists and has the method.
  4. If not found, move to the superclass and repeat until `java/lang/Object` or `none` is reached.
- This ensures `p.getWidth()` correctly hits the `Displayable.getWidth()` native bridge.

### Native Bridge

#### [MODIFY] [NativeMethodBridge.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\api\NativeMethodBridge.kt)

- **Object Sync**:
  - Add `java/lang/Object.wait:(J)V` (Stub with `Thread.sleep` or similar).
  - Add `java/lang/Object.notify:()V` and `notifyAll:()V`.
- **GameCanvas**:
  - Add `javax/microedition/lcdui/game/GameCanvas.getGraphics:()Ljavax/microedition/lcdui/Graphics;`
  - This bridge should returning a Graphics object targeted at the screen (or backbuffer if implemented).
- **DirectGraphics**:
  - Add missing stubs for `DirectGraphics` if they appeared in unhandled logs.

## Verification Plan

### Automated Tests
- Build and run the Android app.
- Check logs for "Resolved via hierarchy" or similar trace messages.
- Verify that `p.getWidth()` now returns `240` instead of `0`.

### Manual Verification
- Launch `Bounce Tales` and verify if the black screen transitions to a loading screen or menu.
- Verify if `fillRect` calls now have non-zero width and height.
