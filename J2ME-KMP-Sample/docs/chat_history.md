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

### Công việc đã làm (Phase 1, 2, 3)

1. **Phase 1 — Native Interop**:

   - Cấu hình KMP `cinterop` cho platform iOS và include headers chuẩn.
   - Thiết lập submodule SDL2 bên trong thư mục `nativeMain/`.
   - Viết `NativeBridge.kt` với khai báo load thư viện động `System.loadLibrary("j2me_native")` cho Android.
   - Khởi tạo script JNI C++ cơ bản `android_jni_bridge.cpp`.
2. **Phase 2 — Bytecode Interpreter (Core JVM)**:

   - Viết luồng đọc Binary `.class` file tĩnh: `ClassFileParser`, `ConstantPoolParser`, `JavaClassFile`. Parse thành công Magic Number, Constant Pool, Methods, Attributes.
   - Định nghĩa `ExecutionFrame` cho cấu trúc stack frame (operand stack, local variables, PC).
   - Ánh xạ hơn 100+ JVM Opcode (chẳng hạn `ILOAD`, `IADD`, `INVOKEVIRTUAL`) trong `Opcodes.kt`.
   - Triển khai vòng lặp vi xử lý (fetch-decode-execute) bên trong `ExecutionEngine.kt`.
   - Triển khai bộ nhớ Heap (`Heap.kt`) cho việc cấp phát `HeapObject` (quản lý instance fields).
3. **Phase 3 — J2ME API Implementation (CLDC/MIDP Stubs)**:

   - Viết `NativeMethodBridge.kt` để can thiệp (intercept) khi Bytecode gọi các hàm của `java.*` hoặc `javax.*`.
   - Giả lập thư viện chuẩn: `java.lang.System.currentTimeMillis()`, `java.io.PrintStream`.
   - Giả lập thư viện UI cốt lõi: `javax.microedition.lcdui.Display`, `Canvas`, `Graphics`.
   - Kết nối `Graphics.fillRect`, `setColor` từ Kotlin gọi ngầm xuống C++ (JNI) qua `NativeGraphicsBridge.kt`.
   - Tạo `MIDlet.kt` base class và demo cấu trúc app J2ME cơ bản.

### Build Result

```text
:shared:compileKotlinAndroid — SUCCESS ✅
:androidApp:assembleDebug — BUILD SUCCESSFUL ✅
Bytecode Core Unit Tests (Mock) — COMPLETED ✅
```

---

## Session 2 — 2026-03-06 (Conversation: 5ac33602)

### Vấn đề báo cáo

App crash khi launch trên Android emulator, liên quan đến phần **J2ME Graphics → SDL2 Render APIs**.

### Root Cause Analysis

| # | Bug                                                                                   | File                                      | Mức độ   |
| - | ------------------------------------------------------------------------------------- | ----------------------------------------- | ----------- |
| 1 | JNI extern trong `standalone object` → sai symbol name → `UnsatisfiedLinkError` | `NativeGraphicsBridge.kt` (androidMain) | 🔴 CRITICAL |
| 2 | `loadLibrary("shared")` — sai tên .so, đúng phải là `"j2me_native"`         | `NativeGraphicsBridge.kt` (androidMain) | 🔴 CRITICAL |
| 3 | `gRenderer == nullptr` vì SDL2 init bị comment (cần SurfaceView chưa có)       | `android_jni_bridge.cpp`                | 🟠 HIGH     |
| 4 | `NativeMethodBridge` luôn pop `this` kể cả INVOKESTATIC → stack underflow     | `NativeMethodBridge.kt`                 | 🟡 MEDIUM   |

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

## Session 3 — 2026-03-06 (Conversation: 5ac33602 part 2)

### Vấn đề báo cáo

App vẫn crash khi launch sau các bản vá ở Session 2. Cần phân tích sâu hơn nguyên nhân trong interpreter và JNI execution flow.

### Root Cause Analysis (Bugs #5 - #7)

| # | Bug                                                                                                         | File                      | Mức độ   |
| - | ----------------------------------------------------------------------------------------------------------- | ------------------------- | ----------- |
| 5 | `ConstantPool.resolveMethodRef()` crash với `IllegalArgumentException` khi gặp `InterfaceMethodRef` | `ConstantPoolParser.kt` | 🔴 CRITICAL |
| 6 | `MainActivity` không catch exceptions, gây silent crash                                                 | `MainActivity.kt`       | 🟠 HIGH     |
| 7 | `INVOKEINTERFACE` sử dụng logic pop args/`this` không an toàn                                       | `ExecutionEngine.kt`    | 🟡 MEDIUM   |

### Fixes đã thực hiện

- **Fix #5**: Cập nhật `resolveMethodRef()` nhận diện và xử lý cả `MethodRef` và `InterfaceMethodRef`.
- **Fix #6**: Wrap logic `onCreate` của `MainActivity` trong khối `try-catch`, in ra màn hình Compose nếu có lỗi để tiện debug.
- **Fix #7**: Dẫn luồng `INVOKEINTERFACE` qua `NativeMethodBridge` giống như `INVOKEVIRTUAL`, và thêm kiểm tra `frame.stackSize() > 0` trước khi pop.

### Build Result

```
:androidApp:assembleDebug — BUILD SUCCESSFUL in 10s ✅
```

---

## Session 4 — 2026-03-06 (Conversation: 5ac33602 part 3)

### Vấn đề / Mục tiêu

1. Lỗi `Unimplemented opcode: 0x3f` khi chạy lệnh `Long Store`
2. Người dùng muốn xuất `println` ra file `.txt` đặt trong app data directory để dễ debug trên máy tính (Android & iOS).

### Root Cause Analysis & Fixes

- **Fix Opcode 0x3F**: Chèn `LSTORE_0..3` và `FSTORE_0..3` vào constant sheet `Opcodes.kt` và thêm logic vào vòng lặp `while` tại `ExecutionEngine.kt`.
- **Feature: Dual PrintStream KMP Logger**:
  - `shared/src/commonMain/kotlin/emulator/core/Logger.kt`: Khai báo `expect fun setupFileLogging(context: Any?)`
  - `shared/src/androidMain/kotlin/emulator/core/Logger.kt`: Clone 2 luồng `System.out` và `System.err`, vừa in vào Logcat, vừa viết vào tệp `/sdcard/Android/data/com.example.j2me.android/files/emulator_log.txt`. Ghi đè file mỗi lần bật app.
  - `shared/src/iosMain/kotlin/emulator/core/Logger.kt`: Stub `println` chờ C-Interop `freopen` trong Phase tới.
  - `MainActivity.kt`: Khởi tạo hàm `setupFileLogging(this)` tại `onCreate`.

### Build Result

```
:androidApp:assembleDebug — BUILD SUCCESSFUL in 8s ✅
```

---

## Session 5 — 2026-03-06 (Conversation: 5ac33602 part 4)

### Vấn đề / Mục tiêu

1. Tiến hành Phase 4: Thay thế Emulator mock (console loop) bằng UI thực tế trên Jetpack Compose.
2. Render graphics mảng byte từ C++ (`gFramebuffer`) trực tiếp lên màn hình điện thoại sử dụng NDK `ANativeWindow` và `SurfaceView`.
3. Tương thích đa độ phân giải của game J2ME.

### Công việc đã làm

1. **Dynamic Resolution & Scaling**:
   - Sửa đổi C++ bridge (`nativeInitSDL`) để nhận thông số `width, height` chủ động từ Kotlin.
   - Thêm `nativeSetSurface`. Sử dụng cơ chế hardware scaling tự động thông qua `ANativeWindow_setBuffersGeometry`. C++ tự động copy (`blit`) pixel buffer lên Surface khi `nativePresentScreen` được gọi.
2. **Compose UI Shell**:
   - Thay thế `MainActivity.kt` logic để render `AndroidView` (SurfaceView). Bổ sung cơ chế Letterboxing (`Modifier.aspectRatio`) để chống méo hình.
   - Khởi chạy Emulator core nằm ngầm bên trong một Thread riêng rẽ (Coroutine `Dispatchers.Default`) để UI thread không bị đơ.
   - Viết component Component Compose `VirtualKeypad` (Nokia style) giả lập các nút điện thoại chuẩn.
3. Fix lỗi import `JarLoader` và undeclared `FB_WIDTH/HEIGHT` trong khi port C++ code.

### Build Result

```
:androidApp:assembleDebug — BUILD SUCCESSFUL in 5s ✅
```

---

## Session 7 — 2026-03-07 (Conversation: 5ac33602 part 6)

### Vấn đề / Mục tiêu

1. Cần fix bug crash khi load game `GloftBBPM` do Emulator báo lỗi "Invoking unhandled non-native method" tại các hàm khởi tạo class cơ bản (như `<clinit>`, `<init>`).
2. Mở rộng Engine thực thi Bytecode (`ExecutionEngine`) để hỗ trợ Recursive Virtual Call.

### Công việc đã làm (Phase 5.1: Dynamic Bytecode Execution)

Tôi đã implement thành công Dynamic Bytecode Execution Engine thay vì phụ thuộc hoàn toàn vào `NativeMethodBridge`.

Tiến độ kỹ thuật:

- **Method Execution (Recursive Virtual Calls)**: `ExecutionEngine` đã được nâng cấp. Khi gặp lệnh gọi hàm non-native (`INVOKEVIRTUAL`, `INVOKESPECIAL`, v.v.), thay vì báo "stub", nó sẽ động khởi tạo một Frame thực thi mới, nạp `.class` bytecode và đệ quy chạy nhánh opcode đó.
- **Class Initializers**: Đảm bảo các khối `<clinit>` luôn được gọi trước khi một Class được dùng tới (khi gặp `NEW`, `GETSTATIC`, `PUTSTATIC`, hoặc `INVOKESTATIC`).
- **Mở rộng Opcode**: Đã bổ sung một lượng lớn các tính năng JVM đang thiếu, bao gồm:
  - Primitive / Multidimensional Arrays (`LALOAD`, `BASTORE`, `MULTIANEWARRAY`, v.v.)
  - Chuyển đổi & So sánh Số thực (Float/Double, `FCMPG`, `DCMPL`, `DLOAD`)
  - Chuyển dịch Bitwise (`ISHL`, `IUSHR`, `LXOR`, v.v.)
  - Đồng bộ hoá luồng (`MONITORENTER` / `MONITOREXIT`)
- **Fix lỗi Primitive mặc định**: Xử lý triệt để bug `Expected int on stack, got null` bằng cách trả về đúng giá trị primitive mặc định (`0`, `0.0f`) cho các trường static chưa khởi tạo.

**Kết quả**: Game `GloftBBPM` đã load thành công toàn bộ các Class nội bộ (`cGame`, `i`), tuần tự chạy các bộ khởi tạo tĩnh của chúng, và hoàn thành hàm `startApp` nguyên bản qua Engine của chúng ta! Game loop đã được trigger thành công tại `java.lang.Thread.start()`.

### Trạng thái build

```text
:androidApp:installDebug — BUILD SUCCESSFUL in 9s ✅
```

---

## Session 6 — 2026-03-06 (Conversation: 5ac33602 part 5)

### Vấn đề / Mục tiêu

1. Hoàn thành phần còn lại của Phase 4: Implement màn hình Quản lý Thư viện Game (Game Library).
2. Tích hợp công cụ chọn file (File Picker) để load file `.jar` J2ME thực tế từ thiết bị.

### Công việc đã làm (Phase 4 Mở rộng)

Tôi đã implement xong màn hình Quản lý Thư viện Game và nó đã build thành công rực rỡ!

Tiến độ kỹ thuật:

- **GameLibraryScreen**: Tôi đã tách biệt luồng ứng dụng ra 2 màn hình quản lý trạng thái đơn giản bằng AppScreen Enum (LIBRARY & EMULATOR).
- **File Picker & SAF**: Áp dụng ActivityResultContracts.GetContent() của Jetpack Compose để mở trình quản lý file hệ thống (SAF) ngay khi bấm dấu +. Mimetype được set là application/java-archive, cho phép bạn chọn thoải mái bất kỳ file .jar J2ME gốc nào ở trong thẻ nhớ hoặc Download Folder.
- **Internal Storage**: Mọi game được thêm vào sẽ được ứng dụng sao chép (copy) an toàn vào vùng Data nội bộ (filesDir/games/).
- **Xoá Game**: Đã thêm một nút thùng rác (Thùng Rác đỏ) kế bên tên mỗi game. Khi nhấn vào, file vật lý trong internal disk của Emulator sẽ bị delete khỏi disk.
- **Auto-parse JAR (JarLoader)**: Code Mock bytebuffer hardcode dở dang trước đó đã bị xoá sổ. JarLoader thực sự dành riêng cho Android (actual class) được tích hợp sử dụng API cốt lõi java.util.zip.ZipFile. Nó tự động đọc và bung file .jar ngoài ra quét đọc file cấu hình META-INF/MANIFEST.MF để truy xuất ra được chính xác tên Class Khởi Chạy nằm ở Property MIDlet-1. Việc này giúp mọi game J2ME tải về đều có thể "tự động" dò dúng file chạy chính mà bạn không cần phải truyền thêm tham số!

Phase 5.2:

Game "Bubble Bash Mania" (GloftBBPM) đã khởi chạy thành gốc vòng lặp game ngầm (background thread) mà không bị crash.

Dưới đây là tóm tắt tiến độ **Phase 5.2** vừa hoàn thành:

1. **JAD Parsing & File Picker** : `MainActivity.kt` đã được cấu hình cho chọn nhiều file cùng lúc (từ Android SAF). App sẽ đọc file `.jad` đi kèm để lấy flag cấu hình (`MIDlet-Touch-Support`) và gộp với thông tin từ file `.jar`.
2. **Native Threading (`java.lang.Thread`)** : Tôi đã implement bridge cho thư viện `java.lang.Thread.start()`. Ngay khi game gọi `start()`, Emulator tự động spawn một Coroutine (GlobalScope) chạy ngầm và gọi đệ quy hàm `run()` của `Runnable` trên hệ thống BytecodeInterpreter. *(Xác nhận qua logcat: Engine đang chạy trơn tru thực thi các lệnh ldc, iastore, dup ở đoạn mã bytecode PC > 7000 ngầm).*

Việc tiếp theo (để hình ảnh game lên màn hình) theo Implementation Plan là  **Phase 5.3** :

* Implement Graphics API (`setColor`, `fillRect`, `drawImage`...).
* Kết nối thông tin Input (Touch & Keyboard) từ Jetpack Compose xuống hệ thống game.

---

## Trạng thái hiện tại (2026-03-07)

### Hoàn thành

- [X] Phase 1: Native Interop (JNI + C-Interop)
- [X] Phase 2: Bytecode Interpreter
- [X] Phase 3: J2ME APIs (CLDC + MIDP) — crash bugs đã fix
- [X] Phase 4: Compose Multiplatform App Shell
- [X] **Phase 5.1**: Dynamic Bytecode Execution (ExecutionEngine, Class Init, Opcodes)

### Còn lại

- [ ] **Phase 5.2**: Threading & Game Loop
  - [ ] Implement `java.lang.Thread` handler trong `NativeMethodBridge` để spawn Android/KMP coroutines/threads.
- [ ] **Phase 5.3**: Tương tác Hệ thống & System Events
  - [ ] Chuyển tiếp Event từ Virtual Keyboard xuống `ExecutionEngine`.
  - [ ] Ánh xạ Touch Events (Translate Compose Pointer input to J2ME `pointerPressed`).
- [ ] **Phase 6**: Optimization & C++
  - [ ] Giải quyết các lỗi Lint khó chịu trong thư mục `nativeMain`.
  - [ ] Viết màn hình "Game Settings" để cấu hình độ phân giải.

### Ghi chú kỹ thuật

- Tích hợp vòng lặp mô phỏng nằm trong `EmulatorThread` để đảm bảo UI mượt. Game loop C++ sẽ được tối ưu hoá lại tuỳ trường hợp.

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
