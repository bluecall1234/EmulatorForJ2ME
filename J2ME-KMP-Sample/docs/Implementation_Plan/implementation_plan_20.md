# Implementation Plan - Fix Locale and DataInputStream EOF

## Goal
Resolve the `ArrayIndexOutOfBoundsException` crash caused by missing language resources (`lang.en-US`) and ensure robust `EOFException` handling in `DataInputStream`.

## Proposed Changes

### [Component] J2ME API Bridge (`NativeMethodBridge.kt`)

#### [MODIFY] [NativeMethodBridge.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Change `microedition.locale` property from `"en-US"` to `"en"`.
- Update `readShort`, `readInt`, `readByte` in `registerJavaIoDataInputStream` to throw `RuntimeException("java/io/EOFException")` if the internal stream is null or data is exhausted.
- **[NEW]** Implement `java/io/DataOutputStream`: `<init>`, `writeByte`, `writeShort`, `writeInt`, `close`.
- **[NEW]** Implement `java/io/DataInputStream.readLong:()J`.
- **[NEW]** Stub `javax/microedition/media/Player.getState:()I`.
- **[NEW]** Stub `com/nokia/mid/ui/DeviceControl.setLights:(II)V`.
- **[NEW]** Stub `com/nokia/mid/sound/Sound` methods to prevent crashes during audio initialization.
- **[NEW]** Implement `javax/microedition/lcdui/Graphics.drawSubstring:(Ljava/lang/String;IIIII)V`.
- **[NEW]** Implement `javax/microedition/lcdui/Font.substringWidth:(Ljava/lang/String;II)I`.

## Verification Plan

### Automated Tests
- Run the emulator and monitor `emulator_log.txt`.
- Verify that `[JarLoader] Loading resource: lang.en` (or similar) is seen and the crash does not occur.
- Verify that `fillRect` and other graphics calls continue to show correct colors (opaque white/black).

### Manual Verification
- Check the screen for the "Bounce Tales" logo or main menu.
- Verify that the game progresses past the loading screen.
