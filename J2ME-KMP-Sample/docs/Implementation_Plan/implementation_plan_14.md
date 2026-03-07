# Implementation Plan 14: Bounce Tales Crash Fixes

Address the `ClassCastException` and missing native methods identified during `Bounce Tales` startup.

## Proposed Changes

### Core Interpreter

#### [MODIFY] [ExecutionEngine.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\interpreter\ExecutionEngine.kt)

- Update `Opcodes.NEWARRAY` to handle:
  - `6` -> `FloatArray(count)`
  - `7` -> `DoubleArray(count)`
- Update `Opcodes.LDC` and `LDC_W` to handle Float constants correctly if they are stored as `FloatInfo` in the constant pool. (Verify if `ConstantPoolEntry` has this).

#### [MODIFY] [BytecodeInterpreter.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\BytecodeInterpreter.kt)

- In `getClass(className)`, prevent auto-loading from JAR if the class name starts with `java/` or `javax/`. These should be handled by `NativeMethodBridge` or returned as empty shells to prevent crashes.

### Native Bridge

#### [MODIFY] [NativeMethodBridge.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\api\NativeMethodBridge.kt)

- Add `java/lang/String.equals:(Ljava/lang/Object;)Z` bridge.
- Add `java/io/DataInputStream` bridge with following methods:
  - `<init>:(Ljava/io/InputStream;)V`
  - `mark:(I)V`
  - `skipBytes:(I)I`
  - `readUnsignedShort:()I`
  - `readUTF:()Ljava/lang/String;`
  - `markSupported:()Z`
  - `close:()V`
- Implement `readUTF` logic (basic version) and `readUnsignedShort`.

## Verification Plan

### Automated Tests
- Build and run the Android app.
- Check logs for the `ClassCastException` resolution.

### Manual Verification
- Launch `Bounce Tales` and verify it gets past the `r.a` initialization and `FASTORE` calls.
- Verify `DataInputStream` calls are logged as "handled" or "stubbed" without crashing.
