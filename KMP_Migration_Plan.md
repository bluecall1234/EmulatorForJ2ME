# Kế hoạch chuyển đổi J2ME-Loader sang Kotlin Multiplatform (KMP)

Việc chuyển đổi từ một ứng dụng Android thuần túy (J2ME-Loader) sang mô hình đa nền tảng (Android & iOS) yêu cầu một chiến lược chia tách mã nguồn rõ ràng giữa logic nghiệp vụ (Shared) và các thành phần phụ thuộc vào hệ điều hành (Platform-specific).

## 1. Cấu trúc Dự án Mới
Khởi tạo dự án KMP với các module:
- `commonMain`: Chứa logic cốt lõi, trình thông dịch Java Bytecode, và các lớp giao diện J2ME (MIDP/CLDC) đã được convert sang Kotlin.
- `androidMain`: Chứa code wrapper cho Android, tích hợp JNI cho các thư viện C++.
- `iosMain`: Chứa code wrapper cho iOS, tích hợp Objective-C/Swift interop và C-interop cho đồ họa 3D.

## 2. Các Bước Thực Hiện Chi Tiết

### Bước 1: Chuyển đổi và Di trú J2ME API (`javax.microedition.*`)
Đây là phần lớn nhất của source code. Bạn cần:
- Chuyển toàn bộ các class Java trong `app/src/main/java/javax` sang Kotlin.
- Tách biệt logic xử lý khỏi Android framework. Ví dụ: `Graphics` hiện đang dùng `android.graphics.Canvas`.
- **Giải pháp**: Sử dụng **Compose Multiplatform Canvas** (dựa trên Skia). Skia chạy tốt trên cả Android và iOS, giúp bạn viết code vẽ 2D một lần trong `commonMain`.

### Bước 2: Giải quyết vấn đề Bytecode trên iOS
Apple không cho phép chạy máy ảo Dalvik. Vì vậy:
- **Trên Android**: Có thể giữ nguyên cơ chế dịch sang DEX của J2ME-Loader để đạt tốc độ cao nhất.
- **Trên iOS**: Phải xây dựng/tích hợp một **Bytecode Interpreter** (Trình thông dịch). Bạn sẽ parse file `.class` và thực thi từng lệnh logic (Opcode) thủ công. 
- *Gợi ý*: Có thể tham khảo hoặc port các dự án JVM siêu nhỏ như `MiniJVM` sang Kotlin.

### Bước 3: Đồ họa và Render Engine
- **2D Games**: Sử dụng Skia (qua Compose Multiplatform) để render `javax.microedition.lcdui.Canvas`. Điều này đảm bảo hiệu năng mượt mà trên cả 2 hệ điều hành.
- **3D Games (M3G/Mascot Capsule)**: 
  - J2ME-Loader đang dùng mã nguồn C++ rất tốt. 
  - Bạn sẽ giữ lại phần C++ này. Android dùng JNI để gọi, còn iOS dùng **C-Interop** của Kotlin Native để liên kết trực tiếp với mã C++.

### Bước 4: Hệ thống lưu trữ (Record Store System - RMS)
- Hiện tại J2ME-Loader lưu xuống file system của Android.
- **Chuyển đổi**: Sử dụng thư viện **SQLDelight** hoặc **Okio** để quản lý lưu trữ. Cả 2 thư viện này đều hỗ trợ KMP, giúp việc lưu Database/File hoạt động trong suốt trên cả Android và iOS.

### Bước 5: Audio và Input
- Xây dựng lớp `expected` cho Player điều khiển âm thanh.
- `androidMain` sẽ gọi `MidiDriver` hoặc API hệ thống Android.
- `iosMain` sẽ gọi `AVFoundation` (thông qua Kotlin/Native interop) để phát nhạc MIDI/WAV.
- Touch input sẽ được map từ các sự kiện của Compose Multiplatform xuống logic phím bấm của J2ME.

## 3. Lộ trình Triển khai (Roadmap)
1. **Giai đoạn 1**: Tạo project KMP, setup Compose Multiplatform và hiển thị được một màn hình J2ME "Hello World" (Canvas trắng).
2. **Giai đoạn 2**: Port các class cơ bản của CLDC (Thread, String, I/O) và MIDP (Display, Displayable).
3. **Giai đoạn 3**: Viết Bytecode Interpreter đơn giản để chạy được logic tính toán cơ bản từ file JAR.
4. **Giai đoạn 4**: Tích hợp Skia Rendering và hiển thị hình ảnh từ game JAR.
5. **Giai đoạn 5**: Port phần âm thanh và lưu trữ RMS.
6. **Giai đoạn 6**: Tích hợp C++ 3D engine cho iOS (sử dụng Metal/OpenGL).
