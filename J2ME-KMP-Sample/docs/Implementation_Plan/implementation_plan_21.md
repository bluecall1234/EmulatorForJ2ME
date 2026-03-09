# Implementation Plan 21 - Bổ sung InputStream.read và các API còn thiếu

## Mục tiêu
Giải quyết lỗi `Lookup failed for: java/io/InputStream.read:([BII)I` và các lỗi lookup liên quan được phát hiện trong log của trình giả lập Android. Đảm bảo game có thể đọc dữ liệu mảng byte một cách chính xác.

## Thay đổi đề xuất

### [Component] J2ME API Bridge (`NativeMethodBridge.kt`)

#### [MODIFY] [NativeMethodBridge.kt](file:///d:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- **[NEW]** Bổ sung `registerJavaIoInputStream` để triển khai phương thức `read:([BII)I`.
- **[NEW]** Đăng ký `java/io/DataInputStream.read:([BII)I` (ủy quyền cho implementation của `InputStream`).
- **[MODIFY]** Cập nhật phương thức `init` để gọi `registerJavaIoInputStream`.

## Kế hoạch xác minh

### Kiểm tra tự động
- Build lại project bằng lệnh: `./gradlew :shared:assembleDebug`
- Chạy trình giả lập và kiểm tra `emulator_log.txt`.
- Xác nhận không còn lỗi `Lookup failed for: java/io/InputStream.read:([BII)I`.

### Kiểm tra thủ công
- Kiểm tra xem game có load được sprite hoặc dữ liệu màn chơi nhanh hơn/chính xác hơn không.
