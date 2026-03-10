# Implementation Plan 155 - Triển khai Displayable.kt (MIDP 2.0)

Kế hoạch này tập trung vào việc tạo lớp `Displayable.kt` để quản lý các thuộc tính chung của các đối tượng đồ họa hiển thị được (Displayable), bao gồm bộ lệnh (Commands), tiêu đề (Title) và các sự kiện vòng đời.

## Proposed Changes

### [J2ME API Layer]

#### [NEW] [Displayable.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/javax/microedition/lcdui/Displayable.kt)
- Khai báo đối tượng `Displayable` để xử lý các lời gọi từ Interpreter.
- Các hàm sẽ được triển khai:
    - `addCommand(frame)`: Lưu command vào danh sách trong `HeapObject`.
    - `removeCommand(frame)`: Xóa command khỏi danh sách.
    - `setCommandListener(frame)`: Lưu listener vào `HeapObject`.
    - `setTitle(frame)` / `getTitle(frame)`: Quản lý tiêu đề.
    - `getWidth(frame)` / `getHeight(frame)`: Trả về kích thước màn hình (mặc định 240x320 hoặc lấy từ Display).
    - `isShown(frame)`: Trả về trạng thái hiển thị (mặc định true).
    - `setTicker(frame)` / `getTicker(frame)`: Stub cho thành phần Ticker.

#### [MODIFY] [NativeMethodBridge.kt](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/J2ME-KMP-Sample/shared/src/commonMain/kotlin/emulator/core/api/NativeMethodBridge.kt)
- Cập nhật hàm `registerJavaxMicroeditionLcduiDisplayable` để:
    - Chuyển logic `getWidth`, `getHeight` sang `Displayable.kt`.
    - Thêm đăng ký cho `addCommand`, `removeCommand`, `setCommandListener`, `setTitle`, `getTitle`, `isShown`, `setTicker`, `getTicker`.
    - Giữ lại các đăng ký liên quan đến `<init>` cho `Displayable` và `Canvas`.

## Verification Plan

### Automated Tests
- Kiểm tra tính đúng đắn của việc lưu trữ và truy xuất Command trong `HeapObject`.
- Đảm bảo các subclass như `Canvas` vẫn hoạt động bình thường khi kế thừa logic từ `Displayable` qua bytecode.

### Manual Verification
- Chạy emulator và quan sát log khi game thực hiện `addCommand` hoặc `setTitle`.
