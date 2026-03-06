# Feature Plan: Cross-platform App Logger

**Mục tiêu**: Bắt (intercept) tất cả nội dung log (`println`) và ném ra 1 file text nằm bên trong thiết bị để người dùng dễ dàng debug trên PC. Hệ thống phải hỗ trợ cả **Android** lẫn **iOS** (KMP).

---

## Phân tích vấn đề

Bạn hỏi rất chính xác. `System.out` và `System.err`, cùng với thư viện `java.io.File`, **CHỈ TỒN TẠI TRÊN JVM (ANDROID)**. Nếu viết code log thẳng trong `MainActivity.kt` bằng các package của Java thì:
- Android: Sẽ chạy tốt.
- iOS: Code chạy hoàn toàn trên Native (không có máy ảo Java), do đó thư viện `java.io` hay `System.setOut` sẽ không được biên dịch hoặc bị lỗi. Hàm `println` trên iOS thực chất gọi C-level `printf` (tùy thuộc vào hạ tầng LLVM Kotlin/Native).

Để hỗ trợ đa nền tảng, ta không thể viết mã chỉ chạy trên Android mà bắt buộc phải thiết kế KMP Logging Interceptor. Hoặc đơn giản là tạo ra `expect fun setupFileLogging()` ở thư mục `common` và hiện thực nó ở `androidMain` / `iosMain`.

---

## Proposed Changes

Tôi sẽ tạo kiến trúc **Cross-platform Logger** tối giản như sau:

### 1. `shared/src/commonMain/kotlin/emulator/core/Logger.kt`
Khai báo 1 hàm expect dùng chung:
```kotlin
package emulator.core

/**
 * Khởi tạo hệ thống ghi log ra file trên thiết bị.
 * Phải được gọi lúc mới khởi động app.
 */
expect fun setupFileLogging()
```

### 2. `shared/src/androidMain/kotlin/emulator/core/Logger.kt`
Hiện thực (actual) cho Android. Chúng ta sẽ lợi dụng Object `Context` để lấy đường dẫn thực tế (`getExternalFilesDir`).
Vì `shared` module của ta không phải là một `Context` (Nó chỉ chứa thư viện), ta có một cách tiếp cận sau: Cập nhật hàm thành `setupFileLogging(appContext: Any?)`. Trên Android truyền `Context` vào, trên iOS truyền `null`.

```kotlin
package emulator.core
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import android.content.Context

actual fun setupFileLogging(context: Any?) {
    val androidContext = context as? Context ?: return
    
    // Tạo file: /sdcard/Android/data/[app]/files/emulator_log.txt
    val logFile = File(androidContext.getExternalFilesDir(null), "emulator_log.txt")
    val fileOut = PrintStream(FileOutputStream(logFile, false))
    
    val consoleOut = java.lang.System.out
    val dualOut = object : PrintStream(fileOut) {
        override fun write(b: Int) { super.write(b); consoleOut.write(b) }
        override fun write(buf: ByteArray, off: Int, len: Int) { 
            super.write(buf, off, len); consoleOut.write(buf, off, len) 
        }
    }
    
    java.lang.System.setOut(dualOut)
    java.lang.System.setErr(dualOut)
    println("=== [Android] Logger Started at ${logFile.absolutePath} ===")
}
```

(Sẽ sửa lại hàm `expect fun setupFileLogging(context: Any?)` cho phù hợp).

### 3. `shared/src/iosMain/kotlin/emulator/core/Logger.kt`
Trên iOS, chúng ta sẽ bắt `stdout` bằng C-interop.
Tuy nhiên, để giữ mọi thứ ĐƠN GIẢN trong đợt này (và do chúng ta chưa build UI iOS), tôi sẽ tạo hàm stub (rỗng) cho iOS. Khi nào app làm tới Phase iOS, ta sẽ gắn `freopen("log.txt", "w", stdout)` bằng CInterop.

```kotlin
package emulator.core

actual fun setupFileLogging(context: Any?) {
    // TODO: Implement iOS file logging using CInterop's `freopen`
    println("=== [iOS] Logger Started (Console only for now) ===")
}
```

### 4. `MainActivity.kt` (Android)
Gọi `setupFileLogging(this)` lúc `onCreate()`.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    emulator.core.setupFileLogging(this)
    // ...
}
```

---

## Verification Plan
1. Viết mã hệ thống KMP Logger (common, android, ios).
2. Chạy Build.
3. Chạy `adb shell ls /sdcard/Android/data/com.example.j2me.android/files/` để lấy `emulator_log.txt` và đảm bảo nó có nội dung.
