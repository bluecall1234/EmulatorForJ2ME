# Fix: App Launch Crash — J2ME Graphics → SDL2 Pipeline

**Ngày**: 2026-03-06  
**Trạng thái**: ✅ Hoàn thành — BUILD SUCCESSFUL

---

## Root Cause Analysis

Sau khi phân tích toàn bộ pipeline khởi động, tìm ra **4 nguyên nhân crash**:

### Bug #1 – Sai tên JNI function 🔴 CRITICAL

**File**: `shared/src/androidMain/kotlin/.../NativeGraphicsBridge.kt`

Kotlin khai báo `private external fun nativeInitSDL()` bên trong `actual object NativeGraphicsBridge`. JNI symbol thực sự cần phải là:
```
Java_emulator_core_api_javax_microedition_lcdui_NativeGraphicsBridge_nativeInitSDL
```
Nhưng Kotlin `standalone object` không phát sinh symbol đúng chuẩn JNI khi dùng `private external fun` trực tiếp.

**Kết quả**: `UnsatisfiedLinkError` → crash ngay khi gọi `initSDL()`.

---

### Bug #2 – Sai tên thư viện `.so` 🔴 CRITICAL

**File**: `shared/src/androidMain/kotlin/.../NativeGraphicsBridge.kt`

```kotlin
System.loadLibrary("shared")  // ← SAI: không có libshared.so chứa JNI functions
```

`CMakeLists.txt` build ra `libj2me_native.so`. `libshared.so` là Kotlin bytecode, không phải C++ JNI.

**Kết quả**: `UnsatisfiedLinkError` bị bắt, nhưng tiếp theo gọi JNI lại crash `NoSuchMethodError`.

---

### Bug #3 – `gRenderer == nullptr` — render pipeline câm 🟠 HIGH

**File**: `nativeMain/bridge/android_jni_bridge.cpp`

Toàn bộ `SDL_Init`, `SDL_CreateWindow`, `SDL_CreateRenderer` bị comment out. `gRenderer` luôn là `nullptr`. Các hàm render check `if (gRenderer)` nên không crash nhưng **không làm gì cả**.

> SDL2 cần `ANativeWindow` (từ `SurfaceView` — Phase 4 chưa làm) để khởi tạo window/renderer. Không thể gọi SDL2 render trực tiếp khi chưa có Surface.

---

### Bug #4 – NativeMethodBridge pop sai stack 🟡 MEDIUM

**File**: `shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt`

```kotlin
if (methodName != "<init>") {
    frame.pop() // pop 'this' — SAI: luôn pop kể cả cho INVOKESTATIC
}
```

`INVOKESTATIC` không có `this` trên stack → `pop()` trên stack rỗng → `StackUnderflowException` → crash.

---

## Proposed Changes

### [MODIFY] NativeGraphicsBridge.kt (androidMain)

1. Đổi `System.loadLibrary("shared")` → `System.loadLibrary("j2me_native")`
2. Tách JNI externals ra `internal object NativeGraphicsBridgeJni` (Kotlin standalone object không cho phép companion object) để sinh JNI symbol đúng chuẩn.

### [MODIFY] android_jni_bridge.cpp

- Thay SDL2 init (cần SurfaceView) bằng **software framebuffer 240×320 ARGB8888**
- Cập nhật JNI function names để match class `NativeGraphicsBridgeJni`
- Implement `nativeFillRect` với bounds check an toàn

### [MODIFY] NativeMethodBridge.kt

- Thêm `isStatic: Boolean = false` vào `callNativeMethod()`
- Chỉ pop `this` khi `!isStatic && stackSize() > 0`

### [MODIFY] ExecutionEngine.kt

- Truyền `isStatic = (opcode == Opcodes.INVOKESTATIC)` vào `callNativeMethod()`
- Cập nhật stub path tương tự

### [MODIFY] Graphics.kt

- Thêm `setColorRGB()` xử lý overload `Graphics.setColor(int r, int g, int b)` — descriptor `(III)V`

---

## Verification Results

```
.\gradlew :androidApp:assembleDebug

:shared:compileDebugKotlinAndroid        ✅ BUILD SUCCESSFUL
:androidApp:buildCMakeDebug[arm64-v8a]  ✅
:androidApp:buildCMakeDebug[x86_64]     ✅
:androidApp:assembleDebug               ✅ BUILD SUCCESSFUL in 5s
```

> [!NOTE]
> SDL2 thực sự (render lên màn hình) cần `SurfaceView` (Phase 4 — chưa làm).
> Software framebuffer là placeholder an toàn cho đến khi Phase 4 hoàn thiện.

> [!WARNING]
> `MainActivity` gọi `NativeGraphicsBridge.initSDL()` trực tiếp trước khi có Surface.
> Phase 4 cần dùng `SurfaceView.Callback.surfaceCreated()` để gọi init đúng thời điểm.
