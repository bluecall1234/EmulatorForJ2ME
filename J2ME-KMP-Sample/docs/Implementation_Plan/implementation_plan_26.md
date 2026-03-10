# Implementation Plan - Dynamic JAD and Manifest Parsing

This plan transitions the emulator from hard-coded properties to dynamic metadata parsing from `.jad` and `MANIFEST.MF` files, ensuring compatibility with various J2ME games.

## User Review Required

> [!IMPORTANT]
> The emulator will now look for a `.jad` file with the same name as the `.jar` in the same directory. If the `.jad` is missing, it will only use the `MANIFEST.MF` from inside the JAR.

## Proposed Changes

### [Interpreter Core]
#### [NEW] [J2meMetadata.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/J2meMetadata.kt)
- Create a class to parse and store key-value pairs from JAD/Manifest files.
- Handle line folding (lines starting with space are continuations).

#### [MODIFY] [BytecodeInterpreter.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/BytecodeInterpreter.kt)
- Add `val metadata: J2meMetadata` to the `BytecodeInterpreter` interface and its implementation.
- Load metadata during initialization in `SimpleKMPInterpreter`.

#### [MODIFY] [JarLoader.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/JarLoader.kt)
- Add `loadManifest(filePath: String): String?` to the expected `JarLoader`.

#### [MODIFY] [androidMain/JarLoader.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/androidMain/kotlin/emulator/core/JarLoader.kt)
- Implement `loadManifest` to read `META-INF/MANIFEST.MF`.

### [J2ME API]
#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Update `MIDlet.getAppProperty` to query `interpreter.metadata`.
- Update `System.getProperty` to check `interpreter.metadata` for common keys (like `microedition.platform`) while falling back to defaults.

## Verification Plan

### Manual Verification
1.  Run the emulator with `Bubble Bash Mania`.
2.  Verify in logs that Properties are loaded from `BubbleBashMania...jad` and `MANIFEST.MF`.
3.  Verify `MIDlet-Name` returned by `getAppProperty` matches the value in the metadata files.
4.  Verify `System.getProperty("microedition.platform")` returns the value defined in the metadata (if any).
