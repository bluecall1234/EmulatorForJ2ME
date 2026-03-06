# Kế hoạch triển khai Phase 4: Compose Multiplatform App Shell & SurfaceView

**Mục tiêu**: Thay thế chế độ "chạy ngầm, in log" hiện tại bằng một Giao diện Người Dùng (UI) thực sự chạy trên Android bằng Jetpack Compose. Tích hợp `SurfaceView` để kết xuất đồ họa từ C++ lên màn hình thiết bị và tạo một Bàn phím ảo (Virtual Keypad) để điều khiển giả lập.

---

## 1. Tích hợp SurfaceView & Native Rendering (C++)

Hiện tại, `android_jni_bridge.cpp` đã vẽ ra RAM (`gFramebuffer`) với kích thước cứng cứng `240x320`. Ta cần:
1. Đẩy buffer này lên màn hình thật thông qua `SurfaceView` bằng Android NDK (`ANativeWindow`).
2. **Hỗ trợ Đa độ phân giải (Dynamic Resolution) & Tỉ lệ khung hình (Aspect Ratio)**: Các game J2ME có thể là `240x320`, `320x240` (Landscape), `480x640`, v.v.

### **Cập nhật `android_jni_bridge.cpp`**
- Gọi `#include <android/native_window_jni.h>`
- Thêm JNI function `nativeSetSurface(JNIEnv* env, jobject clazz, jobject surface)` để nhận `Surface` từ Java. Lấy `ANativeWindow` từ `surface` và lưu vào biến toàn cục `gWindow`.
- **Cơ chế Scaling (Tự động canh giữa & giữ tỉ lệ):**
  - Khi thiết lập `ANativeWindow_setBuffersGeometry(window, base_width, base_height, format)`, hệ điều hành Android HMM (Hardware Composer) sẽ **TỰ ĐỘNG CHUẨN HOÁ SCALING** (Stretch/Scale) bằng GPU từ độ phân giải cơ sở (ví dụ: `240x320`) lên vừa chính xác với kích thước thực của `SurfaceView` trên điện thoại!
  - Điều này có nghĩa ta KHÔNG CẦN viết thuật toán Pixel Scaling bằng tay trong C++. Chỉ cần khởi tạo kích thước Render Buffer gốc đúng với game mong muốn, Android sẽ lo phần phóng to đồ hoạ (Hardware Stretch).
  - *Lưu ý (Future Phase)*: Sẽ xây dựng một màn hình "Cấu hình Game" (Game Settings) ở UI dùng để người dùng nhập/chọn độ phân giải gốc của game. Biến này sẽ được truyền xuống `initSDL(width, height)`.
- Cập nhật hàm `nativePresentScreen()`: Sử dụng `ANativeWindow_lock` để lấy vùng nhớ màn hình, copy (blit) `gFramebuffer` vào đó (sự chênh lệch `stride` sẽ được xử lý), và gọi `ANativeWindow_unlockAndPost` để hiển thị khung hình mượt mà.

### **Cập nhật `NativeGraphicsBridge.kt` (AndroidMain)**
- Bổ sung `external fun nativeSetSurface(surface: Any)` để truyền instance `android.view.Surface` xuống C++.
- Bổ sung hàm thiết lập kích thước giả lập (Ví dụ: `initSDL(width: Int, height: Int)`) để C++ có thể cấp phát/cấu hình Resize mảng `gFramebuffer` theo đúng độ phân giải của game (thay vì cố định `240x320`).

---

## 2. Compose UI: Màn hình Giả lập (EmulatorScreen)

Thay thế `MainActivity` hiện tại bằng một kiến trúc UI chuẩn của Jetpack Compose.

### **Thành phần `EmulatorDisplay` (Màn hình chính)**
- Sử dụng `AndroidView` wrapper để nhúng một `SurfaceView` truyền thống vào trong Jetpack Compose.
- **Bảo toàn tỷ lệ (Aspect Ratio Preserving)**: Áp dụng `Modifier.aspectRatio(gameWidth / gameHeight.toFloat())` vào `AndroidView` (kết hợp với canh giữa màn hình). Máy ảo sẽ hiện đúng kích cỡ (ví dụ hộp chữ nhật dọc cho 240x320), và có thanh đen (letterbox) ở các phần khoảng trống nếu màn hình điện thoại tỷ lệ khác.
- Gắn `SurfaceHolder.Callback`. Khi `surfaceCreated` kích hoạt, truyền đối tượng `holder.surface` xuống `NativeGraphicsBridge.nativeSetSurface(...)`. Khi `surfaceDestroyed`, truyền `null` để giải phóng bộ nhớ.

### **Thành phần `VirtualKeypad` (Bàn phím ảo J2ME)**
- Thiết kế một Composable chứa các nút bấm phổ biến của điện thoại nokia cũ: 
  - Phím điều hướng D-Pad (Up, Down, Left, Right, Fire/Action).
  - Phím chức năng (Soft Left, Soft Right).
  - Phím số (0-9, *, #).
- Các nút bấm này sẽ bắt sự kiện chạm (`pointerInput` hoặc `Modifier.clickable`) để gửi mã phím (Key Codes chuẩn J2ME như `-1` cho UP, `-5` cho Fire) vào hệ thống Emulator.

---

## 3. Kiến trúc Đa luồng & Vòng lặp giả lập (Emulator Thread)

Hiện tại, `MainActivity.onCreate` đang gọi `interpreter.executeMethod("startApp")` ngay trên luồng chính (Main Thread/UI Thread). Nếu J2ME game có vòng lặp vô tận (game loop), UI Thread sẽ bị đóng băng (ANR).

**Giải pháp**:
- Chuyển toàn bộ chu trình khởi tạo và `startApp` vào một luồng chạy nền (Coroutines/Thread background) có tên là `EmulatorThread`.
- Luồng đồ họa (JNI `fillRect` và `presentScreen`) sẽ chạy ổn định và gọi trực tiếp lên `SurfaceView` bất kể luồng UI của Compose đang làm gì.

---

## Các bước thực hiện (Verification Plan)

1. Cập nhật `CMakeLists.txt` để link thư viện `android` (cần cho `ANativeWindow`).
2. Code C++: Triển khai bộ đệm khung (Blitting gFramebuffer -> ANativeWindow).
3. Code Kotlin JNI: `nativeSetSurface`.
4. Code Compose: Chỉnh sửa lại `MainActivity.kt`, tạo layout màn hình dọc `Column` chia làm 2 nửa (Trên là SurfaceView màn hình 240x320, Dưới là Bàn phím ảo).
5. (Tuỳ chọn) Điều phối lại game loop `MyTestGame` chạy ở một Coroutine riêng (Dispatchers.Default) để không block UI.
6. Chạy ứng dụng và nhìn thấy đồ hoạ J2ME hiện ra thật thay vì chỉ xem tệp log.
