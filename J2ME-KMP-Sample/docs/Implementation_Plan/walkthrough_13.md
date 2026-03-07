# Walkthrough 13: Bounce Tales Crash Fixes & DataInputStream Support

I have resolved the `ClassCastException` and missing native method errors that were causing `Bounce Tales` to crash on startup.

## Changes Made

### 1. Bytecode Interpreter Fixes
- **NEWARRAY Support**: Added missing support for `T_FLOAT` (6) and `T_DOUBLE` (7) in the `NEWARRAY` opcode. This resolves the `ClassCastException: int[] cannot be cast to float[]` when the game initializes its physics/render arrays.
- **Native Class Protection**: Updated the `getClass` logic to prevent the interpreter from attempting to load `java.*` or `javax.*` classes from the game's JAR file. These are now correctly identified as native-only to ensure stability.

### 2. Native Bridge Enhancements
- **DataInputStream Stubs**: Implemented native handlers for `java.io.DataInputStream`, including:
  - `readUTF()`: Correctly decodes Modified UTF-8 strings (essential for loading level metadata and text).
  - `readUnsignedShort()`: Reads 16-bit big-endian values.
  - `skipBytes()`, `mark()`, `close()`.
- **String Support**: Added `java.lang.String.equals(Object)` and `String.length()` to allow the game to perform string comparisons during resource lookup and configuration parsing.

## Verification Results

- **Build Status**: `BUILD SUCCESSFUL` on Android.
- **Opcode Handling**: Verified that `FASTORE` / `FALOAD` now work correctly with `FloatArray` instances created via `NEWARRAY`.
- **Resource Loading**: `Bounce Tales` successfully loads its level data and translation strings via the new `DataInputStream.readUTF` implementation.

## Next Steps
- Users can now launch `Bounce Tales.jar` and reach the gameplay phase.
- Further testing is recommended to identify any additional obfuscated opcode patterns.
