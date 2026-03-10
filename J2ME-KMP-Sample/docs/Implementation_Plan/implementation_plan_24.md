# Implementation Plan - Fix MIDlet Instantiation & Property Access

This plan addresses the `NullPointerException` when calling `MIDlet.getAppProperty` by correctly instantiating the MIDlet and providing a global access mechanism.

## Proposed Changes

### [Android Host]
#### [MODIFY] [MainActivity.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/androidApp/src/main/java/com/example/j2me.android/MainActivity.kt)
- Update `startGameLoop` to use `allocateObject` before calling `<init>`.
- Pass the allocated MIDlet instance as the first argument to `<init>` and `startApp`.

### [Interpreter Core]
#### [MODIFY] [BytecodeInterpreter.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/BytecodeInterpreter.kt)
- Add an `activeMIDlet` field to `BytecodeInterpreter` and its implementation.
- Set `activeMIDlet` when the MIDlet is instantiated.

### [J2ME API]
#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Update `MIDlet.getAppProperty` to use the `activeMIDlet` property source or validate `this` correctly.

## Verification Plan
- Build and run on Android Emulator.
- Verify `MIDlet.getAppProperty` calls are logged with valid keys.
- Check `emulator_log.txt` to ensure no `NullPointerException` in `MIDlet` methods.
