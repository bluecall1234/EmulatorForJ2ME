# Chat History — J2ME Emulator Development

> 📁 File này tóm tắt lịch sử phát triển dự án J2ME Emulator theo từng phiên làm việc.  
> **Cập nhật lần cuối**: 2026-03-06

---

## Session 1 — 2026-03-04 (Conversation: a81392b1)

### Mục tiêu
Xây dựng J2ME Emulator đa nền tảng (Android + iOS) theo kiến trúc **Hybrid: KMP + SDL2 + Compose Multiplatform**.

### Kiến trúc được chốt
```
J2ME-KMP-Sample/
├── androidApp/        # Android app shell (Compose)
├── shared/            # Logic cốt lõi (KMP)
│   ├── commonMain/    # Bytecode Interpreter, J2ME APIs (expect)
│   ├── androidMain/   # JNI Bridge → SDL2 (actual)
│   └── iosMain/       # C-Interop → SDL2 (actual)
└── nativeMain/        # C/C++ code (SDL2, JNI bridge)
    └── bridge/        # android_jni_bridge.cpp
```

### Công việc đã hoàn thành

#### Phase 1 — Native Interop ✅
- Cấu hình `cinterop` trong `shared/build.gradle.kts` cho iOS
- Setup SDL2 submodule trong `nativeMain/`
- Tạo `NativeBridge.kt` (Android: `System.loadLibrary("j2me_native")`)
- Tạo `android_jni_bridge.cpp` với JNI functions cơ bản

#### Phase 2 — Bytecode Interpreter ✅
- **`ClassFileParser.kt`**: Parse binary `.class` file header, interfaces, fields, methods
- **`ConstantPoolParser.kt` + `ConstantPoolEntry.kt`**: Parse toàn bộ Constant Pool (18 entry types)
- **`ClassMembers.kt`**: Đại diện cho `MemberInfo`, `AttributeInfo`, `CodeAttribute`
- **`JavaClassFile.kt`**: Full class file parser + `JarLoader` mock data builder
- **`ExecutionFrame.kt`**: Stack frame với operand stack + local variables + PC
- **`Opcodes.kt`**: Bảng tên 100+ opcode JVM
- **`ExecutionEngine.kt`**: Vòng lặp fetch-decode-execute với 80+ opcode (arithmetic, branching, method invocation, object/array ops)
- **`Heap.kt`**: `HeapObject` với `instanceFields` map

#### Phase 3 — J2ME API Implementation ✅
- **`NativeMethodBridge.kt`**: Router intercepting `java.*`/`javax.*` calls
- **`java/lang/System.kt`** + **`SystemTime.kt`**: `currentTimeMillis()` (KMP expect/actual)
- **`java/io/PrintStream.kt`**: `println()` stub
- **`javax/microedition/lcdui/Display.kt`**: `getDisplay()`, `setCurrent()`
- **`javax/microedition/lcdui/Canvas.kt`**: `repaint()`, `setFullScreenMode()`
- **`javax/microedition/lcdui/Graphics.kt`**: `setColor()`, `fillRect()`, `drawImage()`
- **`javax/microedition/lcdui/NativeGraphicsBridge.kt`** (expect/actual): Bridge → SDL2 JNI
- **`javax/microedition/midlet/MIDlet.kt`**: Base class cho J2ME apps
- **`demo/game/MyTestGame.kt`**: Demo MIDlet để test

---

## Session 2 — 2026-03-06 (Conversation: 5ac33602)

### Vấn đề báo cáo
App crash khi launch trên Android emulator, liên quan đến phần **J2ME Graphics → SDL2 Render APIs**.

### Root Cause Analysis

| # | Bug | File | Mức độ |
|---|-----|------|--------|
| 1 | JNI extern trong `standalone object` → sai symbol name → `UnsatisfiedLinkError` | `NativeGraphicsBridge.kt` (androidMain) | 🔴 CRITICAL |
| 2 | `loadLibrary("shared")` — sai tên .so, đúng phải là `"j2me_native"` | `NativeGraphicsBridge.kt` (androidMain) | 🔴 CRITICAL |
| 3 | `gRenderer == nullptr` vì SDL2 init bị comment (cần SurfaceView chưa có) | `android_jni_bridge.cpp` | 🟠 HIGH |
| 4 | `NativeMethodBridge` luôn pop `this` kể cả INVOKESTATIC → stack underflow | `NativeMethodBridge.kt` | 🟡 MEDIUM |

### Fixes đã thực hiện

#### Fix #1 & #2 — `NativeGraphicsBridge.kt` (androidMain)
- Tạo `internal object NativeGraphicsBridgeJni` để host các JNI `external fun`
- Đổi `loadLibrary("shared")` → `loadLibrary("j2me_native")`
- `actual object NativeGraphicsBridge` delegate sang `NativeGraphicsBridgeJni`

#### Fix #3 — `android_jni_bridge.cpp`
- Thay SDL2 render (cần SurfaceView) bằng **software framebuffer 240×320 ARGB8888**
- Implement `nativeFillRect` với bounds-check
- Cập nhật JNI function names → `NativeGraphicsBridgeJni`
- Phase 4: sẽ thay bằng `blit(gFramebuffer → ANativeWindow)`

#### Fix #4 — `NativeMethodBridge.kt` + `ExecutionEngine.kt`
- Thêm `isStatic: Boolean = false` vào `callNativeMethod()`
- `ExecutionEngine` truyền `isStatic = (opcode == Opcodes.INVOKESTATIC)`
- Fallback path: chỉ pop `this` khi `!isStatic && stackSize() > 0`

#### Bonus — `Graphics.kt`
- Thêm `setColorRGB()` cho overload `setColor(int r, int g, int b)` — descriptor `(III)V`

### Build Result
```
:androidApp:assembleDebug — BUILD SUCCESSFUL in 5s  ✅
:androidApp:buildCMakeDebug[arm64-v8a]  ✅
:androidApp:buildCMakeDebug[x86_64]     ✅
```

---

## Trạng thái hiện tại (2026-03-06)

### Hoàn thành
- [x] Phase 1: Native Interop (JNI + C-Interop)
- [x] Phase 2: Bytecode Interpreter
- [x] Phase 3: J2ME APIs (CLDC + MIDP) — crash bugs đã fix

### Còn lại
- [ ] **Phase 4**: Compose Multiplatform App Shell
  - [ ] Build Compose App Shell (màn hình chính, game library)
  - [ ] Implement SurfaceView integration (thay software framebuffer bằng SDL2 thực)
  - [ ] Virtual Keyboard Overlay (Compose đè lên SDL2 Surface)
- [ ] **Phase 5**: Optimization (FastArrayHeapObject)

### Ghi chú kỹ thuật
- `MainActivity` hiện gọi `NativeGraphicsBridge.initSDL()` trực tiếp — cần chuyển vào `SurfaceView.Callback.surfaceCreated()` ở Phase 4
- Software framebuffer (`gFramebuffer[]`) sẵn sàng để `blit` lên `ANativeWindow` khi Phase 4 hoàn thành

---

## Hướng dẫn cập nhật file này

Sau mỗi phiên làm việc, thêm 1 section mới theo format:

```markdown
## Session N — YYYY-MM-DD

### Vấn đề / Mục tiêu
...

### Công việc đã làm
...

### Build Result
...
```
