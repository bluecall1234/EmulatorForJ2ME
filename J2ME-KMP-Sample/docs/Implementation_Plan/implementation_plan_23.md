# Implementation Plan - Fix String.getBytes & NPE Handling

This plan covers the fix for the `arraylength on non-array: null` error and the implementation of missing `String` methods and robust exception handling.

## Proposed Changes

### [Interpreter Core]
#### [MODIFY] [ExecutionFrame.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionFrame.kt)
- Added `popFloat()` and `popDouble()` methods to support array operations for these types.

#### [MODIFY] [ExecutionEngine.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionEngine.kt)
- Added `throwException` helper to throw J2ME-level exceptions.
- Added NullPointerException and ArrayIndexOutOfBoundsException checks to:
    - `ARRAYLENGTH`, `IALOAD`, `IASTORE`, `BALOAD`, `BASTORE`, `CALOAD`, `CASTORE`, `SALOAD`, `SASTORE`, `LALOAD`, `LASTORE`, `FALOAD`, `FASTORE`, `DALOAD`, `DASTORE`, `AALOAD`, `AASTORE`.
    - `GETFIELD`, `PUTFIELD`.
    - `INVOKEVIRTUAL`, `INVOKEINTERFACE`.
- Replaced invalid `return@when` with `continue`.

### [J2ME API]
#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Implemented `java/lang/String.getBytes()` and `java/lang/String.getBytes(String enc)`.

## Verification Plan
- Build, install, and run on Android Emulator.
- Monitor `emulator_log.txt` for any crashes or "arraylength" errors.
- Verify game "Bounce Tales" starts correctly.
