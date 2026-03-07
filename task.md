# Task Plan: Clean-room Hybrid J2ME Emulator (KMP + SDL2 + CMP)

## Phase 1: Core Architecture & Setup
- [x] Configure Project for Native Interop (C-Interop)
- [x] Set up SDL2 submodules/binaries in `nativeMain`
- [x] Create JNI Bridge for Android (`androidMain` -> SDL2)
- [x] Create C-Interop Bridge for iOS (`iosMain` -> SDL2)

## Phase 2: Bytecode Interpreter (The Brain)
- [x] Build Constant Pool Parser
- [x] Implement Class Loader (parsing Methods, Fields, Attributes)
- [x] Build Execution Engine (Opcode loops)
- [x] Implement Memory Management (Object Allocation)

## Phase 3: J2ME API Implementation (The Skeleton)
- [x] Port CLDC APIs (`java.lang.*`, `java.util.*`, `java.io.*`)
- [x] Port MIDP UI APIs (`javax.microedition.lcdui.*`)
- [x] Map J2ME Graphics calls to SDL2 Render APIs

## Phase 4: UI & App Shell (The Face)
- [x] Build Compose Multiplatform App Shell (`composeApp`)
- [ ] Implement Game Library (File Picker, Add/Delete Games)
- [x] Build Virtual Keyboard Overlay (CMP over SDL2 Surface)

## Phase 5: Optimization & System Interaction (Future)
### Phase 5.1: Dynamic Bytecode Method Execution
- [x] Implement Dynamic Bytecode Method Execution for non-native invocations (Fix for Custom Classes/Constructors)
- [x] Expand Opcodes support (Shift, Bitwise, Multi-dim Arrays, Local Variable Load variants)

## Phase 5.2: JAD Support & Native Threading
- [x] Update `GameLibraryScreen` & File Picker to accept `.jad` files.
- [x] Create `JadParser` to parse key-value attributes from `.jad` files.
- [x] If a `.jad` is selected, locate the companion `.jar` via `MIDlet-Jar-URL`.
- [x] Implement `java.lang.Thread.<init>` native bridge.
- [x] Implement `java.lang.Thread.start()` native bridge (launch background coroutine).

## Phase 5.3: Graphics APIs & Input Mapping
- [x] Implement `javax.microedition.lcdui.Graphics` bridging methods.
- [x] Route pointer events (Touch) to `Canvas.pointerPressed` via `ExecutionEngine`.
- [x] Build "Game Settings" screen to configure Touch Flags / Dimensions.

## Phase 5.4: Fixing Game Load Crashes
- [x] Implement missing `0xAA` (TABLESWITCH) and `0xAB` (LOOKUPSWITCH) opcodes.
- [x] Implement missing `java.lang.StringBuffer` append/toString methods to prevent NativeBridge unhandled call runtime fallbacks.
- [x] Implement overloaded `StringBuffer.<init>` constructors (`String` and `I`) to fix NullPointerExceptions during game string memory instantiations.
- [x] Implement `StringBuffer.append` for primitives (`Float`, `Long`, `Char`, `Boolean`) to prevent NativeBridge fallbacks returning `0` references.

## Phase 6: Future APIs
- [ ] Implement Record Management System (RMS) for Game Saves
- [ ] Connect Virtual Keyboard events to `ExecutionEngine` (Hardware Key mapping)
- [ ] Implement FastArrayHeapObject for Performance Memory Mode
