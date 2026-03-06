# Walkthrough: Fix App Launch Crash — J2ME Graphics → SDL2

**Ngày**: 2026-03-06  
**Kết quả**: ✅ BUILD SUCCESSFUL

---

## Build Output

```
:shared:compileDebugKotlinAndroid        ✅
:androidApp:buildCMakeDebug[arm64-v8a]  ✅
:androidApp:buildCMakeDebug[x86_64]     ✅
:androidApp:assembleDebug               ✅  BUILD SUCCESSFUL in 5s
```

---

## Bug #1 & #2 — `NativeGraphicsBridge.kt` (androidMain)

**Trước:**
```kotlin
init { System.loadLibrary("shared") }          // ← sai tên .so
private external fun nativeInitSDL()           // ← sai JNI symbol
```

**Sau:**
```kotlin
// internal object để host JNI externals (standalone object không cho companion)
internal object NativeGraphicsBridgeJni {
    init { System.loadLibrary("j2me_native") } // ← đúng tên CMake
    external fun nativeInitSDL()               // ← symbol đúng chuẩn JNI
    // ...
}
actual object NativeGraphicsBridge {
    actual fun initSDL() = NativeGraphicsBridgeJni.nativeInitSDL()
}
```

**JNI Symbol sinh ra**: `Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridgeJni_nativeInitSDL`

---

## Bug #3 — `android_jni_bridge.cpp`

**Trước:** SDL2 init bị comment out → `gRenderer == nullptr` → render hoàn toàn câm

**Sau:** Software Framebuffer 240×320 ARGB8888 làm placeholder:

```cpp
static uint32_t gFramebuffer[240 * 320];  // offline render target
static bool     gInitialized = false;

// nativeInitSDL → clear buffer to black
// nativeFillRect → write pixels to buffer (bounds-checked)
// nativePresentScreen → log "frame ready" (Phase 4: blit to SurfaceView)
```

---

## Bug #4 — `NativeMethodBridge.kt`

**Trước:**
```kotlin
fun callNativeMethod(className, methodName, descriptor, frame): Boolean {
    // ...
    if (methodName != "<init>") frame.pop()  // ← crash nếu INVOKESTATIC (stack rỗng)
}
```

**Sau:**
```kotlin
fun callNativeMethod(..., isStatic: Boolean = false): Boolean {
    // ...
    if (!isStatic && frame.stackSize() > 0) frame.pop()  // ← safe
}
```

`ExecutionEngine` truyền đúng: `isStatic = (opcode == Opcodes.INVOKESTATIC)`

---

## Bonus — `Graphics.kt`

Thêm overload `setColor(r, g, b)` — descriptor `(III)V`:

```kotlin
fun setColorRGB(frame: ExecutionFrame) {
    val b = frame.popInt(); val g = frame.popInt(); val r = frame.popInt()
    val thisGraphics = frame.pop() as? HeapObject
    val packed = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    thisGraphics?.instanceFields?.put("color:I", packed)
}
```

---

## Render Pipeline (sau fix)

```
J2ME Game bytecode
  └─ Graphics.fillRect(x,y,w,h)            NativeMethodBridge intercepted
       └─ Graphics.kt.fillRect()           Kotlin API — pops args
            └─ NativeGraphicsBridge.fillRect()
                 └─ NativeGraphicsBridgeJni.nativeFillRect()   JNI
                      └─ android_jni_bridge.cpp
                           └─ gFramebuffer[py*240+px] = color  software FB
                                └─ [Phase 4] blit → ANativeWindow / SurfaceView
```

---

## Các file đã thay đổi

| File | Thay đổi |
|------|----------|
| `shared/src/androidMain/.../NativeGraphicsBridge.kt` | Fix lib name, dùng `NativeGraphicsBridgeJni` |
| `nativeMain/bridge/android_jni_bridge.cpp` | Software framebuffer, JNI names mới |
| `shared/src/commonMain/.../NativeMethodBridge.kt` | Thêm `isStatic`, fix fallback pop logic |
| `shared/src/commonMain/.../ExecutionEngine.kt` | Truyền `isStatic` vào `callNativeMethod` |
| `shared/src/commonMain/.../Graphics.kt` | Thêm `setColorRGB()` overload |
