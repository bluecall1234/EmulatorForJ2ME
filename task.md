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
- [ ] Implement Dynamic Bytecode Method Execution for non-native invocations (Fix for Custom Classes/Constructors)
- [ ] Implement Record Management System (RMS) for Game Saves
- [ ] Implement Touch Events Mapping (Translate Compose Pointer input to J2ME `pointerPressed` coords)
- [ ] Build "Game Settings" Screen (Dynamic Resolution configs, toggle Soft/Touch input)
- [ ] Connect Virtual Keyboard events to `ExecutionEngine` (Hardware Key mapping)
- [ ] Implement FastArrayHeapObject for Performance Memory Mode
