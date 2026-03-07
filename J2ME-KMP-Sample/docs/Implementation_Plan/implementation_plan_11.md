# Implementation Plan: Phase 6 - Record Management System (RMS)

## Goal Description
The J2ME Record Management System (RMS) is necessary for games to save their state, high scores, and settings persistently. We need to implement the `javax.microedition.rms.RecordStore` API within our KMP emulator. 
Since this emulator runs across Android (and eventually iOS via Kotlin Multiplatform), we should use a cross-platform file storage mechanism. The standard `okio` library or Kotlin's standard platform-specific file APIs can handle writing the raw bytes arrays to a persistent `DataStore` or local directory.

We will bridge the J2ME bytecode calls to native Kotlin methods that manage a `RecordStore` class handling `.rms` file serialization.

## User Review Required
- File storage location: We will store `.rms` files in a dedicated `rms_data` subdirectory within the app's internal files directory.

## Proposed Changes

### `shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/rms/`

#### [NEW] [RecordStore.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/rms/RecordStore.kt)
Create the Kotlin representation of a record store.
- Implement memory cache (`MutableMap<Int, ByteArray>`).
- Implement methods:
  - `openRecordStore(name)`
  - `closeRecordStore()`
  - `addRecord(byte[], offset, numBytes)` -> returns `recordId`
  - `getRecord(recordId)` -> returns `byte[]`
  - `setRecord(recordId, byte[], offset, numBytes)`
  - `deleteRecord(recordId)`
  - `getNumRecords()`
- **Persistence Handling:** Delegate actual disk I/O to an interface (e.g. `RmsStorage`), allowing platform-specific actualization (Android internal storage vs iOS documents directory) using `expect`/`actual` declarations.

### `shared/src/commonMain/kotlin/emulator/core/api/`

#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Create a `registerJavaxMicroeditionRmsRecordStore()` function to intercept JVM bytecode invocations for:
  - `openRecordStore(Ljava/lang/String;Z)Ljavax/microedition/rms/RecordStore;`
  - `addRecord([BII)I`
  - `getRecord(I)[B`
  - `setRecord(I[BII)V`
  - `closeRecordStore()V`
  - `getNumRecords()I`
  - `deleteRecordStore(Ljava/lang/String;)V`

### `shared/src/androidMain/kotlin/emulator/core/api/javax/microedition/rms/`
#### [NEW] [AndroidRmsStorage.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/androidMain/kotlin/emulator/core/api/javax/microedition/rms/AndroidRmsStorage.kt)
- Provide the Android-specific implementation of the persistent storage saving the byte arrays to `Context.getFilesDir() + "/rms/" + storeName`.

### `shared/src/iosMain/kotlin/emulator/core/api/javax/microedition/rms/`
#### [NEW] [IosRmsStorage.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/iosMain/kotlin/emulator/core/api/javax/microedition/rms/IosRmsStorage.kt)
- Provide the iOS-specific implementation for file writing to `NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, ...)`.

## Verification Plan

### Automated Tests
- None

### Manual Verification
- Compile and run Android build (`./gradlew :androidApp:installDebug`).
- Track logcat for `RecordStore` calls.
- Launch a game, change settings or progress past a save point, restart the emulator, and ensure the save data remains intact (RMS files are loaded properly).
