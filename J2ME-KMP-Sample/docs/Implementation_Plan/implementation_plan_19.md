# Implementation Plan 19: Rendering Fixes & Diagnostics

I will implement the missing J2ME UI bridges and add diagnostic logging to the native rendering pipeline to identify why the game screen remains black.

## Proposed Changes

### 1. `shared` [NativeMethodBridge.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- **Implement Canvas UI Loop**:
  - [MODIFY] Add `javax/microedition/lcdui/Canvas.repaint:()V`.
  - [MODIFY] Add `javax/microedition/lcdui/Canvas.serviceRepaints:()V`.
- **Enhanced Image Diagnostics**:
  - Add Success/Failure logs to `createImage(InputStream)` and `createImage(byte[])`.
- **Nokia / Siemens API Stability**:
  - Ensure `com/nokia/mid/ui/DirectGraphics` stubs don't block the thread.

### 2. `shared` [NativeGraphicsBridge.kt (Android)](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/androidMain/kotlin/emulator/core/api/javax/microedition/lcdui/NativeGraphicsBridge.kt)
- **Log JNI Activity**:
  - Add periodic logs to `presentScreen()` and `drawImage()`.

### 3. `shared` [ExecutionEngine.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionEngine.kt)
- **Call Trace Logging**:
  - Implement a lightweight log for method calls to track game progress.

## Verification Plan

### Automated Tests
- Build verification: `./gradlew :androidApp:assembleDebug`

### Manual Verification
1. Launch `Bounce Tales`.
2. Observe `adb logcat` or `emulator_log.txt`.
3. Check for specific logs:
   - `[NativeBridge] Image.createImage SUCCESS`
   - `[J2ME Graphics] repaint() called`
   - `[JNI] nativePresentScreen called (frame #60)`
