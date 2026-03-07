# Implementation Plan 13: JSR-135 Media Stubs

Add basic stubs for the J2ME Media API (`javax.microedition.media`) to prevent runtime crashes and excessive logging when games attempt to play music or sounds.

## Proposed Changes

### Native Bridge

#### [MODIFY] [NativeMethodBridge.kt](file:///d:/WebTrainning\Antigravity\EmulatorForJ2ME\J2ME-KMP-Sample\shared\src\commonMain\kotlin\emulator\core\api\NativeMethodBridge.kt)

- Register `javax/microedition/media/Manager` and `javax/microedition/media/Player` methods.
- Methods to stub:
  - `Manager.createPlayer:(Ljava/io/InputStream;Ljava/lang/String;)Ljavax/microedition/media/Player;`
  - `Player.realize:()V`
  - `Player.prefetch:()V`
  - `Player.start:()V`
  - `Player.stop:()V`
  - `Player.close:()V`
  - `Player.setLoopCount:(I)V`
  - `Player.getControl:(Ljava/lang/String;)Ljavax/microedition/media/Control;`

## Verification Plan

### Automated Tests
- Build the project and ensure it compiles.
- Check logs during `Bounce Tales` startup for "Unhandled native call" warnings related to `media`.

### Manual Verification
- Launch `Bounce Tales` in the emulator.
- Observe that the game proceeds past the loading screen without crashing due to missing media methods.
