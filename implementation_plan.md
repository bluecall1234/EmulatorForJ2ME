# Kế hoạch Xây dựng Clean-room Hybrid J2ME Emulator

Dự án này sẽ được xây dựng theo phương pháp **Clean-room Implementation**, nghĩa là chúng ta tự viết lại mã nguồn dựa trên tài liệu đặc tả (Specifications) của J2ME (JSR 118, JSR 139) thay vì copy code từ các dự án mã nguồn mở khác. Điều này đảm bảo dự án sở hữu bản quyền 100% hợp lệ để đưa lên Google Play và App Store.

## 1. Kiến trúc Hybrid (KMP + SDL2 + CMP)
Kiến trúc này kết hợp sức mạnh của 3 công nghệ hiện đại nhất:
*   **Compose Multiplatform (CMP)**: Quản lý giao diện người dùng (App Shell, cài đặt, danh sách game).
*   **SDL2 (C++)**: Quản lý cửa sổ render (OpenGL/Metal), xử lý input mức thấp và âm thanh.
*   **Kotlin Multiplatform (KMP)**: Đóng vai trò là "Bộ não" - chứa Trình thông dịch Bytecode và các API J2ME. KMP giao tiếp với SDL2 thông qua JNI (Android) và C-Interop (iOS).

## 2. Cấu trúc Dự án
```text
J2ME-Emulator/
├── composeApp/             # App vỏ bề ngoài (Menu, Cài đặt) viết bằng Compose
├── shared/                 # Module Logic cốt lõi (KMP)
│   ├── commonMain/         # Trình thông dịch Bytecode (Interpreter), J2ME APIs
│   ├── androidMain/        # JNI Bridge gọi xuống thư viện .so của SDL2
│   └── iosMain/            # C-Interop gọi trực tiếp mã C của SDL2
└── nativeMain/             # Thư mục mã nguồn C/C++
    ├── sdl2/               # Thư viện SDL2
    ├── graphics/           # Logic chuyển đổi lệnh vẽ sang OpenGL/Metal
    └── bridge/             # Các hàm C nhận lệnh từ Kotlin Interpreter
```

## 3. Lộ trình triển khai (Roadmap)

### Giai đoạn 1: Liên kết Native (Native Interop)
*   **Mục tiêu**: Làm cho Kotlin (ở module `shared`) có thể gọi được một hàm C cơ bản (chưa cần SDL2 vội).
*   **Thực hiện**: Cấu hình `cinterop` trong Gradle. Phía Android dùng JNI (`System.loadLibrary`), phía iOS dùng liên kết tĩnh (Static Linking).

### Giai đoạn 2: Trình thông dịch Bytecode (Interpreter)
Đây là phần lõi giá trị nhất của Emulator.
*   **Bước 2.1**: Xây dựng `JavaClassFile` parser để đọc file `.class` từ JAR.
*   **Bước 2.2**: Phân tích `Constant Pool` (Nơi chứa các chuỗi, tên hàm, tên biến).
*   **Bước 2.3**: Trích xuất bytecode của các methods và xây dựng vòng lặp thực thi Opcode (ví dụ: `aload`, `invokevirtual`, `iadd`).

### Giai đoạn 3: Porting J2ME APIs (Native Method Bridge)
*   Tự viết lại các class tiêu chuẩn của J2ME (`java.lang.*`, `javax.microedition.lcdui.*`) bằng Kotlin.
*   **Cơ chế Native Bridge**: Mọi lời gọi hàm (`invokestatic`, `invokevirtual`) trỏ tới các package hệ thống `java.*` hoặc `javax.*` sẽ bị đánh chặn bởi `NativeMethodBridge` và chuyển hướng sang Lambda chạy bằng Kotlin tĩnh thay vì thông dịch bytecode.
*   Khi có hàm gọi đồ họa (ví dụ `Canvas.fillRect`), Kotlin sẽ tiếp tục gọi hàm C tương ứng đi vào `nativeMain` để SDL2 vẽ lên màn hình.

### Giai đoạn 4: Cửa sổ hiển thị (Surface Integration)
*   Nhúng `SurfaceView` vào giao diện Compose của Android.
*   Đẩy con trỏ của `SurfaceView` xuống SDL2 để SDL2 bắt đầu render game lên mặt kính đó. Phủ lên trên là một lớp Compose xử lý Phím ảo (Virtual Keypad).


