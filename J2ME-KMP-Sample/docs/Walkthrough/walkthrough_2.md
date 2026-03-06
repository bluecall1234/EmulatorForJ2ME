# Walkthrough: Fix App Launch Crash — Session 3

**Ngày**: 2026-03-06  
**Kết quả**: ✅ BUILD SUCCESSFUL

---

## Build Output

```
:shared:compileDebugKotlinAndroid        ✅
:androidApp:buildCMakeDebug[arm64-v8a]  ✅
:androidApp:buildCMakeDebug[x86_64]     ✅
:androidApp:assembleDebug               ✅  BUILD SUCCESSFUL in 10s
```

---

## Bug #5 — `ConstantPool.resolveMethodRef()` 🔴 CRITICAL

**Trước:** Chỉ handle `MethodRef` (tag 10) → `IllegalArgumentException` khi gặp `InterfaceMethodRef` (tag 11)

**Sau:**
```kotlin
fun resolveMethodRef(index: Int): Triple<String, String, String> {
    return when (val entry = entries[index]) {
        is ConstantPoolEntry.MethodRef -> { ... }
        is ConstantPoolEntry.InterfaceMethodRef -> { ... }  // ← NEW
        else -> throw IllegalArgumentException(...)
    }
}
```

**File**: `shared/src/commonMain/kotlin/emulator/core/classfile/ConstantPoolParser.kt`

---

## Bug #6 — `MainActivity` try-catch 🟠 HIGH

**Trước:** Không có try-catch → crash im lặng, không có thông tin debug

**Sau:** Toàn bộ logic trong `onCreate()` được wrap trong try-catch. Crash info hiển thị trên UI:
- Step-by-step execution log
- Error message + class name
- Full stack trace

**File**: `androidApp/src/main/java/com/example/j2me/android/MainActivity.kt`

---

## Bug #7 — `INVOKEINTERFACE` handler 🟡 MEDIUM

**Trước:**
```kotlin
for (j in 0 until argCount) frame.pop()  // unsafe
frame.pop() // unsafe pop "this"
```

**Sau:**
```kotlin
// Route through NativeMethodBridge first (like INVOKEVIRTUAL)
val isHandled = NativeMethodBridge.callNativeMethod(cls, name, desc, frame, isStatic = false)
if (!isHandled) {
    for (j in 0 until argCount) { if (frame.stackSize() > 0) frame.pop() }  // safe
    if (frame.stackSize() > 0) frame.pop()  // safe pop "this"
}
```

**File**: `shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionEngine.kt`

---

## Tổng kết bugs đã fix (7 bugs qua 3 sessions)

| # | Bug | File | Session |
|---|-----|------|---------|
| 1 | Sai tên library: `"shared"` → `"j2me_native"` | NativeGraphicsBridge.kt | 2 |
| 2 | JNI extern trong standalone object → sai symbol | NativeGraphicsBridge.kt | 2 |
| 3 | SDL2 init bị comment, gRenderer=null | android_jni_bridge.cpp | 2 |
| 4 | NativeMethodBridge pop `this` cho INVOKESTATIC | NativeMethodBridge.kt | 2 |
| 5 | resolveMethodRef chỉ handle MethodRef, không InterfaceMethodRef | ConstantPoolParser.kt | 3 |
| 6 | MainActivity không có try-catch → crash im lặng | MainActivity.kt | 3 |
| 7 | INVOKEINTERFACE unsafe pop + không route qua NativeMethodBridge | ExecutionEngine.kt | 3 |
