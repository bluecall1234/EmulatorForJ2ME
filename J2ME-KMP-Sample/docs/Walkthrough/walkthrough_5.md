# Walkthrough: Fix App Launch Crash (J2ME Graphics → SDL2)

## Kết quả Build

```
BUILD SUCCESSFUL
:shared:compileDebugKotlinAndroid  ✅
:androidApp:buildCMakeDebug[arm64-v8a]  ✅  
:androidApp:buildCMakeDebug[x86_64]  ✅
:androidApp:assembleDebug  ✅
```

---

## Bug #1 & #2 — `NativeGraphicsBridge.kt` (androidMain)

**Trước:**
- Load sai library: `System.loadLibrary("shared")` → không tìm thấy JNI functions
- `private external fun` trong `actual object` → không sinh ra JNI symbol đúng

**Sau:**
- `System.loadLibrary("j2me_native")` — khớp với `CMakeLists.txt`
- Tách ra `internal object NativeGraphicsBridgeJni` để host các `external fun`
- Symbol JNI sinh ra đúng định dạng: `Java_..._NativeGraphicsBridgeJni_nativeInitSDL`

render_diffs(file:///d:/WebTrainning/Antigravity/Example1/J2ME-KMP-Sample/shared/src/androidMain/kotlin/emulator/core/api/javax/microedition/lcdui/NativeGraphicsBridge.kt)

---

## Phase 5.1: Dynamic Bytecode Execution (Completed)

We implemented a full dynamic bytecode execution engine instead of relying entirely on `NativeMethodBridge`.
- **Method Execution**: The `ExecutionEngine` was upgraded to dynamically spawn new frames and recursively execute `.class` bytecode whenever a non-native method is invoked (`INVOKEVIRTUAL`, `INVOKESPECIAL`, etc.).
- **Class Initializers**: Ensured `<clinit>` blocks run when encountering `NEW`, `GETSTATIC`, `PUTSTATIC`, or `INVOKESTATIC`.
- **Opcode Expansion**: We dramatically expanded the J2ME JVM feature set. Added support for:
  - Primitive and Multidimensional Arrays (`LALOAD`, `BASTORE`, `MULTIANEWARRAY`, etc.)
  - Float/Double comparisons (`FCMPG`, `DCMPL`, etc.)
  - Bitwise manipulations (`ISHL`, `IUSHR`, `LXOR`, etc.)
  - `MONITORENTER` / `MONITOREXIT` synchronization
- **Default Primitives**: Fixed a crash caused by uninitialized static fields by returning proper default primitives (e.g. `0`, `0.0f`).

**Result:** The game `GloftBBPM` seamlessly loads all its internal classes (`cGame`, `i`, etc.), sequentially runs all their static initializers, and successfully completes `startApp` natively via our Engine, reaching the `java.lang.Thread.start()` game loop trigger!

---

## Bug #3 — `android_jni_bridge.cpp`

**Trước:**
- Toàn bộ `SDL_Init`, `SDL_CreateWindow`, `SDL_CreateRenderer` bị comment out
- `gRenderer == nullptr` → `nativeFillRect` không làm gì, render câm hoàn toàn

**Sau:**
- Thay bằng **Software Framebuffer** 240×320 ARGB8888
- `nativeInitSDL` → xóa buffer về đen
- `nativeFillRect` → vẽ pixel đúng vào buffer (bounded, không crash)
- `nativePresentScreen` → log "frame ready" (Phase 4 sẽ blit lên SurfaceView)
- Cập nhật JNI function names → `NativeGraphicsBridgeJni`

render_diffs(file:///d:/WebTrainning/Antigravity/Example1/J2ME-KMP-Sample/nativeMain/bridge/android_jni_bridge.cpp)

---

## Bug #4 — `NativeMethodBridge.kt`

**Trước:**
- Fallback path luôn `frame.pop()` để pop `this`, kể cả cho `INVOKESTATIC` (không có `this` trên stack) → `StackUnderflowException`/crash

**Sau:**
- Thêm tham số `isStatic: Boolean = false` vào `callNativeMethod()`
- `ExecutionEngine` truyền `isStatic = (opcode == Opcodes.INVOKESTATIC)`
- Fallback path chỉ pop `this` khi `!isStatic && frame.stackSize() > 0`

render_diffs(file:///d:/WebTrainning/Antigravity/Example1/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)

---

## Bonus — `Graphics.kt`

- Thêm `setColorRGB(frame)` handle descriptor `(III)V` — overload 3-param của J2ME `Graphics.setColor(r,g,b)`

---

## Kiến trúc Render Pipeline (sau fix)

```
J2ME Game bytecode
  └─ Graphics.fillRect(x,y,w,h)    ← Intercepted by NativeMethodBridge
       └─ Graphics.kt fillRect()   ← Kotlin API layer pops stack args
            └─ NativeGraphicsBridge.fillRect() ← Kotlin actual object
                 └─ NativeGraphicsBridgeJni.nativeFillRect() ← JNI call
                      └─ android_jni_bridge.cpp ← C++ software framebuffer
                           └─ [Phase 4] blit to ANativeWindow / SurfaceView
```

> [!NOTE]
> `gRenderer` (SDL2) vẫn chưa được khởi tạo vì cần `SurfaceView` (Phase 4).
> Software framebuffer đóng vai trò placeholder an toàn đến khi Phase 4 hoàn thiện.
