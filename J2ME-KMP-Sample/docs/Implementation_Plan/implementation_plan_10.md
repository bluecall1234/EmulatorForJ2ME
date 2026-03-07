# Implementation Plan: Phase 5.4 - Fixing Game Load Crashes

## Goal Description
During testing of Phase 5.3 with "Bubble Bash Mania", the game loading process crashed. Analysis of the logs revealed two core issues causing dynamic execution failure:
1.  **Missing Opcodes:** The KMP Execution Engine encountered bytecode instruction `0xAA` (TABLESWITCH) which was not yet implemented in `ExecutionEngine.kt`, halting execution. 
2.  **Missing Core `java.lang` Implementations:** The game triggered unhandled native calls to `java.lang.StringBuffer.<init>`, `StringBuffer.append`, and `StringBuffer.toString`. Because these standard Java string manipulation routines lacked native KMP implementations in `NativeMethodBridge`, the emulator fell back to stubbing them, which lead to invalid object states and downstream execution failure.

In this phase, we will implement the missing switch opcodes and add the core `StringBuffer` routines to the `NativeMethodBridge` to allow the game logic to parse strings successfully and boot properly.

## User Review Required
None at this stage.

## Proposed Changes

### `shared/src/commonMain/kotlin/emulator/core/interpreter/`

#### [MODIFY] [Opcodes.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/Opcodes.kt)
Add JVM standard definitions for branching opcodes:
- `0xAA` -> `TABLESWITCH`
- `0xAB` -> `LOOKUPSWITCH`
- Update the `nameOf()` debugging output utility to recognize them.

#### [MODIFY] [ExecutionFrame.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionFrame.kt)
- Add a `readU4(): Long` parsing method to the stack frame, enabling the engine to safely read the 32-bit (4 byte) offset indicators required by tableswitch instructions.

#### [MODIFY] [ExecutionEngine.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionEngine.kt)
Implement handling for `Opcodes.TABLESWITCH` and `Opcodes.LOOKUPSWITCH` inside the bytecode evaluation loop.
- Pop the index/key from the frame stack.
- Parse the JVM alignment padding (0-3 bytes) depending on the currently advanced PC.
- Extract the default offset and tables.
- Iterate the lookup options or access the bounds table.
- Calculate the correct `jumpOffset` pointer math and advance the `frame.pc` register accordingly.

### `shared/src/commonMain/kotlin/emulator/core/api/`

#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
Add explicit intercept patterns for `java.lang.StringBuffer`.
- `StringBuffer.<init>:()V`: Initialize an empty Kotlin String into the `instanceFields["value"]` registry.
- `StringBuffer.append:(Ljava/lang/String;)Ljava/lang/StringBuffer;`: Concatenate strings and push the `this` reference back to the stack.
- `StringBuffer.append:(I)Ljava/lang/StringBuffer;`: Parse and concatenate Integers.
- `StringBuffer.toString:()Ljava/lang/String;`: Retrieve the native Kotlin string from memory and push it to the evaluation stack.

## Verification Plan

### Automated Tests
- Not applicable.

### Manual Verification
- Compile the KMP Android application (`androidApp:compileDebugKotlin`) to verify the engine bytecode operations and switch offset types (`UInt` vs `Int`) calculate without throwing ambiguity errors.
- Launch "Bubble Bash Mania". The game should proceed gracefully past the splash screens or initial logging procedures without crashing on unidentified bytecode exceptions.
