# Implementation Plan 12: Connecting Virtual Keyboard to Execution Engine

Allow the user to interact with J2ME games by routing virtual keypad presses to the `ExecutionEngine`.

## Proposed Changes

### Core Interpreter

#### [MODIFY] [BytecodeInterpreter.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/BytecodeInterpreter.kt)

- Add `injectKeyEvent(type: Int, keyCode: Int)` to the `companion object`.
- Logic:
  1. Get the `activeDisplayable` from `Display`.
  2. Map `type` to method names: `0` -> `keyPressed`, `1` -> `keyReleased`, `2` -> `keyRepeated`.
  3. Resolve the method with descriptor `(I)V` (takes one integer argument: the keyCode).
  4. Launch a coroutine to execute the method on the interpreter.

### Android UI

#### [MODIFY] [MainActivity.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/androidApp/src/main/java/com/example/j2me/android/MainActivity.kt)

- Update the `VirtualKeypad` callback in `EmulatorScreen` to call `BytecodeInterpreter.injectKeyEvent(0, keyCode)`.

## Verification Plan

### Automated Tests
- Build and run the Android app.
- Load a game that uses keypad input (e.g., Bubble Bash Mania or a simple test Midlet).

### Manual Verification
- Press the "UP", "DOWN", "LEFT", "RIGHT", and "FIRE" buttons on the virtual keypad.
- Verify that the game responds to these inputs (e.g., character moves, menu items change).
- Check the console logs for `[VM] INVOKEVIRTUAL ... .keyPressed(I)V` calls.
